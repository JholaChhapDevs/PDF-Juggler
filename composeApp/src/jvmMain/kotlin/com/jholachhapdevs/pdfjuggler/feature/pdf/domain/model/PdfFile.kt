package com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PdfFile(
    val path: String,
    val name: String
)
