package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab

import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.TextPositionData

/**
 * Data class representing a search match in the PDF
 */
data class SearchMatch(
    val pageIndex: Int, // original page index
    val positions: List<TextPositionData>
)

/**
 * Enum for AI request modes
 */
enum class AiRequestMode { 
    Dictionary, 
    Translate 
}

/**
 * Data class for AI requests from text selection
 */
data class AiRequest(
    val text: String, 
    val mode: AiRequestMode, 
    val ts: Long = System.currentTimeMillis()
)