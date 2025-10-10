// kotlin
package com.jholachhapdevs.pdfjuggler.feature.ai.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.jholachhapdevs.pdfjuggler.feature.ai.domain.model.AttachedFile
import com.jholachhapdevs.pdfjuggler.feature.ai.domain.model.ChatMessage
import com.jholachhapdevs.pdfjuggler.feature.ai.domain.usecase.SendPromptUseCase
import com.jholachhapdevs.pdfjuggler.feature.ai.domain.usecase.UploadFileUseCase
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.PdfFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AiScreenModel(
    val pdfFile: PdfFile,
    val sendPromptUseCase: SendPromptUseCase,
    private val uploadFileUseCase: UploadFileUseCase,
    initialSelectedPageIndex: Int
) : ScreenModel {
    companion object {
        private const val MIME_PDF = "application/pdf"
    }

    var uiState by mutableStateOf(ChatUiState())
        private set

    private var currentJob: Job? = null

    // Cache: uploaded PDF fileUri
    private var pdfFileUri: String? = null

    // Current page index (for UI / optional prompt context)
    private var selectedPageIndex: Int = initialSelectedPageIndex

    init {
        // Upload the PDF once in the background
        screenModelScope.launch {
            try {
                ensurePdfFileAttachment()
            } catch (t: Throwable) {
                uiState = uiState.copy(error = "PDF upload failed: ${t.message}")
            }
        }
    }

    fun setSelectedPage(index: Int) {
        selectedPageIndex = index
    }

    fun updateInput(text: String) {
        uiState = uiState.copy(input = text)
    }

    fun send() {
        val prompt = uiState.input.trim()
        if (prompt.isBlank() || uiState.isSending) return

        val userMessage = ChatMessage(role = "user", text = prompt)
        uiState = uiState.copy(
            messages = uiState.messages + userMessage,
            input = "",
            isSending = true,
            error = null
        )

        currentJob = screenModelScope.launch {
            try {
                val attached = ensurePdfFileAttachment()
                val withAttachment = if (attached != null) {
                    // Attach the PDF to the last user message
                    uiState.messages.dropLast(1) + userMessage.copy(files = listOf(attached))
                } else {
                    uiState.messages
                }

                val reply = sendPromptUseCase(withAttachment)
                uiState = uiState.copy(messages = withAttachment + reply, isSending = false)
            } catch (t: Throwable) {
                uiState = uiState.copy(isSending = false, error = t.message ?: "Failed to send")
            }
        }
    }

    fun cancelSending() {
        val job = currentJob ?: return
        screenModelScope.launch {
            try {
                job.cancelAndJoin()
            } catch (_: Throwable) {
            }
            uiState = uiState.copy(isSending = false)
        }
    }

    fun clearMessages() {
        if (uiState.isSending) return
        uiState = uiState.copy(messages = emptyList())
    }

    fun newChat() {
        if (uiState.isSending) return
        uiState = ChatUiState()
    }

    fun dismissError() {
        uiState = uiState.copy(error = null)
    }

    private suspend fun ensurePdfFileAttachment(): AttachedFile? {
        val cached = pdfFileUri
        if (cached != null) {
            return AttachedFile(mimeType = MIME_PDF, fileUri = cached)
        }
        return try {
            val bytes = withContext(Dispatchers.IO) { File(pdfFile.path).readBytes() }
            val uri = uploadFileUseCase(pdfFile.name, MIME_PDF, bytes)
            pdfFileUri = uri
            AttachedFile(mimeType = MIME_PDF, fileUri = uri)
        } catch (_: Throwable) {
            null
        }
    }
}
