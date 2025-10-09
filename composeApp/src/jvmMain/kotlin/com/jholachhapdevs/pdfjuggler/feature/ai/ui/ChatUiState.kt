package com.jholachhapdevs.pdfjuggler.feature.ai.ui

import com.jholachhapdevs.pdfjuggler.feature.ai.domain.model.ChatMessage

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val isSending: Boolean = false,
    val error: String? = null
)