package com.jholachhapdevs.pdfjuggler.ui.pdf

import androidx.compose.runtime.*
import com.jholachhapdevs.pdfjuggler.domain.model.PdfTab
import com.jholachhapdevs.pdfjuggler.domain.model.TabContent
import java.util.*

class PdfViewModel {

    var tabs by mutableStateOf(listOf<PdfTab>())
        private set

    var selectedTabIndex by mutableStateOf(0)
        private set

    val currentTab: PdfTab?
        get() = tabs.getOrNull(selectedTabIndex)

    // Open a fresh Home tab
    fun newHomeTab() {
        val tab = PdfTab(
            id = UUID.randomUUID().toString(),
            title = "New Tab",
            content = TabContent.Home
        )
        tabs = tabs + tab
        selectedTabIndex = tabs.lastIndex
        println("New Home Tab Added → $tab")
    }

    fun switchTab(index: Int) {
        if (index in tabs.indices) {
            selectedTabIndex = index
            println("Switched to tab $index → ${tabs[index].content}")
        }
    }

    fun closeTab(index: Int) {
        if (index !in tabs.indices) return
        val mutable = tabs.toMutableList()
        mutable.removeAt(index)
        tabs = mutable
        selectedTabIndex = (selectedTabIndex.coerceAtMost(tabs.lastIndex)).coerceAtLeast(0)
    }

    fun openPdfInCurrentTab(filePath: String) {
        val totalPages = PdfUtils.getTotalPages(filePath)
        val thumbnails = PdfUtils.renderThumbnails(filePath, totalPages)
        val firstPage = PdfUtils.renderPage(filePath, 0)

        val current = currentTab ?: return

        val updated = current.copy(
            title = filePath.substringAfterLast("/"),
            content = TabContent.PdfViewer(
                filePath = filePath,
                totalPages = totalPages,
                currentPage = 0,
                thumbnails = thumbnails,
                pageBitmaps = mapOf(0 to firstPage)
            )
        )

        tabs = tabs.mapIndexed { i, tab ->
            if (i == selectedTabIndex) updated else tab
        }

        println("Opened PDF: ${updated.title} ($totalPages pages)")
    }

    fun selectPage(pageIndex: Int) {
        val current = currentTab ?: return
        if (current.content is TabContent.PdfViewer) {
            val pdf = current.content
            if (pageIndex in 0 until pdf.totalPages) {
                val updated = current.copy(content = pdf.copy(currentPage = pageIndex))
                tabs = tabs.mapIndexed { i, t -> if (i == selectedTabIndex) updated else t }
                println("Page switched to $pageIndex in tab $selectedTabIndex")
            }
        }
    }

    fun nextPage() {
        val tab = currentTab ?: return
        if (tab.content is TabContent.PdfViewer) {
            val pdf = tab.content
            if (pdf.currentPage < pdf.totalPages - 1) {
                val nextPage = pdf.currentPage + 1
                val existingBitmap = pdf.pageBitmaps[nextPage]
                    ?: PdfUtils.renderPage(pdf.filePath, nextPage)

                val updated = tab.copy(
                    content = pdf.copy(
                        currentPage = nextPage,
                        pageBitmaps = pdf.pageBitmaps + (nextPage to existingBitmap)
                    )
                )
                tabs = tabs.mapIndexed { i, t -> if (i == selectedTabIndex) updated else t }
            }
        }
    }


    fun prevPage() {
        val tab = currentTab ?: return
        if (tab.content is TabContent.PdfViewer) {
            val pdf = tab.content
            if (pdf.currentPage > 0) {
                val prevPage = pdf.currentPage - 1
                val existingBitmap = pdf.pageBitmaps[prevPage]
                    ?: PdfUtils.renderPage(pdf.filePath, prevPage)

                val updated = tab.copy(
                    content = pdf.copy(
                        currentPage = prevPage,
                        pageBitmaps = pdf.pageBitmaps + (prevPage to existingBitmap)
                    )
                )
                tabs = tabs.mapIndexed { i, t -> if (i == selectedTabIndex) updated else t }
            }
        }
    }

}
