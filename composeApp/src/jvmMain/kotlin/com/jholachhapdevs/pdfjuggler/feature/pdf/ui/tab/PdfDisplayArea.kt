package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jholachhapdevs.pdfjuggler.feature.ai.ui.AiScreenModel
import com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab.displayarea.PdfLeft
import com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab.displayarea.PdfMid
import com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab.displayarea.PdfRight

@Composable
fun PdfDisplayArea(
    tabScreenModel: TabScreenModel,
    aiScreenModel: AiScreenModel
) {
    val listState = rememberLazyListState()

    // Keep left pane scrolled to the selected page
    LaunchedEffect(tabScreenModel.pdfFile.path) {
        if (tabScreenModel.thumbnails.isNotEmpty()) {
            val idx = tabScreenModel.selectedPageIndex.coerceIn(0, tabScreenModel.thumbnails.lastIndex)
            listState.scrollToItem(idx, 0)
        }
    }

    // Keep AI model in sync with the current selected page
    LaunchedEffect(tabScreenModel.selectedPageIndex) {
        aiScreenModel.setSelectedPage(tabScreenModel.selectedPageIndex)
    }

    if (tabScreenModel.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else {
        Row(Modifier.fillMaxSize()) {
            PdfLeft(
                modifier = Modifier.weight(0.15f).fillMaxSize(),
                thumbnails = tabScreenModel.thumbnails,
                selectedIndex = tabScreenModel.selectedPageIndex,
                onThumbnailClick = { tabScreenModel.selectPage(it) },
                listState = listState
            )
            PdfMid(
                modifier = Modifier.weight(0.65f).fillMaxSize(),
                pageImage = tabScreenModel.currentPageImage
            )
            PdfRight(
                modifier = Modifier.weight(0.30f).fillMaxSize(),
                screenModel = aiScreenModel
            )
        }
    }
}