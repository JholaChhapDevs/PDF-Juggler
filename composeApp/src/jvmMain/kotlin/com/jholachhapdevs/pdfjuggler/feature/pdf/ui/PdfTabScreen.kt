// Kotlin
package com.jholachhapdevs.pdfjuggler.feature.pdf.ui

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import com.jholachhapdevs.pdfjuggler.core.util.LocalComposeWindow
import com.jholachhapdevs.pdfjuggler.feature.pdf.data.repository.PdfFileRepository
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.PdfFile
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.usecase.GetPdfUseCase

data class PdfTabScreen(
    val pdfFile: PdfFile
) : Screen {

    @Composable
    override fun Content() {
        val window = LocalComposeWindow.current

        val model = rememberScreenModel {
            PdfTabScreenModel(
                getPdfUseCase = GetPdfUseCase(PdfFileRepository(window)),
                initial = pdfFile
            )
        }

        PdfTabComponent(model)
    }
}