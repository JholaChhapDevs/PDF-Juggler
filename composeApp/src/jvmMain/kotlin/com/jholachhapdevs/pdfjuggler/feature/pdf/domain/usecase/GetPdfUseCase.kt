package com.jholachhapdevs.pdfjuggler.feature.pdf.domain.usecase

import com.jholachhapdevs.pdfjuggler.feature.pdf.data.repository.PdfFileRepository
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.PdfFile

class GetPdfUseCase(
    private val repository: PdfFileRepository
) {
    operator fun invoke(): PdfFile? = repository.pickPdfFile()?.toDomain()
}