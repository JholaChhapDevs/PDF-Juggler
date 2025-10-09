// Kotlin
package com.jholachhapdevs.pdfjuggler.feature.ai.domain.usecase

import com.jholachhapdevs.pdfjuggler.core.util.Resources
import com.jholachhapdevs.pdfjuggler.feature.ai.data.model.GeminiResponse
import com.jholachhapdevs.pdfjuggler.feature.ai.data.remote.GeminiRemoteDataSource
import com.jholachhapdevs.pdfjuggler.feature.ai.domain.model.ChatMessage

/**
 * Sends the full chat history (including any inline image tokens) to Gemini
 * and returns the next model message.
 */
class SendPromptUseCase(
    private val remote: GeminiRemoteDataSource,
    private val modelName: String = Resources.DEFAULT_AI_MODEL
) {
    suspend operator fun invoke(messages: List<ChatMessage>): ChatMessage {
        val response: GeminiResponse = remote.sendChat(modelName, messages)
        val text = response.candidates
            ?.firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.text ?: "No response"
        return ChatMessage(role = "model", text = text)
    }
}