package com.jholachhapdevs.pdfjuggler.feature.ai.domain.usecase

import com.jholachhapdevs.pdfjuggler.core.util.Resources
import com.jholachhapdevs.pdfjuggler.feature.ai.data.remote.GeminiRemoteDataSource
import com.jholachhapdevs.pdfjuggler.feature.ai.domain.model.AttachedFile
import com.jholachhapdevs.pdfjuggler.feature.ai.domain.model.ChatMessage
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.TableOfContentData
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

@Serializable
data class GeminiTocItem(
    val title: String,
    val pageIndex: Int,
    val destinationY: Float = 0f,
    val children: List<GeminiTocItem> = emptyList()
)

@Serializable
data class GeminiTocResponse(
    val tableOfContents: List<GeminiTocItem>
)

/**
 * Use case for generating table of contents from PDF using Gemini API
 */
class GenerateTableOfContentsUseCase(
    private val remote: GeminiRemoteDataSource,
    private val uploadFileUseCase: UploadFileUseCase,
    private val modelName: String = Resources.DEFAULT_AI_MODEL
) {
    private val json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    suspend operator fun invoke(
        pdfBytes: ByteArray,
        fileName: String,
        totalPages: Int
    ): List<TableOfContentData> {
        try {
            // Upload PDF to Gemini
            val fileUri = uploadFileUseCase(
                fileName = fileName,
                mimeType = "application/pdf",
                bytes = pdfBytes
            )

            // Create the prompt for table of contents generation
            val prompt = createTableOfContentsPrompt(totalPages)

            // Create chat message with attached PDF file
            val message = ChatMessage(
                role = "user",
                text = prompt,
                files = listOf(
                    AttachedFile(
                        mimeType = "application/pdf",
                        fileUri = fileUri
                    )
                )
            )

            // Send to Gemini
            val response = remote.sendChat(modelName, listOf(message))

            // Parse the response
            val responseText = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text ?: throw Exception("No response from Gemini")

            return parseGeminiResponse(responseText)

        } catch (e: Exception) {
            println("Error generating table of contents with Gemini: ${e.message}")
            e.printStackTrace()
            return emptyList()
        }
    }

    private fun createTableOfContentsPrompt(totalPages: Int): String {
        return """
            Please analyze this PDF document and generate a comprehensive table of contents. 
            
            Instructions:
            1. Examine the document structure and identify main sections, chapters, and subsections
            2. For each item, determine the exact page number (0-based index) where it appears
            3. Create a hierarchical structure with nested items where appropriate
            4. The document has $totalPages pages total (pages 0 to ${totalPages - 1})
            5. Use destinationY as 0.0 for all items (we'll use the top of the page)
            
            Please respond with ONLY a valid JSON object in this exact format:
            {
              "tableOfContents": [
                {
                  "title": "Chapter 1: Introduction",
                  "pageIndex": 0,
                  "destinationY": 0.0,
                  "children": [
                    {
                      "title": "1.1 Overview",
                      "pageIndex": 1,
                      "destinationY": 0.0,
                      "children": []
                    }
                  ]
                },
                {
                  "title": "Chapter 2: Main Content",
                  "pageIndex": 3,
                  "destinationY": 0.0,
                  "children": []
                }
              ]
            }
            
            Important:
            - Ensure all pageIndex values are between 0 and ${totalPages - 1}
            - Include meaningful section titles based on the actual document content
            - Create a logical hierarchy with main sections and subsections
            - Respond with ONLY the JSON, no additional text or formatting
        """.trimIndent()
    }

    private fun parseGeminiResponse(responseText: String): List<TableOfContentData> {
        return try {
            // Clean up the response text to extract JSON
            val cleanedResponse = extractJsonFromResponse(responseText)
            
            // Parse JSON response
            val geminiResponse = json.decodeFromString<GeminiTocResponse>(cleanedResponse)
            
            // Convert to our domain model
            geminiResponse.tableOfContents.map { convertToTableOfContentData(it) }
            
        } catch (e: Exception) {
            println("Error parsing Gemini TOC response: ${e.message}")
            println("Response was: $responseText")
            
            // Return a fallback simple TOC based on page numbers
            createFallbackToc(responseText)
        }
    }

    private fun extractJsonFromResponse(responseText: String): String {
        // Try to find JSON content between ```json and ``` or just raw JSON
        val jsonStart = responseText.indexOf("{")
        val jsonEnd = responseText.lastIndexOf("}") + 1
        
        return if (jsonStart >= 0 && jsonEnd > jsonStart) {
            responseText.substring(jsonStart, jsonEnd)
        } else {
            responseText.trim()
        }
    }

    private fun convertToTableOfContentData(item: GeminiTocItem): TableOfContentData {
        return TableOfContentData(
            title = item.title,
            pageIndex = item.pageIndex,
            destinationY = item.destinationY,
            children = item.children.map { convertToTableOfContentData(it) }
        )
    }

    private fun createFallbackToc(responseText: String): List<TableOfContentData> {
        // If JSON parsing fails, try to extract meaningful information from the response
        val lines = responseText.split("\n").filter { it.trim().isNotEmpty() }
        val tocItems = mutableListOf<TableOfContentData>()
        
        var currentPageIndex = 0
        lines.forEach { line ->
            val cleanLine = line.trim().removePrefix("-").removePrefix("*").trim()
            
            // Try to extract page numbers from the text
            val pageNumberRegex = """(?:page\s*|p\.?\s*)(\d+)""".toRegex(RegexOption.IGNORE_CASE)
            val pageMatch = pageNumberRegex.find(cleanLine)
            
            if (cleanLine.isNotEmpty() && !cleanLine.startsWith("{") && !cleanLine.startsWith("}")) {
                val pageIndex = pageMatch?.groupValues?.get(1)?.toIntOrNull()?.minus(1) ?: currentPageIndex
                
                tocItems.add(
                    TableOfContentData(
                        title = cleanLine.replace(pageNumberRegex, "").trim(),
                        pageIndex = maxOf(0, pageIndex),
                        destinationY = 0f,
                        children = emptyList()
                    )
                )
                currentPageIndex = pageIndex + 1
            }
        }
        
        return if (tocItems.isEmpty()) {
            // Ultimate fallback: create a simple chapter-based TOC
            listOf(
                TableOfContentData(
                    title = "Document Content",
                    pageIndex = 0,
                    destinationY = 0f,
                    children = emptyList()
                )
            )
        } else {
            tocItems
        }
    }
}