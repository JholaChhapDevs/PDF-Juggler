package com.jholachhapdevs.pdfjuggler.feature.pdf.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.jholachhapdevs.pdfjuggler.core.datastore.PrefsManager
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.PdfFile
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.usecase.GetPdfUseCase
import kotlinx.coroutines.launch

class PdfScreenModel(
    private val getPdfUseCase: GetPdfUseCase
) : ScreenModel {

    var pickedFile by mutableStateOf<PdfFile?>(null)
        private set

    var showApiKeyDialog by mutableStateOf(false)
        private set

    var apiKey by mutableStateOf("")
        private set

    init {
        screenModelScope.launch {
            apiKey = com.jholachhapdevs.pdfjuggler.core.datastore.ApiKeyStore.getGeminiApiKey() ?: ""
        }
    }

    fun openApiKeyDialog() {
        showApiKeyDialog = true
    }

    fun closeApiKeyDialog() {
        showApiKeyDialog = false
    }

    fun saveApiKey(key: String) {
        screenModelScope.launch {
            com.jholachhapdevs.pdfjuggler.core.datastore.ApiKeyStore.saveGeminiApiKey(key.trim())
            apiKey = key.trim()
        }
    }

    fun clearApiKey() {
        screenModelScope.launch {
            com.jholachhapdevs.pdfjuggler.core.datastore.ApiKeyStore.clearGeminiApiKey()
            apiKey = ""
        }
    }

    fun pickPdfFile() {
        screenModelScope.launch {
            pickedFile = getPdfUseCase()
        }
    }

    fun consumePickedFile(): PdfFile? {
        val file = pickedFile
        pickedFile = null
        return file
    }
}
