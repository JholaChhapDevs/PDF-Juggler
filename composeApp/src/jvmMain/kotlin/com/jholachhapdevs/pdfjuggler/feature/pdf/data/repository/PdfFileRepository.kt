package com.jholachhapdevs.pdfjuggler.feature.pdf.data.repository


import androidx.compose.ui.awt.ComposeWindow
import com.jholachhapdevs.pdfjuggler.feature.pdf.data.model.PdfFileData
import java.awt.FileDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PdfFileRepository(
    private val window: ComposeWindow
) {

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun pickPdfFile(): PdfFileData? {
        val dialog = FileDialog(window, "Select PDF", FileDialog.LOAD)
        dialog.file = "*.pdf"
        dialog.isVisible = true

        val selectedFile = dialog.files.firstOrNull() ?: return null

        val data = PdfFileData(
            name = selectedFile.name,
            uri = selectedFile.absolutePath
        )

        // Save to recent MRU in background (non-blocking)
        ioScope.launch {
            try {
                com.jholachhapdevs.pdfjuggler.core.datastore.RecentPdfsStore.addRecentPath(data.uri)
            } catch (_: Throwable) {
            }
        }

        return data
    }
}