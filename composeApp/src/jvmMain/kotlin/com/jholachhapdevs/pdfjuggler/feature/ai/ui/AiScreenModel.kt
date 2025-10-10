package com.jholachhapdevs.pdfjuggler.feature.ai.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import com.jholachhapdevs.pdfjuggler.feature.ai.domain.model.ChatMessage
import com.jholachhapdevs.pdfjuggler.feature.ai.domain.usecase.SendPromptUseCase
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.PdfFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File

class AiScreenModel(
    val pdfFile: PdfFile,
    private val sendPromptUseCase: SendPromptUseCase
) : ScreenModel {

    companion object {
        private const val CONTEXT_CHAR_LIMIT = 12000 // keep request compact
    }

    var uiState by mutableStateOf(ChatUiState())
        private set

    private val scope = CoroutineScope(Dispatchers.Main)
    private var currentJob: Job? = null

    // Cached extracted text and a flag to inject only once
    private var pdfText: String? = null
    private var contextInjected = false

    init {
        // Preload PDF text off the main thread (best-effort)
        scope.launch(Dispatchers.IO) {
            pdfText = safelyExtractPdfText(pdfFile.path)?.let { compressAndLimit(it) }
        }
    }

    fun updateInput(text: String) {
        uiState = uiState.copy(input = text)
    }

    fun send() {
        val prompt = uiState.input.trim()
        if (prompt.isBlank() || uiState.isSending) return

        // Update UI immediately with the user's message
        val uiWithUser = uiState.messages + ChatMessage(role = "user", text = prompt)
        uiState = uiState.copy(messages = uiWithUser, input = "", isSending = true, error = null)

        currentJob = scope.launch {
            try {
                // Build the payload to the model: optionally prepend hidden PDF context once
                val toModel = mutableListOf<ChatMessage>()

                if (!contextInjected) {
                    val ctx = ensureContextMessage()
                    if (ctx != null) {
                        toModel += ctx
                        contextInjected = true
                    }
                }

                toModel += uiWithUser

                val response = sendPromptUseCase(toModel)
                uiState = uiState.copy(messages = uiState.messages + response)
            } catch (t: Throwable) {
                uiState = uiState.copy(error = t.message ?: "Something went wrong")
            } finally {
                uiState = uiState.copy(isSending = false)
                currentJob = null
            }
        }
    }

    fun cancelSending() {
        val job = currentJob ?: return
        scope.launch {
            try {
                job.cancelAndJoin()
            } finally {
                uiState = uiState.copy(isSending = false)
                currentJob = null
            }
        }
    }

    fun clearMessages() {
        if (uiState.isSending) return
        uiState = uiState.copy(messages = emptyList())
        // Do not reset contextInjected so new sends still include context when needed
    }

    fun newChat() {
        if (uiState.isSending) return
        uiState = ChatUiState()
        contextInjected = false
    }

    fun dismissError() {
        uiState = uiState.copy(error = null)
    }

    private suspend fun ensureContextMessage(): ChatMessage? {
        val text = pdfText ?: withContext(Dispatchers.IO) {
            safelyExtractPdfText(pdfFile.path)?.let { compressAndLimit(it) }
                .also { pdfText = it }
        }

        if (text.isNullOrBlank()) return null

        val msgText = "PDF context — `${pdfFile.name}`: $text"
        return ChatMessage(role = "user", text = msgText)
    }

    private fun safelyExtractPdfText(path: String): String? = try {
        PDDocument.load(File(path)).use { doc ->
            PDFTextStripper().apply {
                sortByPosition = true
                startPage = 1
                endPage = Integer.MAX_VALUE
            }.getText(doc)
        }
    } catch (_: Throwable) {
        null
    }

    private fun compressAndLimit(text: String): String {
        // Keep paragraphs but normalize whitespace; then truncate
        val normalized = text
            .replace("\r", "")
            .replace(Regex("[\\t\\u00A0]"), " ")
            .replace(Regex(" +"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
        return if (normalized.length > CONTEXT_CHAR_LIMIT)
            normalized.substring(0, CONTEXT_CHAR_LIMIT)
        else
            normalized
    }
}