package com.jholachhapdevs.pdfjuggler.feature.ai.domain.usecase

import com.jholachhapdevs.pdfjuggler.core.util.Resources
import com.jholachhapdevs.pdfjuggler.feature.ai.data.model.GeminiResponse
import com.jholachhapdevs.pdfjuggler.feature.ai.data.remote.GeminiRemoteDataSource
import com.jholachhapdevs.pdfjuggler.feature.ai.domain.model.ChatMessage

/**
 * Sends the full chat history (plus a persona preface) to Gemini and returns the next model message.
 */
class SendPromptUseCase(
    private val remote: GeminiRemoteDataSource,
    private val modelName: String = Resources.DEFAULT_AI_MODEL,
    private val assistantName: String = "Ringmaster"
) {
    suspend operator fun invoke(messages: List<ChatMessage>): ChatMessage {
        // Persona preface (kept concise and circus-themed)
        val persona = """
            You are $assistantName, a succinct, friendly PDF assistant with a circus theme.
            Always introduce yourself as "$assistantName" when greeting or when asked your name.
            Keep responses concise and focused on the user's PDF and request context.
        """.trimIndent()

        // Ensure persona is always included within the last 20 messages
        val preface = ChatMessage(role = "user", text = persona)
        val limited = messages.takeLast(19)
        val response: GeminiResponse = remote.sendChat(modelName, listOf(preface) + limited)

        val text = response.candidates
            ?.firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.text ?: "No response"
        return ChatMessage(role = "model", text = text)
    }
}