package com.jholachhapdevs.pdfjuggler.feature.ai.data.remote

import com.jholachhapdevs.pdfjuggler.core.networking.httpClient
import com.jholachhapdevs.pdfjuggler.core.util.Env
import com.jholachhapdevs.pdfjuggler.feature.ai.data.model.GeminiContent
import com.jholachhapdevs.pdfjuggler.feature.ai.data.model.GeminiPart
import com.jholachhapdevs.pdfjuggler.feature.ai.data.model.GeminiRequest
import com.jholachhapdevs.pdfjuggler.feature.ai.data.model.GeminiResponse
import com.jholachhapdevs.pdfjuggler.feature.ai.domain.model.ChatMessage
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class GeminiRemoteDataSource(
    private val apiKey: String = Env.GEMINI_API_KEY
) {

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta"

    /**
     * Sends the full conversation to Gemini to preserve context.
     */
    suspend fun sendChat(
        model: String,
        messages: List<ChatMessage>
    ): GeminiResponse {
        // Optionally limit history to last N turns to avoid token overflow
        val limited = messages.takeLast(20)

        val contents = limited.map { msg ->
            GeminiContent(
                role = msg.role, // "user" or "model"
                parts = listOf(GeminiPart(text = msg.text))
            )
        }

        val requestBody = GeminiRequest(contents = contents)

        return httpClient.post("$baseUrl/models/$model:generateContent") {
            contentType(ContentType.Application.Json)
            parameter("key", apiKey)
            setBody(requestBody)
        }.body()
    }
}