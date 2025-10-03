package com.jholachhapdevs.pdfjuggler.domain.model

import androidx.compose.ui.graphics.ImageBitmap


sealed class TabContent {
    object Home : TabContent()
    data class PdfViewer(
        val filePath: String,
        val totalPages: Int,
        val currentPage: Int = 0,
        val thumbnails: List<ImageBitmap?> = emptyList(),   // new
        val pageBitmaps: Map<Int, ImageBitmap?> = emptyMap() // new
    ) : TabContent()
}

data class PdfTab(
    val id: String,
    val title: String,
    val content: TabContent
)
