package com.jholachhapdevs.pdfjuggler.feature.ai.domain.model

data class ChatMessage(
    val role: String,
    val text: String,
    val files: List<AttachedFile> = emptyList()
)
