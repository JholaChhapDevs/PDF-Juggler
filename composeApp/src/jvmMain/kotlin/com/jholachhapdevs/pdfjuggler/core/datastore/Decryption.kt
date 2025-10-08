package com.jholachhapdevs.pdfjuggler.core.datastore

import java.io.File
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec

object Decryption {
    fun readAndValidateFile(filePath: String): String? {
        val fileContent = File(filePath).readText()
        val parts = fileContent.split("|")
        if (parts.size != 4) return null

        val (encryptedMessage, ivString, crcString, backupData) = parts
        val storedCRC = crcString.toLongOrNull() ?: return null

        return try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val iv = Base64.getDecoder().decode(ivString)
            cipher.init(Cipher.DECRYPT_MODE, SecretKey.get(), IvParameterSpec(iv))
            val decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedMessage))
            val decryptedMessage = String(decryptedBytes, Charsets.UTF_8)

            val calculatedCRC = Encryption.calculateCRC(decryptedMessage)
            if (calculatedCRC == storedCRC) {
                decryptedMessage
            } else {
                println("Data corrupted! Restoring corrupted parts...")
                restoreCorruptedParts(filePath, backupData)
            }
        } catch (e: Exception) {
            println("Decryption failed! Restoring from backup...")
            restoreCorruptedParts(filePath, backupData)
        }
    }

    fun restoreCorruptedParts(filePath: String, backupData: String): String {
        val backupBytes = Base64.getDecoder().decode(backupData)
        val restoredMessage = String(backupBytes, Charsets.UTF_8)
        val regeneratedFileContent = Encryption.encryptAESWithBackup(restoredMessage)
        File(filePath).writeText(regeneratedFileContent)
        return restoredMessage
    }
}