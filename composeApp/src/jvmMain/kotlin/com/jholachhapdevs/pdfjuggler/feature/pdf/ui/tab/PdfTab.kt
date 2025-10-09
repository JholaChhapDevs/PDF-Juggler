package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.jholachhapdevs.pdfjuggler.feature.ai.data.remote.GeminiRemoteDataSource
import com.jholachhapdevs.pdfjuggler.feature.ai.domain.usecase.SendPromptUseCase
import com.jholachhapdevs.pdfjuggler.feature.ai.domain.usecase.UploadFileUseCase
import com.jholachhapdevs.pdfjuggler.feature.ai.ui.AiScreenModel
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.PdfFile
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

class PdfTab(
    val pdfFile: PdfFile,
    private val modelProvider: (PdfFile) -> TabScreenModel
) : Tab {

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
        val screenModel = rememberScreenModel(pdfFile.path) {
            modelProvider(pdfFile)
        }
        val remote = remember { GeminiRemoteDataSource() }
        val aiScreenModel = rememberScreenModel("ai-$tabId") {
            AiScreenModel(
                pdfFile = pdfFile,
                sendPromptUseCase = SendPromptUseCase(remote),
                uploadFileUseCase = UploadFileUseCase(remote),
                initialSelectedPageIndex = screenModel.selectedPageIndex
            )
        }
        PdfDisplayArea(screenModel, aiScreenModel)
    }

    companion object {
        private val nextIndex = AtomicInteger(0)
    }
}