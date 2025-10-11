package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab

import androidx.compose.ui.geometry.Rect
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.BookmarkData
import kotlin.math.abs
import kotlin.math.max

/**
 * Utility functions for TabScreenModel operations
 */
object TabScreenUtils {
    
    /**
     * Escape special characters for JSON
     */
    fun escapeJson(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    /**
     * Unescape JSON string
     */
    fun unescapeJson(text: String): String {
        return text
            .replace("\\\\", "\\")
            .replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
    }

    /**
     * Serialize bookmarks to JSON string
     */
    fun serializeBookmarks(bookmarks: List<BookmarkData>): String {
        val bookmarksArray = bookmarks.joinToString(",") { bookmark ->
            """{"pageIndex":${bookmark.pageIndex},"title":"${escapeJson(bookmark.title)}","note":"${escapeJson(bookmark.note)}"}"""
        }
        return "[$bookmarksArray]"
    }

    /**
     * Deserialize bookmarks from JSON string
     */
    fun deserializeBookmarks(json: String): List<BookmarkData> {
        try {
            val bookmarks = mutableListOf<BookmarkData>()

            // Remove brackets and trim
            val cleaned = json.trim().removeSurrounding("[", "]").trim()
            if (cleaned.isEmpty()) return emptyList()

            // More robust parsing: Split by "},{"
            val bookmarkStrings = if (cleaned.contains("},{")) {
                cleaned.split("},{").map {
                    var s = it.trim()
                    if (!s.startsWith("{")) s = "{$s"
                    if (!s.endsWith("}")) s = "$s}"
                    s
                }
            } else {
                listOf(if (cleaned.startsWith("{")) cleaned else "{$cleaned}")
            }

            for (bookmarkStr in bookmarkStrings) {
                try {
                    // Extract values using regex for more reliable parsing
                    val pageIndexMatch = """"pageIndex"\s*:\s*(\d+)""".toRegex().find(bookmarkStr)
                    val titleMatch = """"title"\s*:\s*"([^"\\]*(?:\\.[^"\\]*)*)"""".toRegex().find(bookmarkStr)
                    val noteMatch = """"note"\s*:\s*"([^"\\]*(?:\\.[^"\\]*)*)"""".toRegex().find(bookmarkStr)

                    val pageIndex = pageIndexMatch?.groupValues?.get(1)?.toIntOrNull() ?: continue
                    val title = titleMatch?.groupValues?.get(1)?.let { unescapeJson(it) } ?: "Bookmark"
                    val note = noteMatch?.groupValues?.get(1)?.let { unescapeJson(it) } ?: ""

                    bookmarks.add(BookmarkData(pageIndex, title, note))
                } catch (e: Exception) {
                    println("Failed to parse bookmark: $bookmarkStr")
                    e.printStackTrace()
                }
            }

            return bookmarks
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    /**
     * Inverse rotate rectangle normalized coordinates
     */
    fun inverseRotateRectNormalized(l: Float, t: Float, w: Float, h: Float, angle: Int): Rect {
        // inverse of PdfMid.rotateRectNormalized
        return when ((angle % 360 + 360) % 360) {
            90 -> {
                // original rect that when rotated 90 produced current rect
                val nl = t
                val nt = 1f - (l + w)
                Rect(nl, nt, nl + h, nt + w)
            }
            180 -> {
                val nl = 1f - (l + w)
                val nt = 1f - (t + h)
                Rect(nl, nt, nl + w, nt + h)
            }
            270 -> {
                val nl = 1f - (t + h)
                val nt = l
                Rect(nl, nt, nl + h, nt + w)
            }
            else -> Rect(l, t, l + w, t + h)
        }
    }

    /**
     * Merge adjacent rectangles that lie on the same line (in normalized unrotated coordinates)
     */
    fun mergeRectsOnLinesNormalized(rects: List<Rect>): List<Rect> {
        if (rects.isEmpty()) return emptyList()
        val sorted = rects.sortedWith(compareBy({ it.top }, { it.left }))
        val merged = mutableListOf<Rect>()
        val heights = sorted.map { it.height }.sorted()
        val medianH = heights[heights.size / 2]
        val lineTol = medianH * 0.7f
        val gapTol = medianH * 1.0f
        var currentLineTop = sorted.first().top
        val currentLine = mutableListOf<Rect>()
        
        fun flush() {
            if (currentLine.isEmpty()) return
            currentLine.sortBy { it.left }
            var acc = currentLine.first()
            for (i in 1 until currentLine.size) {
                val r = currentLine[i]
                val sameRow = abs(r.top - acc.top) <= lineTol
                val close = r.left - acc.right <= gapTol
                if (sameRow && close) {
                    acc = Rect(
                        left = acc.left,
                        top = minOf(acc.top, r.top),
                        right = maxOf(acc.right, r.right),
                        bottom = maxOf(acc.bottom, r.bottom)
                    )
                } else {
                    merged.add(acc)
                    acc = r
                }
            }
            merged.add(acc)
            currentLine.clear()
        }
        
        for (r in sorted) {
            if (abs(r.top - currentLineTop) <= lineTol) currentLine.add(r) else {
                flush()
                currentLine.add(r)
                currentLineTop = r.top
            }
        }
        flush()
        return merged
    }
}