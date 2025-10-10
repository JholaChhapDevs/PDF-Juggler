package com.jholachhapdevs.pdfjuggler.feature.ai.domain.usecase

import com.jholachhapdevs.pdfjuggler.feature.ai.data.remote.GeminiRemoteDataSource

class UploadFileUseCase(
    private val remote: GeminiRemoteDataSource
) {
    suspend operator fun invoke(
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): String {
        val file = remote.uploadFile(fileName, mimeType, bytes)
        return file.uri ?: file.name
    }
}