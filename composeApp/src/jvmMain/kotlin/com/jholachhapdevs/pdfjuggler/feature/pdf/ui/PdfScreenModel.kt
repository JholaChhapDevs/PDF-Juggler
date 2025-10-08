package com.jholachhapdevs.pdfjuggler.feature.pdf.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.PdfFile
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.usecase.GetPdfUseCase
import kotlinx.coroutines.launch

class PdfScreenModel(
    private val getPdfUseCase: GetPdfUseCase
) : ScreenModel {

    var pickedFile by mutableStateOf<PdfFile?>(null)
        private set

    fun pickPdfFile() {
        screenModelScope.launch {
            pickedFile = getPdfUseCase()
        }
    }

    // Consume once and clear so navigation doesn't retrigger on return
    fun consumePickedFile(): PdfFile? {
        val file = pickedFile
        pickedFile = null
        return file
    }
}