package com.jholachhapdevs.pdfjuggler.feature.ai.data.remote

import com.jholachhapdevs.pdfjuggler.core.networking.httpClient
import com.jholachhapdevs.pdfjuggler.feature.ai.data.model.GeminiContent
import com.jholachhapdevs.pdfjuggler.feature.ai.data.model.GeminiPart
import com.jholachhapdevs.pdfjuggler.feature.ai.data.model.GeminiRequest
import com.jholachhapdevs.pdfjuggler.feature.ai.data.model.GeminiResponse
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class GeminiRemoteDataSource(
    private val apiKey: String
) {

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta"

    /**
     * Sends a text prompt to Gemini and returns the model's reply.
     */
    suspend fun sendPrompt(
        model: String,
        prompt: String
    ): GeminiResponse {
        val requestBody = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = prompt)
                    )
                )
            )
        )

        return httpClient.post("$baseUrl/models/$model:generateContent") {
            contentType(ContentType.Application.Json)
            parameter("key", apiKey)
            setBody(requestBody)
        }.body()
    }
}
