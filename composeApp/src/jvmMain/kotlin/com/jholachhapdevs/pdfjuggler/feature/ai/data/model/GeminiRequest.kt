package com.jholachhapdevs.pdfjuggler.feature.ai.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>
)

@Serializable
data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

@Serializable
data class GeminiPart(
    val text: String? = null,
    @SerialName("inline_data")
    val inlineData: GeminiInlineData? = null,
    @SerialName("file_data")
    val fileData: GeminiFileData? = null
)