package com.jholachhapdevs.pdfjuggler.feature.ai.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeminiFileData(
    @SerialName("file_uri")
    val fileUri: String,
    @SerialName("mime_type")
    val mimeType: String
)