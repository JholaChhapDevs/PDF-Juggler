package com.jholachhapdevs.pdfjuggler.ui.pdf

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import com.jholachhapdevs.pdfjuggler.domain.model.TabContent


@Composable
fun PdfScreen(viewModel: PdfViewModel) {
    val current = viewModel.currentTab

    Column(Modifier.fillMaxSize()) {
        TabBar(
            tabs = viewModel.tabs,
            selectedIndex = viewModel.selectedTabIndex,
            onTabSelected = { viewModel.switchTab(it) },
            onNewTab = { viewModel.newHomeTab() },
            onCloseTab = { viewModel.closeTab(it) }
        )
        when (val c = current?.content) {
            is TabContent.Home -> println("Current content = Home")
            is TabContent.PdfViewer -> println("Current content = PdfViewer(${c.filePath}, page=${c.currentPage})")
            null -> println("No current tab")
        }


        when (val content = current?.content) {

            is TabContent.PdfViewer -> Row(Modifier.fillMaxSize()) {
                println("pdfViewer")
                LeftPanel(
                    thumbnails = content.thumbnails,
                    currentPage = content.currentPage,
                    onPageClick = { viewModel.selectPage(it) },
                    modifier = Modifier.weight(0.25f)
                )
                MiddlePanel(
                    pageBitmap = content.pageBitmaps[content.currentPage], // new
                    currentPage = content.currentPage,
                    totalPages = content.totalPages,
                    onNext = { viewModel.nextPage() },
                    onPrev = { viewModel.prevPage() },
                    modifier = Modifier.weight(0.75f)
                )
            }
            is TabContent.Home -> {
                HomeScreen(
                    viewModel
                )
            }

            null -> Text("No tab open")
        }
    }

}