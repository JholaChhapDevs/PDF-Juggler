package com.jholachhapdevs.pdfjuggler.feature.ai.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeminiFile(
    val name: String,
    val uri: String? = null,
    @SerialName("mime_type")
    val mimeType: String? = null,
    val state: String? = null,
    @SerialName("size_bytes")
    val sizeBytes: String? = null
)

@Serializable
data class UploadFileResponse(
    val file: GeminiFile
)