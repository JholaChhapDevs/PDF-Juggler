package com.jholachhapdevs.pdfjuggler.feature.ai.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.jholachhapdevs.pdfjuggler.core.util.Env
import com.jholachhapdevs.pdfjuggler.feature.ai.data.remote.GeminiRemoteDataSource
import com.jholachhapdevs.pdfjuggler.feature.ai.domain.model.ChatMessage
import kotlinx.coroutines.launch
import java.util.UUID

class AiScreenModel : ScreenModel {

    // For demo purposes, using a hardcoded API key
    // In production, this should be stored securely
    private val apiKey = Env.GEMINI_API_KEY
    private val remoteDataSource = GeminiRemoteDataSource(apiKey)
    private val model = "gemini-2.5-flash"

    var messages by mutableStateOf<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                text = "Hello! I'm your AI assistant. I can help you with questions about your PDF documents. How can I assist you today?",
                isFromUser = false
            )
        )
    )
        private set

    var currentInput by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    fun updateInput(input: String) {
        currentInput = input
    }

    fun sendMessage() {
        if (currentInput.isBlank()) return

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = currentInput,
            isFromUser = true
        )

        // Add user message
        messages = messages + userMessage
        val prompt = currentInput
        currentInput = ""

        // Add loading message
        val loadingMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = "",
            isFromUser = false,
            isLoading = true
        )
        messages = messages + loadingMessage
        isLoading = true

        screenModelScope.launch {
            try {
                val response = remoteDataSource.sendPrompt(model, prompt)
                val aiText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Sorry, I couldn't generate a response at the moment."

                // Remove loading message and add AI response
                messages = messages.filter { !it.isLoading } + ChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = aiText,
                    isFromUser = false
                )
            } catch (e: Exception) {
                // Remove loading message and add error message
                messages = messages.filter { !it.isLoading } + ChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = "Sorry, I encountered an error: ${e.message}. Please make sure you have a valid API key configured.",
                    isFromUser = false
                )
            } finally {
                isLoading = false
            }
        }
    }

    fun clearChat() {
        messages = listOf(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                text = "Hello! I'm your AI assistant. I can help you with questions about your PDF documents. How can I assist you today?",
                isFromUser = false
            )
        )
    }
}