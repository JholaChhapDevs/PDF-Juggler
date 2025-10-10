package com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class BookmarkData(
    val pageIndex: Int,
    val title: String,
    val note: String = ""
)