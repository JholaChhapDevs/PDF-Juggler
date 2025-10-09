// Kotlin
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
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO

class AiScreenModel(
    val pdfFile: PdfFile,
    val sendPromptUseCase: SendPromptUseCase,
    initialSelectedPageIndex: Int
) : ScreenModel {

    companion object {
        private const val CONTEXT_CHAR_LIMIT = 120_000
        private const val WINDOW_SIZE = 5
        private const val PAGE_DPI = 150f
    }

    var uiState by mutableStateOf(ChatUiState())
        private set

    private val scope = CoroutineScope(Dispatchers.Main)
    private var currentJob: Job? = null

    // Text context (once)
    private var pdfText: String? = null
    private var contextInjected = false

    // PDF meta
    private var totalPages: Int? = null

    // Caches: page -> base64 image; page -> already sent in this chat
    private var pageBase64Cache: MutableList<String?>? = null
    private var pageAlreadySent: BooleanArray? = null

    // Currently selected page (kept in sync by the UI)
    private var selectedPageIndex: Int = initialSelectedPageIndex

    fun setSelectedPage(index: Int) {
        selectedPageIndex = index
    }

    init {
        // Preload context and initialize caches
        scope.launch(Dispatchers.IO) {
            try {
                pdfText = safelyExtractPdfText(pdfFile.path)?.let { compressAndLimit(it) }
            } catch (_: Throwable) {
                // ignore, context is optional
            }
            ensureTotalPagesAndInitCaches()
        }
    }

    fun updateInput(text: String) {
        uiState = uiState.copy(input = text)
    }

    fun send() {
        val prompt = uiState.input.trim()
        if (prompt.isBlank() || uiState.isSending) return

        // Show user's message in the UI as typed
        val uiWithUser = uiState.messages + ChatMessage(role = "user", text = prompt)
        uiState = uiState.copy(messages = uiWithUser, input = "", isSending = true, error = null)

        currentJob = scope.launch {
            try {
                val total = ensureTotalPagesAndInitCaches()
                val selectedDisplay = (selectedPageIndex + 1).coerceIn(1, total.coerceAtLeast(1))
                val selectionInfo = "Selected page: $selectedDisplay / $total"

                // Build the list we actually send to the model
                val toModel = uiState.messages.toMutableList()

                // Replace the last user message with an augmented one (prompt + selection + tokens)
                if (toModel.isNotEmpty() && toModel.last().role == "user") {
                    val last = toModel.removeAt(toModel.lastIndex)
                    val tokens = ensureFivePageWindowTokens()
                    val enhanced = buildString {
                        append(last.text)
                        append("\n\n")
                        append(selectionInfo)
                        if (!tokens.isNullOrBlank()) {
                            append("\n\n")
                            append(tokens)
                        }
                    }
                    toModel += last.copy(text = enhanced)
                }

                // Inject compressed full-text context once per chat
                if (!contextInjected) {
                    ensureContextMessage()?.let { ctx ->
                        // Put context just before the last message so the LLM still "answers" the user's prompt
                        val insertAt = (toModel.lastIndex).coerceAtLeast(0)
                        toModel.add(insertAt, ctx)
                        contextInjected = true
                    }
                }

                val modelMsg = withContext(Dispatchers.IO) {
                    sendPromptUseCase(toModel)
                }

                uiState = uiState.copy(
                    messages = uiState.messages + modelMsg,
                    isSending = false,
                    error = null
                )
            } catch (e: Throwable) {
                uiState = uiState.copy(
                    isSending = false,
                    error = e.message ?: "Failed to send"
                )
            }
        }
    }

    fun cancelSending() {
        val job = currentJob ?: return
        scope.launch {
            try {
                job.cancelAndJoin()
            } catch (_: Throwable) {
            } finally {
                uiState = uiState.copy(isSending = false)
            }
        }
    }

    fun clearMessages() {
        if (uiState.isSending) return
        uiState = uiState.copy(messages = emptyList())
    }

    fun newChat() {
        if (uiState.isSending) return
        uiState = ChatUiState()
        contextInjected = false
        pageAlreadySent?.fill(false)
    }

    fun dismissError() {
        uiState = uiState.copy(error = null)
    }

    private suspend fun ensureContextMessage(): ChatMessage? {
        val text = pdfText ?: withContext(Dispatchers.IO) {
            safelyExtractPdfText(pdfFile.path)?.let { compressAndLimit(it) }?.also { pdfText = it }
        }
        if (text.isNullOrBlank()) return null
        val msgText = "PDF context — `${pdfFile.name}`:\n\n$text"
        return ChatMessage(role = "user", text = msgText)
    }

    // Build tokens for up to 5 unseen pages around the selected index.
    // Returns a string with a short header and inline [[image/png;base64,<data>]] tokens.
    private suspend fun ensureFivePageWindowTokens(): String? = withContext(Dispatchers.IO) {
        val total = ensureTotalPagesAndInitCaches()
        if (total <= 0) return@withContext null

        val base64Cache = pageBase64Cache ?: return@withContext null
        val sent = pageAlreadySent ?: return@withContext null

        // Compute clamped 5-page window [selected-2 .. selected+2], filled near edges
        val windowSize = minOf(WINDOW_SIZE, total)
        var start = selectedPageIndex - 2
        var end = start + windowSize - 1
        if (start < 0) {
            start = 0
            end = windowSize - 1
        }
        if (end >= total) {
            end = total - 1
            start = (end - windowSize + 1).coerceAtLeast(0)
        }

        val window = (start..end).toList()

        // First, take unseen pages inside the window (ascending order)
        val chosen = mutableListOf<Int>()
        for (i in window) if (!sent[i]) chosen += i

        // If fewer than 5 unseen pages, expand outward to gather more unseen
        if (chosen.size < windowSize) {
            var left = start - 1
            var right = end + 1
            val preferLeft = end == total - 1
            val preferRight = start == 0

            while (chosen.size < windowSize && (left >= 0 || right < total)) {
                if (preferLeft && left >= 0) {
                    if (!sent[left]) chosen += left
                    left--
                } else if (preferRight && right < total) {
                    if (!sent[right]) chosen += right
                    right++
                } else {
                    var progressed = false
                    if (right < total) {
                        if (!sent[right]) chosen += right
                        right++
                        progressed = true
                    }
                    if (chosen.size < windowSize && left >= 0) {
                        if (!sent[left]) chosen += left
                        left--
                        progressed = true
                    }
                    if (!progressed) break
                }
            }
        }

        if (chosen.isEmpty()) return@withContext null

        // Render any missing base64s in one pass
        val needRender = chosen.filter { base64Cache[it].isNullOrBlank() }
        if (needRender.isNotEmpty()) {
            try {
                PDDocument.load(File(pdfFile.path)).use { document ->
                    val renderer = PDFRenderer(document)
                    for (i in needRender) {
                        val img = renderer.renderImageWithDPI(i, PAGE_DPI, ImageType.RGB)
                        val baos = ByteArrayOutputStream()
                        ImageIO.write(img, "png", baos)
                        base64Cache[i] = Base64.getEncoder().encodeToString(baos.toByteArray())
                    }
                }
            } catch (_: Throwable) {
                // Ignore rendering errors for now; we'll skip pages without base64
            }
        }

        // Mark chosen pages as sent and assemble message
        val actuallyAvailable = chosen.filter { !base64Cache[it].isNullOrBlank() }
        if (actuallyAvailable.isEmpty()) return@withContext null

        for (i in actuallyAvailable) sent[i] = true

        val pagesHeader = "Pages sent: " + actuallyAvailable.map { it + 1 }.joinToString(", ")
        val body = actuallyAvailable.joinToString("\n\n") { i ->
            val pageNum = i + 1 // 1-based for display
            val b64 = base64Cache[i]!!
            "Page $pageNum:\n[[image/png;base64,$b64]]"
        }
        "$pagesHeader\n\n$body"
    }

    private suspend fun ensureTotalPagesAndInitCaches(): Int = withContext(Dispatchers.IO) {
        totalPages?.let { return@withContext it }
        val pages = try {
            PDDocument.load(File(pdfFile.path)).use { it.numberOfPages }
        } catch (_: Throwable) {
            0
        }
        totalPages = pages
        pageBase64Cache = MutableList(pages) { null }
        pageAlreadySent = BooleanArray(pages) { false }
        pages
    }

    private fun safelyExtractPdfText(path: String): String? = try {
        PDDocument.load(File(path)).use { doc ->
            PDFTextStripper().apply {
                sortByPosition = true
                startPage = 1
                endPage = doc.numberOfPages
            }.getText(doc)
        }
    } catch (_: Throwable) {
        null
    }

    private fun compressAndLimit(text: String): String {
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