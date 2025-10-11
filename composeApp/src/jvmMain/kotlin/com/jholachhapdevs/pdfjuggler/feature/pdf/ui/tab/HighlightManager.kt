package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.HighlightMark
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.color.PDColor
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup

/**
 * Manages highlight operations for the PDF viewer
 */
class HighlightManager {
    
    // Highlights state: original page index -> list of marks (rects normalized to unrotated page)
    var highlightsByPage by mutableStateOf<Map<Int, List<HighlightMark>>>(emptyMap())
        private set
        
    var hasUnsavedHighlights by mutableStateOf(false)
        private set

    /**
     * Add a highlight for the currently displayed page.
     * rects are normalized rectangles in the displayed orientation. We convert them back to
     * unrotated page coordinates before storing.
     */
    fun addHighlightForDisplayedPage(
        originalPageIndex: Int,
        displayedRects: List<Rect>, 
        colorArgb: Long, 
        pageRotation: Float
    ) {
        val rotInt = when (val rot = ((pageRotation % 360f) + 360f) % 360f) {
            in 45f..135f -> 90
            in 135f..225f -> 180
            in 225f..315f -> 270
            else -> 0
        }
        
        val unrotatedRects = displayedRects.map { rect ->
            TabScreenUtils.inverseRotateRectNormalized(
                rect.left, 
                rect.top, 
                rect.width, 
                rect.height, 
                rotInt
            )
        }
        
        val list = highlightsByPage[originalPageIndex]?.toMutableList() ?: mutableListOf()
        list.add(HighlightMark(unrotatedRects, colorArgb))
        highlightsByPage = highlightsByPage.toMutableMap().apply { 
            put(originalPageIndex, list) 
        }
        hasUnsavedHighlights = true
    }

    /**
     * Return highlights for the currently displayed page in unrotated normalized coordinates.
     */
    fun getHighlightsForDisplayedPage(originalPageIndex: Int): List<HighlightMark> {
        return highlightsByPage[originalPageIndex] ?: emptyList()
    }

    /**
     * Apply highlights to a PDF page during save operations
     */
    fun applyHighlightsToPage(page: PDPage, marks: List<HighlightMark>) {
        val mediaBox: PDRectangle = page.mediaBox
        val pageW = mediaBox.width
        val pageH = mediaBox.height
        val annotations = page.annotations
        
        marks.forEach { mark ->
            // Merge rects per line to reduce fragmentation
            val mergedRects = TabScreenUtils.mergeRectsOnLinesNormalized(mark.rects)
            mergedRects.forEach { r ->
                val left = r.left * pageW
                val top = r.top * pageH
                val width = r.width * pageW
                val height = r.height * pageH
                val lly = pageH - (top + height) // convert from top-left origin to PDF bottom-left
                val rect = PDRectangle(left, lly, width, height)
                val ann = PDAnnotationTextMarkup(PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT)
                ann.rectangle = rect
                
                // Quad points: x1,y1 x2,y2 x3,y3 x4,y4 (top-left, top-right, bottom-left, bottom-right)
                val x1 = left; val y1 = lly + height
                val x2 = left + width; val y2 = lly + height
                val x3 = left; val y3 = lly
                val x4 = left + width; val y4 = lly
                ann.quadPoints = floatArrayOf(x1, y1, x2, y2, x3, y3, x4, y4)
                
                // Set color (RGB only)
                val rA = ((mark.colorArgb shr 16) and 0xFF) / 255f
                val gA = ((mark.colorArgb shr 8) and 0xFF) / 255f
                val bA = (mark.colorArgb and 0xFF) / 255f
                ann.color = PDColor(floatArrayOf(rA, gA, bA), PDDeviceRGB.INSTANCE)
                annotations.add(ann)
            }
        }
    }

    /**
     * Clear all highlights
     */
    fun clearAllHighlights() {
        highlightsByPage = emptyMap()
        hasUnsavedHighlights = true
    }

    /**
     * Remove highlights for a specific page
     */
    fun removeHighlightsForPage(pageIndex: Int) {
        highlightsByPage = highlightsByPage.toMutableMap().apply { 
            remove(pageIndex) 
        }
        hasUnsavedHighlights = true
    }

    /**
     * Mark highlights as saved
     */
    fun markHighlightsSaved() {
        hasUnsavedHighlights = false
    }

    /**
     * Set highlights map directly (used during initialization)
     */
    fun setHighlights(newHighlights: Map<Int, List<HighlightMark>>) {
        highlightsByPage = newHighlights
        hasUnsavedHighlights = false
    }
}