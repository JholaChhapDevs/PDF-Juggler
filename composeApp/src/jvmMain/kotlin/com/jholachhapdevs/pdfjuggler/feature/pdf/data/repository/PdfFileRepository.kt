package com.jholachhapdevs.pdfjuggler.feature.pdf.data.repository


import androidx.compose.ui.awt.ComposeWindow
import com.jholachhapdevs.pdfjuggler.feature.pdf.data.model.PdfFileData
import java.awt.FileDialog

class PdfFileRepository(
    private val window: ComposeWindow
) {

    fun pickPdfFile(): PdfFileData? {
        val dialog = FileDialog(window, "Select PDF", FileDialog.LOAD)
        dialog.file = "*.pdf"
        dialog.isVisible = true

        val selectedFile = dialog.files.firstOrNull() ?: return null

        return PdfFileData(
            name = selectedFile.name,
            uri = selectedFile.absolutePath
        )
    }
}