package com.jholachhapdevs.pdfjuggler.feature.pdf.ui

import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.TextPositionData
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition
import java.io.IOException

/**
 * A custom PDFTextStripper to extract text and its precise position data.
 */
class PositionAwareTextStripper : PDFTextStripper() {

    // A map to store text data lists, keyed by page index (0-based)
    val allPageTextData: MutableMap<Int, MutableList<TextPositionData>> = mutableMapOf()

    init {
        // Essential for proper coordinate-based extraction
        sortByPosition = true
    }

    @Throws(IOException::class)
    override fun processTextPosition(text: TextPosition) {
        val pageIndex = currentPageNo - 1 // Convert 1-based to 0-based

        val data = TextPositionData(
            text = text.unicode,
            x = text.xDirAdj,
            y = text.yDirAdj,
            width = text.widthDirAdj,
            height = text.heightDir
        )

        allPageTextData
            .getOrPut(pageIndex) { mutableListOf() }
            .add(data)

        // Do NOT call super.processTextPosition(text) if you only need the data.
    }
}