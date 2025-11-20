package com.jholachhapdevs.pdfjuggler.feature.update.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.jholachhapdevs.pdfjuggler.feature.update.domain.usecase.GetUpdatesUseCase
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UpdateScreenModel(
    private val getUpdatesUseCase: GetUpdatesUseCase
) : ScreenModel {

    var uiState: UpdateUiState by mutableStateOf(UpdateUiState(loading = true))
        private set

    init {
        loadUpdateInfo()
    }

    private fun loadUpdateInfo() {
        screenModelScope.launch {
            uiState = UpdateUiState(loading = true)
            runCatching { getUpdatesUseCase() }
                .onSuccess { info ->
                    uiState = uiState.copy(loading = false, updateInfo = info, error = null)
                }
                .onFailure { e ->
                    uiState = uiState.copy(loading = false, updateInfo = null, error = e.message ?: "Unknown error")
                }
        }
    }

    fun showChangelog(show: Boolean) {
        uiState = uiState.copy(showChangelog = show)
    }

    /**
     * Download an update from [url].
     * If [expectedChecksum] is provided it will be used to verify the downloaded file;
     * otherwise the checksum from the currently loaded `uiState.updateInfo` is used (if any).
     */
    fun downloadUpdate(url: String, fileNameHint: String? = null, expectedChecksum: String? = null) {
        if (uiState.isDownloading) return
        screenModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    uiState = uiState.copy(
                        isDownloading = true,
                        downloadProgress = null,
                        downloadedPath = null,
                        downloadedBytes = 0,
                        totalBytes = null,
                        downloadStartedAtMillis = System.currentTimeMillis(),
                        error = null
                    )
                }

                val targetName = (fileNameHint ?: url.substringAfterLast('/')).ifBlank { "update.bin" }
                val downloadsDir = java.io.File(System.getProperty("user.home"), "Downloads")
                val baseDir = if (downloadsDir.exists() && downloadsDir.isDirectory) {
                    java.io.File(downloadsDir, "pdfjuggler-updates")
                } else {
                    java.io.File(System.getProperty("java.io.tmpdir"), "pdfjuggler-updates")
                }
                val pdfFolder = java.io.File(baseDir, "PDF-Juggler").apply { mkdirs() }

                // Clean previous files
                pdfFolder.listFiles()?.forEach { f ->
                    runCatching { f.deleteRecursively() }
                }

                val finalFile = java.io.File(pdfFolder, targetName)
                val tempFile = java.io.File(pdfFolder, "$targetName.part")

                val client = com.jholachhapdevs.pdfjuggler.core.networking.httpClient
                val response = client.get(url)
                if (!response.status.isSuccess()) {
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        uiState = uiState.copy(isDownloading = false, error = "HTTP ${response.status.value}")
                    }
                    return@launch
                }

                val total = response.headers[io.ktor.http.HttpHeaders.ContentLength]?.toLongOrNull()
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    uiState = uiState.copy(totalBytes = total)
                }

                val channel = response.bodyAsChannel()
                var downloaded = 0L
                java.io.FileOutputStream(tempFile).use { fos ->
                    val buffer = ByteArray(8192)
                    var lastUi = 0L
                    while (!channel.isClosedForRead) {
                        if (!kotlin.coroutines.coroutineContext[kotlinx.coroutines.Job]!!.isActive) break
                        val n = channel.readAvailable(buffer, 0, buffer.size)
                        if (n <= 0) break
                        fos.write(buffer, 0, n)
                        downloaded += n
                        val now = System.currentTimeMillis()
                        if (now - lastUi >= 100L) {
                            val p = if (total != null && total > 0) (downloaded.toDouble() / total).toFloat() else null
                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                uiState = uiState.copy(downloadProgress = p, downloadedBytes = downloaded)
                            }
                            lastUi = now
                        }
                    }
                }

                // Checksum
                val expectedChecksumRaw = expectedChecksum ?: uiState.updateInfo?.checksum
                var verificationError: String? = null
                if (!expectedChecksumRaw.isNullOrBlank()) {
                    try {
                        val (algorithm, expectedHash) = parseChecksum(expectedChecksumRaw)
                        val allowed = setOf("SHA-256", "SHA-1", "MD5")
                        if (algorithm !in allowed) {
                            verificationError = "Unsupported checksum algorithm: $algorithm"
                        } else {
                            val digestBytes = computeFileDigest(tempFile, algorithm)
                            val actualHex = toHex(digestBytes)
                            val actualBase64 = java.util.Base64.getEncoder().encodeToString(digestBytes)
                            val expectedIsHex = expectedHash.matches(Regex("^[0-9a-fA-F]+$"))
                            val match = if (expectedIsHex) {
                                actualHex.equals(expectedHash, ignoreCase = true)
                            } else {
                                actualBase64 == expectedHash
                            }
                            if (!match) {
                                verificationError = "Checksum mismatch"
                            }
                        }
                    } catch (e: Exception) {
                        verificationError = "Checksum failed: ${e.message}"
                    }
                }

                if (verificationError == null) {
                    tempFile.renameTo(finalFile)
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        uiState = uiState.copy(
                            isDownloading = false,
                            downloadedPath = finalFile.absolutePath,
                            downloadProgress = 1f,
                            downloadedBytes = downloaded
                        )
                    }
                } else {
                    runCatching { tempFile.delete() }
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        uiState = uiState.copy(isDownloading = false, error = verificationError)
                    }
                }
            } catch (t: Throwable) {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    uiState = uiState.copy(isDownloading = false, error = t.message ?: "Download failed")
                }
            }
        }
    }
}

// Helpers for checksum parsing and computation
private fun parseChecksum(raw: String): Pair<String, String> {
    val lower = raw.trim()
    return if (lower.contains('-')) {
        val alg = lower.substringBefore('-').lowercase()
        val hash = lower.substringAfter('-')
        val jAlg = when (alg) {
            "sha256" -> "SHA-256"
            "sha1" -> "SHA-1"
            "md5" -> "MD5"
            else -> alg.uppercase()
        }
        Pair(jAlg, hash)
    } else {
        Pair("SHA-256", lower)
    }
}

private fun computeFileDigest(file: java.io.File, algorithm: String): ByteArray {
    val md = java.security.MessageDigest.getInstance(algorithm)
    file.inputStream().use { fis ->
        val buffer = ByteArray(8192)
        var read: Int
        while (fis.read(buffer).also { read = it } > 0) {
            md.update(buffer, 0, read)
        }
    }
    return md.digest()
}

private fun toHex(bytes: ByteArray): String = buildString {
    for (b in bytes) append(String.format("%02x", b))
}
