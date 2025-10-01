package com.jholachhapdevs.pdfjuggler.core.datastore

import java.io.File
import java.security.SecureRandom
import java.util.Base64
import java.util.zip.CRC32
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec

object Encryption {
    fun calculateCRC(data: String): Long {
        val crc = CRC32()
        crc.update(data.toByteArray(Charsets.UTF_8))
        return crc.value
    }

    fun encryptAESWithBackup(message: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKey.get(), IvParameterSpec(iv))
        val encryptedBytes = cipher.doFinal(message.toByteArray(Charsets.UTF_8))
        val encryptedMessage = Base64.getEncoder().encodeToString(encryptedBytes)
        val ivString = Base64.getEncoder().encodeToString(iv)
        val crc = calculateCRC(message)
        val backupData = Base64.getEncoder().encodeToString(message.toByteArray(Charsets.UTF_8))
        return listOf(encryptedMessage, ivString, crc.toString(), backupData).joinToString("|")
    }

    fun writeSelfHealingFile(filePath: String, message: String) {
        val fileContent = encryptAESWithBackup(message)
        File(filePath).writeText(fileContent)
    }
}