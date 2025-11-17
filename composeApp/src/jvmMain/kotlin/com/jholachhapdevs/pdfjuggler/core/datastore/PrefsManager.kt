package com.jholachhapdevs.pdfjuggler.core.datastore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Properties

object PrefsManager {

    private val mutex = Mutex()
    private val prefsFile: File by lazy {
        val dir = File(System.getProperty("user.home"), ".jugglerPrefs")
        if (!dir.exists()) dir.mkdirs()
        val f = File(dir, "juggler_prefs")
        if (!f.exists()) f.createNewFile()
        f
    }

    // --- Internal helpers ---
    private fun loadProperties(): Properties {
        val props = Properties()
        val decrypted = Decryption.readAndValidateFile(prefsFile.absolutePath) ?: ""
        if (decrypted.isNotBlank()) {
            props.load(decrypted.byteInputStream())
        }
        return props
    }

    private fun saveProperties(props: Properties) {
        val outBytes = ByteArrayOutputStream().use { baos ->
            props.store(baos, null)
            baos.toByteArray()
        }
        Encryption.writeSelfHealingFile(prefsFile.absolutePath, String(outBytes))
    }

    // --- Public API ---
    suspend fun getString(key: String): String? = withContext(Dispatchers.Default) {
        mutex.withLock { loadProperties().getProperty(key) }
    }

    suspend fun saveString(key: String, value: String) = withContext(Dispatchers.Default) {
        mutex.withLock {
            val props = loadProperties()
            props.setProperty(key, value)
            saveProperties(props)
        }
    }

    suspend fun getInt(key: String) = getString(key)?.toIntOrNull()
    suspend fun saveInt(key: String, value: Int) = saveString(key, value.toString())

    suspend fun getBoolean(key: String) = getString(key)?.toBooleanStrictOrNull()
    suspend fun saveBoolean(key: String, value: Boolean) = saveString(key, value.toString())

    suspend fun getDouble(key: String) = getString(key)?.toDoubleOrNull()
    suspend fun saveDouble(key: String, value: Double) = saveString(key, value.toString())

    suspend fun saveObject(key: String, value: Any) = saveString(key, value.toString())
    suspend inline fun <reified T> getObject(key: String, crossinline parser: (String) -> T): T? =
        getString(key)?.let { parser(it) }

    suspend fun delete(key: String) = withContext(Dispatchers.Default) {
        mutex.withLock {
            val props = loadProperties()
            props.remove(key)
            saveProperties(props)
        }
    }

    suspend fun clear() = withContext(Dispatchers.Default) {
        mutex.withLock { saveProperties(Properties()) }
    }
}