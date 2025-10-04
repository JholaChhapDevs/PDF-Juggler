package com.jholachhapdevs.pdfjuggler.ui.pdf

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter


fun pickPdfFile(): String? {
    val chooser = JFileChooser().apply {
        dialogTitle = "Select a PDF"
        fileFilter = FileNameExtensionFilter("PDF files", "pdf")
        isAcceptAllFileFilterUsed = false
    }

    val result = chooser.showOpenDialog(null)
    return if (result == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile.absolutePath
    } else {
        null
    }
}

@Composable
fun HomeScreen(viewModel: PdfViewModel) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(onClick = {
            val filePath = pickPdfFile()
            if (filePath != null) {
                viewModel.openPdfInCurrentTab(filePath)
            }
        }) {
            Text("Choose a PDF to Open")
        }
    }
}