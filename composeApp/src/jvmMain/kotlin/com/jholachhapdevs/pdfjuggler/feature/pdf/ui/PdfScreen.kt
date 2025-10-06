package com.jholachhapdevs.pdfjuggler.feature.pdf.ui

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import com.jholachhapdevs.pdfjuggler.core.util.LocalComposeWindow
import com.jholachhapdevs.pdfjuggler.feature.pdf.data.repository.PdfFileRepository
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.usecase.GetPdfUseCase

object PdfScreen: Screen{
    private fun readResolve(): Any = PdfScreen

    @Composable
    override fun Content() {

        val window = LocalComposeWindow.current
        val screenModel = rememberScreenModel {
            PdfScreenModel(GetPdfUseCase(PdfFileRepository(window)))
        }
        PdfComponent(screenModel)
    }
}