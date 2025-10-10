package com.jholachhapdevs.pdfjuggler.feature.pdf.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.tab.Tab
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.PdfFile
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.usecase.GetPdfUseCase
import com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab.PdfTab
import com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab.TabScreenModel
import kotlinx.coroutines.launch

class PdfTabScreenModel(
    private val getPdfUseCase: GetPdfUseCase,
    private val window: java.awt.Window,
    initial: PdfFile? = null
) : ScreenModel {

    val tabs = mutableStateListOf<Tab>()
    var current: Tab? by mutableStateOf(null)
        private set

    // Split view support
    var isSplitViewEnabled by mutableStateOf(false)
        private set
    var splitViewLeftTab: Tab? by mutableStateOf(null)
        private set
    var splitViewRightTab: Tab? by mutableStateOf(null)
        private set
        
    // AI chat support
    var isAiChatEnabled by mutableStateOf(false)
        private set

    private val contentCache = LinkedHashMap<String, TabScreenModel>()

    init {
        initial?.let { addTab(it) }
    }

    fun addTabFromPicker() {
        screenModelScope.launch {
            val file = getPdfUseCase() ?: return@launch
            addTab(file)
        }
    }

    fun selectTab(tab: Tab) {
        if (tab in tabs) current = tab
    }

    fun closeTab(tab: Tab) {
        val idx = tabs.indexOf(tab)
        if (idx < 0) return
        val wasCurrent = current == tab

        val path = (tab as? PdfTab)?.pdfFile?.path

        tabs.removeAt(idx)

        if (path != null && tabs.none { (it as? PdfTab)?.pdfFile?.path == path }) {
            contentCache.remove(path)?.let {
            }
        }

        if (tabs.isEmpty()) {
            current = null
            return
        }
        if (wasCurrent) {
            val newIndex = (idx - 1).coerceAtLeast(0).coerceAtMost(tabs.lastIndex)
            current = tabs[newIndex]
        }
    }

    private fun addTab(file: PdfFile) {
        val tab = PdfTab(
            pdfFile = file,
            modelProvider = { f -> getOrCreateModel(f) }
        )
        tabs += tab
        current = tab
    }

    private fun getOrCreateModel(file: PdfFile): TabScreenModel {
        return contentCache.getOrPut(file.path) { TabScreenModel(file, window) }
    }

    // Split view functions
    fun enableSplitView() {
        if (tabs.size >= 2) {
            isSplitViewEnabled = true
            splitViewLeftTab = tabs[0]
            splitViewRightTab = tabs[1]
        } else if (tabs.size == 1) {
            isSplitViewEnabled = true
            splitViewLeftTab = tabs[0]
            splitViewRightTab = null
        }
    }

    fun disableSplitView() {
        isSplitViewEnabled = false
        splitViewLeftTab = null
        splitViewRightTab = null
    }

    fun setSplitViewLeft(tab: Tab?) {
        if (tab == null || tab in tabs) {
            splitViewLeftTab = tab
        }
    }

    fun setSplitViewRight(tab: Tab?) {
        if (tab == null || tab in tabs) {
            splitViewRightTab = tab
        }
    }

    fun toggleSplitView() {
        if (isSplitViewEnabled) {
            disableSplitView()
        } else {
            enableSplitView()
        }
    }
    
    // AI chat functions
    fun toggleAiChat() {
        isAiChatEnabled = !isAiChatEnabled
    }
    fun setAiChatVisible(enabled: Boolean) {
        isAiChatEnabled = enabled
    }

    fun getTabModel(tab: Tab?): TabScreenModel? {
        return (tab as? PdfTab)?.pdfFile?.let { getOrCreateModel(it) }
    }

    fun getCurrentTabModel(): TabScreenModel? {
        val currentPdfTab = current as? PdfTab ?: return null
        return contentCache[currentPdfTab.pdfFile.path]
    }
}