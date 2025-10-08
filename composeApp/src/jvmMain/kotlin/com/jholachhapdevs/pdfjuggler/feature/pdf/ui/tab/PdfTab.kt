package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.PdfFile
import java.util.concurrent.atomic.AtomicInteger

class PdfTab(
    val pdfFile: PdfFile,
    private val modelProvider: (PdfFile) -> TabScreenModel
) : Tab {

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
        // Get the cached model once per file path
        val model = remember(pdfFile.path) { modelProvider(pdfFile) }
        PdfDisplayArea(model)
    }

    companion object {
        private val nextIndex = AtomicInteger(0)
    }
}