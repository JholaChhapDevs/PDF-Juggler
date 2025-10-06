package com.jholachhapdevs.pdfjuggler.feature.pdf.data.model

import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.PdfFile

data class PdfFileData(
    val uri: String,
    val name: String
) {
    fun toDomain() = PdfFile(
        name = name,
        path = uri
    )
}