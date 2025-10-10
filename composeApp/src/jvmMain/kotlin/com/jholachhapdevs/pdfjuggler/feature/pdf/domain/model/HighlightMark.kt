package com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model

import androidx.compose.ui.geometry.Rect

/**
 * A user-created highlight consisting of one or more normalized rectangles (0..1 coordinates
 * relative to the unrotated page top-left origin) and a color encoded as ARGB long.
 */
data class HighlightMark(
    val rects: List<Rect>,
    val colorArgb: Long
)

