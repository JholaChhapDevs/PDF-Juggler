package com.jholachhapdevs.pdfjuggler.feature.update.ui

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.jholachhapdevs.pdfjuggler.feature.update.domain.usecase.GetUpdatesUseCase
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.readAvailable

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
                val targetName = fileNameHint ?: url.substringAfterLast('/')
                val tempBase = java.io.File(System.getProperty("java.io.tmpdir"), "pdfjuggler-updates")
                if (!tempBase.exists()) tempBase.mkdirs()
                val outFile = java.io.File(tempBase, targetName.ifBlank { "update.bin" })
                try { outFile.deleteOnExit() } catch (_: Throwable) {}

                val client = com.jholachhapdevs.pdfjuggler.core.networking.httpClient
                val response = client.get(url)
                val total: Long? = response.headers[io.ktor.http.HttpHeaders.ContentLength]?.toLongOrNull()
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    uiState = uiState.copy(totalBytes = total)
                }
                val channel = response.bodyAsChannel()
                java.io.FileOutputStream(outFile).use { fos ->
                    var downloaded = 0L
                    val buffer = ByteArray(8192)
                    var lastUi = 0L
                    while (!channel.isClosedForRead) {
                        val n = channel.readAvailable(buffer, 0, buffer.size)
                        if (n <= 0) break
                        fos.write(buffer, 0, n)
                        downloaded += n
                        val now = System.currentTimeMillis()
                        if (now - lastUi >= 100L) {
                            val p = if (total != null && total > 0) (downloaded.toDouble() / total.toDouble()).toFloat() else null
                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                uiState = uiState.copy(downloadProgress = p, downloadedBytes = downloaded, totalBytes = total)
                            }
                            lastUi = now
                        }
                    }
                }

                // After download completes, verify checksum if available
                val expectedChecksumRaw = expectedChecksum ?: uiState.updateInfo?.checksum

                var verificationError: String? = null
                if (!expectedChecksumRaw.isNullOrBlank()) {
                    try {
                        val (algorithm, expectedHash) = parseChecksum(expectedChecksumRaw)

                        val digestBytes = computeFileDigest(outFile, algorithm)
                        val actualHex = toHex(digestBytes)
                        val actualBase64 = java.util.Base64.getEncoder().encodeToString(digestBytes)

                        val expectedIsHex = expectedHash.matches(Regex("^[0-9a-fA-F]+$"))
                        val match = if (expectedIsHex) {
                            actualHex.equals(expectedHash, ignoreCase = true)
                        } else {
                            actualBase64 == expectedHash
                        }

                        if (!match) {
                            verificationError = "Checksum mismatch: expected=$expectedHash, actual(hex)=$actualHex"
                        } else {
                            // verification succeeded; nothing to do
                        }
                    } catch (e: Exception) {
                        val exMsg = e.message ?: e.toString()
                        verificationError = "Checksum verification failed: $exMsg"
                    }
                }

                if (verificationError == null) {
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        uiState = uiState.copy(isDownloading = false, downloadedPath = outFile.absolutePath, downloadProgress = 1f, downloadedBytes = uiState.totalBytes ?: uiState.downloadedBytes)
                    }
                } else {
                    // delete the bad file
                    try { outFile.delete() } catch (_: Throwable) {}
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        uiState = uiState.copy(isDownloading = false, downloadedPath = null, error = verificationError)
                    }
                }
            } catch (t: Throwable) {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    uiState = uiState.copy(isDownloading = false, error = t.message ?: "Download failed")
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
}
