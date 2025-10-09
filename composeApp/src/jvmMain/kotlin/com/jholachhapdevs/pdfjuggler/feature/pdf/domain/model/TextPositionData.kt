package com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model

data class TextPositionData(
    val text: String,
    val x: Float,     // Start X coordinate (PDF units)
    val y: Float,     // Base Y coordinate (PDF units, bottom-up origin)
    val width: Float,
    val height: Float
)