package com.jholachhapdevs.pdfjuggler.feature.update.ui

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.jholachhapdevs.pdfjuggler.feature.update.domain.usecase.GetUpdatesUseCase
import kotlinx.coroutines.CancellationException
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

    fun downloadUpdate(url: String, fileNameHint: String? = null) {
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
                        downloadStartedAtMillis = System.currentTimeMillis()
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
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    uiState = uiState.copy(isDownloading = false, downloadedPath = outFile.absolutePath, downloadProgress = 1f, downloadedBytes = uiState.totalBytes ?: uiState.downloadedBytes)
                }
            } catch (t: Throwable) {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    uiState = uiState.copy(isDownloading = false, error = t.message ?: "Download failed")
                }
            }
        }
    }
}
