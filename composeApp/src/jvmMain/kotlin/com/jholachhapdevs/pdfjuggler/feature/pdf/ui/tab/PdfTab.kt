package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.jholachhapdevs.pdfjuggler.feature.ai.data.remote.GeminiRemoteDataSource
import com.jholachhapdevs.pdfjuggler.feature.ai.domain.usecase.SendPromptUseCase
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.PdfFile
import com.jholachhapdevs.pdfjuggler.feature.ai.ui.AiScreenModel
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

class PdfTab(
    val pdfFile: PdfFile,
    private val modelProvider: (PdfFile) -> TabScreenModel
) : Tab {

    // Unique per tab instance to isolate AI chats/state
    private val tabId: String = UUID.randomUUID().toString()

    override val options: TabOptions
        @Composable
        get() = remember {
            TabOptions(
                index = nextIndex.getAndIncrement().toUShort(),
                title = pdfFile.name.ifBlank { "PDF" },
                icon = null
            )
        }

    @Composable
    override fun Content() {
        // PDF content model can stay cached by file path (if desired)
        val screenModel = rememberScreenModel(pdfFile.path) {
            modelProvider(pdfFile)
        }
        // AI chat model is scoped by unique tab id to keep chats separate per tab
        val aiScreenModel = rememberScreenModel("ai-$tabId") {
            AiScreenModel(
                pdfFile = pdfFile,
                sendPromptUseCase = SendPromptUseCase(GeminiRemoteDataSource())
            )
        }
        PdfDisplayArea(screenModel, aiScreenModel)
    }

    companion object {
        private val nextIndex = AtomicInteger(0)
    }
}