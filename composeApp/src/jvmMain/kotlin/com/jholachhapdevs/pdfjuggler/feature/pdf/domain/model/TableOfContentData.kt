package com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model

data class TableOfContentData(
    val title: String,          // The title of the section
    val pageIndex: Int,         // The 0-based index of the page this bookmark points to
    val destinationY: Float,    // The Y coordinate on the page (in PDF User Units)
    val children: List<TableOfContentData> = emptyList() // For nested bookmarks/sections
)