package com.jholachhapdevs.pdfjuggler.feature.ai.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GeminiResponse(
    val candidates: List<Candidate>? = null
)

@Serializable
data class Candidate(
    val content: GeminiContentResponse? = null
)

@Serializable
data class GeminiContentResponse(
    val parts: List<GeminiPartResponse>? = null
)

@Serializable
data class GeminiPartResponse(
    val text: String? = null
)
