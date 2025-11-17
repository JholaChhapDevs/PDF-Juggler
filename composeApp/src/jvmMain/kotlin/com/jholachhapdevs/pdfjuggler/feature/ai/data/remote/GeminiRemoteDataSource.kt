package com.jholachhapdevs.pdfjuggler.feature.ai.data.remote

import com.jholachhapdevs.pdfjuggler.core.networking.httpClient
import com.jholachhapdevs.pdfjuggler.feature.ai.data.model.GeminiContent
import com.jholachhapdevs.pdfjuggler.feature.ai.data.model.GeminiFile
import com.jholachhapdevs.pdfjuggler.feature.ai.data.model.GeminiFileData
import com.jholachhapdevs.pdfjuggler.feature.ai.data.model.GeminiPart
import com.jholachhapdevs.pdfjuggler.feature.ai.data.model.GeminiRequest
import com.jholachhapdevs.pdfjuggler.feature.ai.data.model.GeminiResponse
import com.jholachhapdevs.pdfjuggler.feature.ai.data.model.UploadFileResponse
import com.jholachhapdevs.pdfjuggler.feature.ai.domain.model.ChatMessage
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class GeminiRemoteDataSource(
    private val apiKey: String? = null
) {

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta"
    private val uploadBaseUrl = "https://generativelanguage.googleapis.com/upload/v1beta"
    private val json = Json { ignoreUnknownKeys = true }

    private fun jsonString(obj: JsonObject, key: String): String? {
        val el = obj[key] ?: return null
        return try { el.jsonPrimitive.content } catch (_: Throwable) { null }
    }

    suspend fun sendChat(
        model: String,
        messages: List<ChatMessage>
    ): GeminiResponse {
        val limited = messages.takeLast(20)

        val contents = limited.map { msg ->
            val parts = buildList {
                if (msg.text.isNotBlank()) add(GeminiPart(text = msg.text))
                msg.files.forEach { f ->
                    add(
                        GeminiPart(
                            fileData = com.jholachhapdevs.pdfjuggler.feature.ai.data.model.GeminiFileData(
                                fileUri = f.fileUri,
                                mimeType = f.mimeType
                            )
                        )
                    )
                }
            }
            GeminiContent(role = msg.role, parts = parts)
        }

        val requestBody = GeminiRequest(contents = contents)

        return httpClient.post("$baseUrl/models/$model:generateContent") {
            contentType(ContentType.Application.Json)
            parameter("key", apiKey)
            setBody(requestBody)
        }.body()
    }

    // Generic upload for any mime type (pdf, images, etc.)
    suspend fun uploadFile(
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): GeminiFile {
        val raw = httpClient.post("$uploadBaseUrl/files") {
            parameter("key", apiKey)
            headers {
                append("X-Goog-Upload-File-Name", fileName)
                append("X-Goog-Upload-Protocol", "raw")
            }
            contentType(ContentType.parse(mimeType))
            setBody(bytes)
        }.bodyAsText()

        val initialFile = try {
            json.decodeFromString(com.jholachhapdevs.pdfjuggler.feature.ai.data.model.UploadFileResponse.serializer(), raw).file
        } catch (_: Throwable) {
            // Lenient parse: handle {"file":{...}}, {"name":...}, or {"error":{...}}
            val root: JsonElement = try { json.parseToJsonElement(raw) } catch (t: Throwable) {
                throw Exception("Unexpected upload response (not JSON): $raw", t)
            }
            val obj = root as? JsonObject ?: throw Exception("Unexpected upload response (not an object): $raw")

            // Error shape from Google APIs
            obj["error"]?.let { errEl ->
                val errObj = errEl as? JsonObject
                val msg = errObj?.let { jsonString(it, "message") }
                throw Exception("Gemini upload error: ${msg ?: raw}")
            }

            // file wrapper
            obj["file"]?.let { fEl ->
                val fObj = (fEl as? JsonObject) ?: throw Exception("Unexpected file object: $raw")
                val name = jsonString(fObj, "name")
                if (name.isNullOrBlank()) throw Exception("Upload response missing file.name: $raw")
                return@let com.jholachhapdevs.pdfjuggler.feature.ai.data.model.GeminiFile(
                    name = name,
                    uri = jsonString(fObj, "uri"),
                    mimeType = jsonString(fObj, "mime_type"),
                    state = jsonString(fObj, "state"),
                    sizeBytes = jsonString(fObj, "size_bytes")
                )
            }

            // direct object with name
            val topName = jsonString(obj, "name")
            if (!topName.isNullOrBlank()) {
                com.jholachhapdevs.pdfjuggler.feature.ai.data.model.GeminiFile(
                    name = topName,
                    uri = jsonString(obj, "uri"),
                    mimeType = jsonString(obj, "mime_type"),
                    state = jsonString(obj, "state"),
                    sizeBytes = jsonString(obj, "size_bytes")
                )
            } else {
                throw Exception("Unexpected upload response (no name): $raw")
            }
        }

        return waitUntilActive(initialFile)
    }

    // Backward compatibility
    suspend fun uploadImage(
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): GeminiFile = uploadFile(fileName, mimeType, bytes)

    private suspend fun waitUntilActive(file: GeminiFile): GeminiFile {
        var current = file
        repeat(40) { // ~20s max
            val refreshed = getFile(current.name)
            if (refreshed.state == "ACTIVE") return refreshed
            delay(500)
            current = refreshed
        }
        return current
    }

    private suspend fun getFile(name: String): GeminiFile {
        return httpClient.get("$baseUrl/$name") {
            parameter("key", apiKey)
        }.body()
    }
}