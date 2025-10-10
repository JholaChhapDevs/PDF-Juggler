package com.jholachhapdevs.pdfjuggler.feature.ai.domain.model

data class ChatMessage(
    val role: String, // "user" or "model"
    val text: String
)
