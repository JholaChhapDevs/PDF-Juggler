package com.jholachhapdevs.pdfjuggler.feature.ai.domain.model

data class ChatMessage(
    val id: String,
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false
)