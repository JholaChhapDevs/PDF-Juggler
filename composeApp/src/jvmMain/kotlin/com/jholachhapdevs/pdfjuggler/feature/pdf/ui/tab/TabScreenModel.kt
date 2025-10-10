package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.IntSize
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.jholachhapdevs.pdfjuggler.core.pdf.HighQualityPdfRenderer
import com.jholachhapdevs.pdfjuggler.core.pdf.PdfPageReorderUtil
import com.jholachhapdevs.pdfjuggler.core.pdf.SaveResult
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.TableOfContentData
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.PdfFile
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.TextPositionData
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.BookmarkData
import com.jholachhapdevs.pdfjuggler.feature.pdf.ui.PositionAwareTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDDocumentInformation
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.HighlightMark
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB
import org.apache.pdfbox.pdmodel.graphics.color.PDColor
import kotlin.math.abs
import kotlin.math.max

class TabScreenModel(
    val pdfFile: PdfFile,
    private val window: java.awt.Window? = null
) : ScreenModel {
    
    // High-quality PDF renderer
    private val pdfRenderer = HighQualityPdfRenderer()
    
    // PDF page reordering utility
    private val pdfReorderUtil = PdfPageReorderUtil()

    // --- Search state ---
    data class SearchMatch(
        val pageIndex: Int, // original page index
        val positions: List<TextPositionData>
    )
    var searchQuery by mutableStateOf("")
        private set
    var searchMatches by mutableStateOf<List<SearchMatch>>(emptyList())
        private set
    var currentSearchIndex by mutableStateOf(-1)
        private set

    var thumbnails by mutableStateOf<List<ImageBitmap>>(emptyList())
        private set

    var currentPageImage by mutableStateOf<ImageBitmap?>(null)
        private set

    var selectedPageIndex by mutableStateOf(0)
        private set

    var totalPages by mutableStateOf(0)
        private set

    var isLoading by mutableStateOf(true)
        private set

    var allTextDataWithCoordinates by mutableStateOf<Map<Int, List<TextPositionData>>>(emptyMap())
        private set

    var allTextData by mutableStateOf<Map<Int, String>>(emptyMap())
        private set
    var tableOfContent by mutableStateOf<List<TableOfContentData>>(emptyList())
        private set
        
    // Current zoom and viewport state for adaptive rendering
    private var currentViewport by mutableStateOf(IntSize.Zero)
    
    // Auto-scroll trigger for search matches
    private var _scrollToMatchTrigger by mutableStateOf(0)
    val scrollToMatchTrigger: Int get() = _scrollToMatchTrigger
    
    var currentRotation by mutableStateOf(0f)
        private set
    
    // Fullscreen state
    var isFullscreen by mutableStateOf(false)
        private set

    // Page ordering - maps display order to original page indices
    var pageOrder by mutableStateOf<List<Int>>(emptyList())
        private set
    
    // Track if pages have been reordered
    var hasPageChanges by mutableStateOf(false)
        private set
    
    // Save operation state
    var isSaving by mutableStateOf(false)
        private set
        
    var saveResult by mutableStateOf<SaveResult?>(null)
        private set

    // Bookmarks state
    var bookmarks by mutableStateOf<List<BookmarkData>>(emptyList())
        private set

    var hasUnsavedBookmarks by mutableStateOf(false)
        private set

    // Highlights state: original page index -> list of marks (rects normalized to unrotated page)
    var highlightsByPage by mutableStateOf<Map<Int, List<HighlightMark>>>(emptyMap())
        private set
    var hasUnsavedHighlights by mutableStateOf(false)
        private set

    // Map of page index -> page size in PDF points (mediaBox width/height)
    var pageSizesPoints by mutableStateOf<Map<Int, Size>>(emptyMap())
        private set

    // Pending AI chat request from selection (dictionary/translate)
    enum class AiRequestMode { Dictionary, Translate }
    data class AiRequest(val text: String, val mode: AiRequestMode, val ts: Long = System.currentTimeMillis())
    var pendingAiRequest by mutableStateOf<AiRequest?>(null)
        private set

    fun requestAiDictionary(text: String) {
        if (text.isNotBlank()) pendingAiRequest = AiRequest(text, AiRequestMode.Dictionary)
    }
    fun requestAiTranslate(text: String) {
        if (text.isNotBlank()) pendingAiRequest = AiRequest(text, AiRequestMode.Translate)
    }
    fun clearPendingAiRequest() { pendingAiRequest = null }
    // Add this property near the top with other state variables
    var currentZoom by mutableStateOf(1f)
        private set

    // Add these three methods anywhere in the class
    fun zoomIn() {
        currentZoom = (currentZoom * 1.25f).coerceAtMost(5f)
        screenModelScope.launch {
            try {
                currentPageImage = renderPageHighQuality(selectedPageIndex)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun zoomOut() {
        currentZoom = (currentZoom / 1.25f).coerceAtLeast(0.25f)
        screenModelScope.launch {
            try {
                currentPageImage = renderPageHighQuality(selectedPageIndex)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resetZoom() {
        currentZoom = 1f
        screenModelScope.launch {
            try {
                currentPageImage = renderPageHighQuality(selectedPageIndex)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    init {
        loadPdf()
    }

    private fun loadPdf() {
        screenModelScope.launch {
            isLoading = true
            try {
                totalPages = getTotalPages(pdfFile.path)
                // Initialize page order with original sequence
                pageOrder = (0 until totalPages).toList()
                thumbnails = renderThumbnails(pdfFile.path, totalPages)
                currentPageImage = renderPageHighQuality(0)
                allTextDataWithCoordinates = extractAllTextData(pdfFile.path)
                allTextData = getTextOnlyData()
                tableOfContent = getTableOfContents(pdfFile.path)
                // Load bookmarks from PDF metadata
                bookmarks = loadBookmarksFromMetadata(pdfFile.path)
                // Extract page sizes in points for proper coordinate mapping
                pageSizesPoints = extractPageSizesPoints(pdfFile.path)

                // Clear previous search (if any) and recompute based on current query
                if (searchQuery.isNotBlank()) {
                    recomputeSearchMatches()
                } else {
                    clearSearch()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun selectPage(pageIndex: Int) {
        if (pageIndex < 0 || pageIndex >= pageOrder.size) return
        selectedPageIndex = pageIndex
        screenModelScope.launch {
            try {
                // Get the original page index for rendering
                val originalPageIndex = getOriginalPageIndex(pageIndex)
                currentPageImage = renderPageHighQuality(originalPageIndex)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ============ Bookmark Management Functions ============

    /**
     * Add a new bookmark
     */
    fun addBookmark(bookmark: BookmarkData) {
        // Check if bookmark already exists for this page
        val existingIndex = bookmarks.indexOfFirst { it.pageIndex == bookmark.pageIndex }

        if (existingIndex != -1) {
            // Update existing bookmark
            val updatedBookmarks = bookmarks.toMutableList()
            updatedBookmarks[existingIndex] = bookmark
            bookmarks = updatedBookmarks
        } else {
            // Add new bookmark
            bookmarks = bookmarks + bookmark
        }

        hasUnsavedBookmarks = true
    }

    /**
     * Remove a bookmark by index in the bookmarks list
     */
    fun removeBookmark(bookmarkIndex: Int) {
        if (bookmarkIndex >= 0 && bookmarkIndex < bookmarks.size) {
            bookmarks = bookmarks.toMutableList().apply {
                removeAt(bookmarkIndex)
            }
            hasUnsavedBookmarks = true
        }
    }

    /**
     * Remove bookmark for a specific page
     */
    fun removeBookmarkForPage(pageIndex: Int) {
        bookmarks = bookmarks.filter { it.pageIndex != pageIndex }
        hasUnsavedBookmarks = true
    }

    /**
     * Check if a page has a bookmark
     */
    fun isPageBookmarked(pageIndex: Int): Boolean {
        return bookmarks.any { it.pageIndex == pageIndex }
    }

    /**
     * Get bookmark for a specific page
     */
    fun getBookmarkForPage(pageIndex: Int): BookmarkData? {
        return bookmarks.firstOrNull { it.pageIndex == pageIndex }
    }

    /**
     * Save bookmarks to PDF metadata
     */
    fun saveBookmarksToMetadata() {
        screenModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val file = File(pdfFile.path)

                    // Create a temporary file to save to first
                    val tempFile = File("${file.absolutePath}.tmp")

                    PDDocument.load(file).use { document ->
                        // Get or create document information
                        val info = document.documentInformation ?: PDDocumentInformation()

                        // Serialize ALL bookmarks to a custom metadata field
                        val bookmarksJson = serializeBookmarks(bookmarks)

                        // Debug: Print what we're saving
                        println("Saving ${bookmarks.size} bookmarks to metadata: $bookmarksJson")

                        info.setCustomMetadataValue("Bookmarks", bookmarksJson)

                        // Update document information
                        document.documentInformation = info

                        // Save to temporary file first
                        document.save(tempFile)
                    }

                    // Replace original file with temp file
                    if (tempFile.exists()) {
                        file.delete()
                        tempFile.renameTo(file)
                    }
                }

                hasUnsavedBookmarks = false
                saveResult = SaveResult.Success(pdfFile.path, bookmarks.size, "${bookmarks.size} bookmark(s) saved successfully")

            } catch (e: Exception) {
                e.printStackTrace()
                saveResult = SaveResult.Error("Failed to save bookmarks: ${e.message}")
            }
        }
    }

    /**
     * Load bookmarks from PDF metadata
     */
    private suspend fun loadBookmarksFromMetadata(filePath: String): List<BookmarkData> {
        return withContext(Dispatchers.IO) {
            try {
                PDDocument.load(File(filePath)).use { document ->
                    val info = document.documentInformation
                    val bookmarksJson = info?.getCustomMetadataValue("Bookmarks")

                    println("Loading bookmarks from metadata: $bookmarksJson")

                    if (bookmarksJson != null) {
                        val loadedBookmarks = deserializeBookmarks(bookmarksJson)
                        println("Loaded ${loadedBookmarks.size} bookmarks")
                        loadedBookmarks
                    } else {
                        println("No bookmarks found in metadata")
                        emptyList()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    /**
     * Serialize bookmarks to JSON string
     */
    private fun serializeBookmarks(bookmarks: List<BookmarkData>): String {
        // Simple JSON serialization (you can use a proper JSON library like kotlinx.serialization)
        val bookmarksArray = bookmarks.joinToString(",") { bookmark ->
            """{"pageIndex":${bookmark.pageIndex},"title":"${escapeJson(bookmark.title)}","note":"${escapeJson(bookmark.note)}"}"""
        }
        return "[$bookmarksArray]"
    }

    /**
     * Deserialize bookmarks from JSON string
     */
    private fun deserializeBookmarks(json: String): List<BookmarkData> {
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
                    println("Parsed bookmark: pageIndex=$pageIndex, title=$title, note=$note")
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
     * Unescape JSON string
     */
    private fun unescapeJson(text: String): String {
        return text
            .replace("\\\\", "\\")
            .replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
    }

    /**
     * Escape special characters for JSON
     */
    private fun escapeJson(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    /**
     * Clear all bookmarks
     */
    fun clearAllBookmarks() {
        bookmarks = emptyList()
        hasUnsavedBookmarks = true
    }

    /**
     * Export bookmarks to a text file
     */
    fun exportBookmarksToFile(outputPath: String) {
        screenModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val content = StringBuilder()
                    content.appendLine("PDF Bookmarks - ${pdfFile.name}")
                    content.appendLine("=" .repeat(50))
                    content.appendLine()

                    bookmarks.sortedBy { it.pageIndex }.forEach { bookmark ->
                        content.appendLine("Page ${bookmark.pageIndex + 1}: ${bookmark.title}")
                        if (bookmark.note.isNotEmpty()) {
                            content.appendLine("  Note: ${bookmark.note}")
                        }
                        content.appendLine()
                    }

                    File(outputPath).writeText(content.toString())
                }

                saveResult = SaveResult.Success(outputPath, bookmarks.size, "Bookmarks exported successfully")

            } catch (e: Exception) {
                e.printStackTrace()
                saveResult = SaveResult.Error("Failed to export bookmarks: ${e.message}")
            }
        }
    }

    // ============ End Bookmark Management Functions ============

    // --- Search API ---
    fun updateSearchQuery(query: String) {
        searchQuery = query
        if (query.isBlank()) {
            clearSearch()
        } else {
            recomputeSearchMatches()
        }
    }

    fun clearSearch() {
        searchMatches = emptyList()
        currentSearchIndex = -1
    }

    private fun recomputeSearchMatches() {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) {
            clearSearch(); return
        }
        val matches = mutableListOf<SearchMatch>()
        for (originalPage in 0 until totalPages) {
            val items = allTextDataWithCoordinates[originalPage] ?: continue
            var i = 0
            while (i < items.size) {
                var k = 0
                var j = i
                val collected = mutableListOf<TextPositionData>()
                while (j < items.size && k < q.length) {
                    val t = items[j].text.lowercase()
                    val remaining = q.substring(k)
                    if (remaining.startsWith(t)) {
                        collected.add(items[j])
                        k += t.length
                        j++
                    } else {
                        break
                    }
                }
                if (k == q.length && collected.isNotEmpty()) {
                    matches.add(SearchMatch(originalPage, collected.toList()))
                    i = j
                } else {
                    i++
                }
            }
        }
        searchMatches = matches
        currentSearchIndex = if (matches.isNotEmpty()) 0 else -1
        // If first match exists and is on another page, navigate there
        val first = matches.firstOrNull()
        if (first != null) {
            val displayIdx = pageOrder.indexOf(first.pageIndex)
            if (displayIdx >= 0) selectPage(displayIdx)
        }
    }

    fun goToNextMatch() {
        if (searchMatches.isEmpty()) return
        currentSearchIndex = (currentSearchIndex + 1) % searchMatches.size
        val match = searchMatches[currentSearchIndex]
        val displayIdx = pageOrder.indexOf(match.pageIndex)
        if (displayIdx >= 0) {
            selectPage(displayIdx)
            scrollToCurrentMatch()
        }
    }
    
    fun goToPreviousMatch() {
        if (searchMatches.isEmpty()) return
        currentSearchIndex = if (currentSearchIndex <= 0) {
            searchMatches.size - 1
        } else {
            currentSearchIndex - 1
        }
        val match = searchMatches[currentSearchIndex]
        val displayIdx = pageOrder.indexOf(match.pageIndex)
        if (displayIdx >= 0) {
            selectPage(displayIdx)
            scrollToCurrentMatch()
        }
    }
    
    private fun scrollToCurrentMatch() {
        // Trigger scroll to the current search match by incrementing the trigger
        _scrollToMatchTrigger++
    }

    fun currentMatchForDisplayedPage(): List<TextPositionData> {
        val idx = currentSearchIndex
        if (idx < 0 || idx >= searchMatches.size) return emptyList()
        val match = searchMatches[idx]
        val displayedOriginal = getOriginalPageIndex(selectedPageIndex)
        return if (match.pageIndex == displayedOriginal) match.positions else emptyList()
    }
    
    /**
     * Called when zoom level changes to re-render at appropriate quality
     */
    fun onZoomChanged(zoomFactor: Float) {
        if (zoomFactor != currentZoom) {
            currentZoom = zoomFactor
            // Re-render current page at new zoom level for better quality
            screenModelScope.launch {
                try {
                    currentPageImage = renderPageHighQuality(selectedPageIndex)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    /**
     * Called when viewport size changes
     */
    fun onViewportChanged(viewport: IntSize) {
        if (viewport != currentViewport && viewport.width > 0 && viewport.height > 0) {
            currentViewport = viewport
            // Re-render for new viewport if significant change
            screenModelScope.launch {
                try {
                    currentPageImage = renderPageHighQuality(selectedPageIndex)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    /**
     * Toggle fullscreen mode
     */
    fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        // Toggle actual window fullscreen state
        window?.let { win ->
            if (win is java.awt.Frame) {
                win.extendedState = if (isFullscreen) {
                    java.awt.Frame.MAXIMIZED_BOTH
                } else {
                    java.awt.Frame.NORMAL
                }
            }
            // Use GraphicsDevice for true fullscreen
            val graphicsDevice = java.awt.GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .defaultScreenDevice
            if (isFullscreen) {
                if (graphicsDevice.isFullScreenSupported) {
                    graphicsDevice.fullScreenWindow = win
                }
            } else {
                if (graphicsDevice.fullScreenWindow == win) {
                    graphicsDevice.fullScreenWindow = null
                }
            }
        }
    }

    /**
     * Rotate the current page 90 degrees clockwise
     */
    fun rotateClockwise() {
        currentRotation = (currentRotation + 90f) % 360f
        // Re-render current page with new rotation
        screenModelScope.launch {
            try {
                currentPageImage = renderPageHighQuality(selectedPageIndex)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Rotate the current page 90 degrees counter-clockwise
     */
    fun rotateCounterClockwise() {
        currentRotation = (currentRotation - 90f + 360f) % 360f
        // Re-render current page with new rotation
        screenModelScope.launch {
            try {
                currentPageImage = renderPageHighQuality(selectedPageIndex)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Reset rotation to 0 degrees
     */
    fun resetRotation() {
        currentRotation = 0f
        // Re-render current page with no rotation
        screenModelScope.launch {
            try {
                currentPageImage = renderPageHighQuality(selectedPageIndex)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Move a page up in the order (decrease display position)
     */
    fun movePageUp(displayIndex: Int) {
        if (displayIndex > 0 && displayIndex < pageOrder.size) {
            val newOrder = pageOrder.toMutableList()
            // Swap with previous item
            val temp = newOrder[displayIndex]
            newOrder[displayIndex] = newOrder[displayIndex - 1]
            newOrder[displayIndex - 1] = temp
            
            pageOrder = newOrder
            hasPageChanges = true
            
            // Update thumbnails to reflect new order
            updateThumbnailOrder()
            
            // If the selected page was moved, update the selection
            if (selectedPageIndex == displayIndex) {
                selectedPageIndex = displayIndex - 1
            } else if (selectedPageIndex == displayIndex - 1) {
                selectedPageIndex = displayIndex
            }
        }
    }
    
    /**
     * Move a page down in the order (increase display position)
     */
    fun movePageDown(displayIndex: Int) {
        if (displayIndex >= 0 && displayIndex < pageOrder.size - 1) {
            val newOrder = pageOrder.toMutableList()
            // Swap with next item
            val temp = newOrder[displayIndex]
            newOrder[displayIndex] = newOrder[displayIndex + 1]
            newOrder[displayIndex + 1] = temp
            
            pageOrder = newOrder
            hasPageChanges = true
            
            // Update thumbnails to reflect new order
            updateThumbnailOrder()
            
            // If the selected page was moved, update the selection
            if (selectedPageIndex == displayIndex) {
                selectedPageIndex = displayIndex + 1
            } else if (selectedPageIndex == displayIndex + 1) {
                selectedPageIndex = displayIndex
            }
        }
    }
    
    /**
     * Move a page to a specific position in the order
     */
    fun movePageToPosition(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex || fromIndex < 0 || toIndex < 0 || 
            fromIndex >= pageOrder.size || toIndex >= pageOrder.size) {
            return
        }
        
        val newOrder = pageOrder.toMutableList()
        val pageToMove = newOrder.removeAt(fromIndex)
        newOrder.add(toIndex, pageToMove)
        
        pageOrder = newOrder
        hasPageChanges = true
        
        // Update thumbnails to reflect new order
        updateThumbnailOrder()
        
        // Update selected page index if necessary
        when {
            selectedPageIndex == fromIndex -> selectedPageIndex = toIndex
            selectedPageIndex in (minOf(fromIndex, toIndex) + 1)..maxOf(fromIndex, toIndex) -> {
                if (fromIndex < toIndex) selectedPageIndex-- else selectedPageIndex++
            }
        }
    }
    
    /**
     * Update thumbnails order to match page order
     */
    private fun updateThumbnailOrder() {
        screenModelScope.launch {
            try {
                // Re-render thumbnails in new order
                val reorderedThumbnails = mutableListOf<ImageBitmap>()
                for (originalPageIndex in pageOrder) {
                    val thumbnail = pdfRenderer.renderPage(
                        pdfFile.path,
                        originalPageIndex,
                        HighQualityPdfRenderer.RenderOptions(
                            dpi = 96f,
                            highQuality = true
                        )
                    )
                    thumbnail?.let { reorderedThumbnails.add(it) }
                }
                thumbnails = reorderedThumbnails
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Reset page order to original sequence
     */
    fun resetPageOrder() {
        pageOrder = (0 until totalPages).toList()
        hasPageChanges = false
        updateThumbnailOrder()
        // Re-select current page based on original index
        selectPage(selectedPageIndex)
    }
    
    /**
     * Get the original page index for a given display index
     */
    fun getOriginalPageIndex(displayIndex: Int): Int {
        return if (displayIndex >= 0 && displayIndex < pageOrder.size) {
            pageOrder[displayIndex]
        } else {
            displayIndex
        }
    }

    /**
     * Helper to fetch the page size (in PDF points) for the given display index.
     */
    fun getPageSizePointsForDisplayIndex(displayIndex: Int): Size? {
        val original = getOriginalPageIndex(displayIndex)
        return pageSizesPoints[original]
    }
    
    /**
     * Save the PDF with current page ordering to a new file
     */
    fun savePdfAs(outputPath: String) {
        if (isSaving) return
        
        screenModelScope.launch {
            isSaving = true
            saveResult = null
            
            try {
                val result = pdfReorderUtil.saveReorderedPdf(
                    inputFilePath = pdfFile.path,
                    outputFilePath = outputPath,
                    pageOrder = pageOrder
                )
                
                saveResult = result
                
                // If save was successful, reset the changes state
                if (result is SaveResult.Success) {
                    hasPageChanges = false
                }
                
            } catch (e: Exception) {
                saveResult = SaveResult.Error("Save failed: ${e.message}")
                e.printStackTrace()
            } finally {
                isSaving = false
            }
        }
    }
    
    /**
     * Save the PDF with current page ordering, overwriting the original file
     */
    fun savePdf() {
        // Create a temporary file with reordered pages
        val tempOutputPath = "${pdfFile.path}.tmp"
        
        screenModelScope.launch {
            isSaving = true
            saveResult = null
            
            try {
                // Save to temporary file first
                val result = pdfReorderUtil.saveReorderedPdf(
                    inputFilePath = pdfFile.path,
                    outputFilePath = tempOutputPath,
                    pageOrder = pageOrder
                )
                
                if (result is SaveResult.Success) {
                    // Replace original file with the reordered version
                    val originalFile = java.io.File(pdfFile.path)
                    val tempFile = java.io.File(tempOutputPath)
                    
                    if (tempFile.exists()) {
                        // Backup original file
                        val backupPath = "${pdfFile.path}.backup"
                        val backupFile = java.io.File(backupPath)
                        if (backupFile.exists()) backupFile.delete()
                        originalFile.renameTo(backupFile)
                        
                        // Move temp file to original location
                        if (tempFile.renameTo(originalFile)) {
                            // Delete backup on successful replacement
                            backupFile.delete()
                            saveResult = SaveResult.Success(pdfFile.path, pageOrder.size)
                            hasPageChanges = false
                        } else {
                            // Restore backup if replacement failed
                            backupFile.renameTo(originalFile)
                            saveResult = SaveResult.Error("Failed to replace original file")
                        }
                    } else {
                        saveResult = SaveResult.Error("Temporary file was not created")
                    }
                } else {
                    saveResult = result
                }
                
            } catch (e: Exception) {
                saveResult = SaveResult.Error("Save failed: ${e.message}")
                e.printStackTrace()
            } finally {
                // Clean up temp file
                java.io.File(tempOutputPath).delete()
                isSaving = false
            }
        }
    }
    
    /**
     * Clear the last save result
     */
    fun clearSaveResult() {
        saveResult = null
    }
    
    /**
     * Validate an output path for saving
     */
    fun validateSavePath(outputPath: String) = pdfReorderUtil.validateOutputPath(outputPath)

    private suspend fun getTotalPages(filePath: String): Int = withContext(Dispatchers.IO) {
        PDDocument.load(File(filePath)).use { doc ->
            doc.numberOfPages
        }
    }

    /**
     * Render page with high quality using adaptive DPI
     */
    private suspend fun renderPageHighQuality(pageIndex: Int): ImageBitmap? {
        return if (currentViewport.width > 0 && currentViewport.height > 0) {
            // Use adaptive rendering based on viewport and zoom
            pdfRenderer.renderPageAdaptiveWithRotation(
                pdfFile.path,
                pageIndex,
                currentViewport.width,
                currentViewport.height,
                currentZoom,
                currentRotation
            )
        } else {
            // Fallback to high-quality rendering with adaptive DPI
            val adaptiveDPI = HighQualityPdfRenderer.calculateAdaptiveDPI(currentZoom)
            pdfRenderer.renderPage(
                pdfFile.path,
                pageIndex,
                HighQualityPdfRenderer.RenderOptions(dpi = adaptiveDPI, rotation = currentRotation)
            )
        }
    }
    
    @Deprecated("Use renderPageHighQuality instead")
    private suspend fun renderPage(filePath: String, pageIndex: Int, dpi: Float = 150f): ImageBitmap? =
        withContext(Dispatchers.IO) {
            try {
                PDDocument.load(File(filePath)).use { document ->
                    val renderer = PDFRenderer(document)
                    val bufferedImage: BufferedImage = renderer.renderImageWithDPI(pageIndex, dpi, ImageType.RGB)
                    bufferedImage.toImageBitmap()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    private suspend fun renderThumbnails(filePath: String, maxPages: Int): List<ImageBitmap> =
        withContext(Dispatchers.IO) {
            try {
                PDDocument.load(File(filePath)).use { document ->
                    val totalPages = document.numberOfPages
                    val count = minOf(totalPages, maxPages)

                    (0 until count).mapNotNull { index ->
                        try {
                            // Use high-quality renderer for thumbnails too, but at lower DPI for performance
                            pdfRenderer.renderPage(
                                filePath,
                                index,
                                HighQualityPdfRenderer.RenderOptions(
                                    dpi = 96f, // Good balance of quality and performance for thumbnails
                                    highQuality = true
                                )
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

    private fun BufferedImage.toImageBitmap(): ImageBitmap {
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(this, "png", outputStream)
        val byteArray = outputStream.toByteArray()
        val skiaImage = org.jetbrains.skia.Image.makeFromEncoded(byteArray)
        return skiaImage.toComposeImageBitmap()
    }



   private fun extractAllTextData(filePath: String): Map<Int, List<TextPositionData>> {
        PDDocument.load(File(filePath)).use { document ->
            val stripper = PositionAwareTextStripper()

            stripper.getText(document)

            return stripper.allPageTextData.mapValues { it.value.toList() }
        }
   }

   //function to get to convert allTextData to a map of page number to textOnly
    private fun getTextOnlyData(): Map<Int, String> {
        return allTextDataWithCoordinates.mapValues { entry ->
            entry.value.joinToString(" ") { it.text }
        }
    }

    // Extract page sizes (MediaBox width/height) in PDF points for each page
    private fun extractPageSizesPoints(filePath: String): Map<Int, Size> {
        PDDocument.load(File(filePath)).use { document ->
            val sizes = mutableMapOf<Int, Size>()
            for (i in 0 until document.numberOfPages) {
                val page = document.getPage(i)
                val mb = page.mediaBox
                sizes[i] = Size(mb.width, mb.height)
            }
            return sizes
        }
    }

    private suspend fun getTableOfContents(filePath: String): List<TableOfContentData> = withContext(Dispatchers.IO) {
        PDDocument.load(File(filePath)).use { document ->
            val outline: PDDocumentOutline? = document.documentCatalog.documentOutline

            if (outline == null) return@withContext emptyList()

            // Process the children of the root outline. Note the use of children()
            return@withContext outline.children().mapNotNull {
                if (it is PDOutlineItem) processOutlineItem(it, document) else null
            }
        }
    }

     //Helper function to recursively process the PDOutline tree.

    private fun processOutlineItem(item: PDOutlineItem, document: PDDocument): TableOfContentData? {
        val title = item.title
        var pageIndex = -1
        var destinationY = 0f

        val destination = item.action as? PDActionGoTo

        if (destination != null) {
            val dest = destination.destination as? PDPageDestination

            if (dest != null) {
                for (i in 0 until document.numberOfPages) {
                    if (document.getPage(i) == dest.page) {
                        pageIndex = i
                        destinationY = if (dest is PDPageXYZDestination) {
                            dest.top?.toFloat() ?: 0f
                        } else {
                            0f
                        }
                        break
                    }
                }
            }
        }

        if (pageIndex == -1) return null

        val children = item.children().mapNotNull {
            if (it is PDOutlineItem) processOutlineItem(it, document) else null
        }

        return TableOfContentData(
            title = title,
            pageIndex = pageIndex,
            destinationY = destinationY,
            children = children
        )
    }

    /** Add a highlight for the currently displayed page.
     * rects are normalized rectangles in the displayed orientation. We convert them back to
     * unrotated page coordinates before storing.
     */
    fun addHighlightForDisplayedPage(displayedRects: List<Rect>, colorArgb: Long, pageRotation: Float) {
        val original = getOriginalPageIndex(selectedPageIndex)
        val rotInt = when (val rot = ((pageRotation % 360f) + 360f) % 360f) {
            in 45f..135f -> 90
            in 135f..225f -> 180
            in 225f..315f -> 270
            else -> 0
        }
        val unrotatedRects = displayedRects.map { inverseRotateRectNormalized(it.left, it.top, it.width, it.height, rotInt) }
        val list = highlightsByPage[original]?.toMutableList() ?: mutableListOf()
        list.add(HighlightMark(unrotatedRects, colorArgb))
        highlightsByPage = highlightsByPage.toMutableMap().apply { put(original, list) }
        hasUnsavedHighlights = true
    }

    /** Return highlights for the currently displayed page in unrotated normalized coordinates. */
    fun getHighlightsForDisplayedPage(): List<HighlightMark> {
        val original = getOriginalPageIndex(selectedPageIndex)
        return highlightsByPage[original] ?: emptyList()
    }

    private fun inverseRotateRectNormalized(l: Float, t: Float, w: Float, h: Float, angle: Int): Rect {
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

    // Merge adjacent rectangles that lie on the same line (in normalized unrotated coordinates)
    private fun mergeRectsOnLinesNormalized(rects: List<Rect>): List<Rect> {
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

    /**
     * Save all current changes (page order, bookmarks metadata, highlights) to a new file
     */
    fun saveChangesAs(outputPath: String) {
        if (isSaving) return
        screenModelScope.launch {
            isSaving = true
            saveResult = null
            try {
                val result = withContext(Dispatchers.IO) {
                    saveCombined(pdfFile.path, outputPath)
                }
                saveResult = result
                if (result is SaveResult.Success) {
                    hasPageChanges = false
                    hasUnsavedBookmarks = false
                    hasUnsavedHighlights = false
                }
            } catch (e: Exception) {
                saveResult = SaveResult.Error("Save failed: ${e.message}")
                e.printStackTrace()
            } finally {
                isSaving = false
            }
        }
    }

    /**
     * Save all current changes (page order, bookmarks metadata, highlights) over the original file
     */
    fun saveChanges() {
        val tempOutputPath = "${pdfFile.path}.tmp"
        screenModelScope.launch {
            isSaving = true
            saveResult = null
            try {
                val result = withContext(Dispatchers.IO) { saveCombined(pdfFile.path, tempOutputPath) }
                if (result is SaveResult.Success) {
                    val originalFile = File(pdfFile.path)
                    val tempFile = File(tempOutputPath)
                    if (tempFile.exists()) {
                        val backupPath = "${pdfFile.path}.backup"
                        val backupFile = File(backupPath)
                        if (backupFile.exists()) backupFile.delete()
                        originalFile.renameTo(backupFile)
                        if (tempFile.renameTo(originalFile)) {
                            backupFile.delete()
                            saveResult = SaveResult.Success(pdfFile.path, pageOrder.size)
                            hasPageChanges = false
                            hasUnsavedBookmarks = false
                            hasUnsavedHighlights = false
                        } else {
                            backupFile.renameTo(originalFile)
                            saveResult = SaveResult.Error("Failed to replace original file")
                        }
                    } else {
                        saveResult = SaveResult.Error("Temporary file was not created")
                    }
                } else {
                    saveResult = result
                }
            } catch (e: Exception) {
                saveResult = SaveResult.Error("Save failed: ${e.message}")
                e.printStackTrace()
            } finally {
                File(tempOutputPath).delete()
                isSaving = false
            }
        }
    }

    private fun saveCombined(inputPath: String, outputPath: String): SaveResult {
        return try {
            val inputFile = File(inputPath)
            val outputFile = File(outputPath)
            if (!inputFile.exists()) return SaveResult.Error("Input file does not exist: $inputPath")
            outputFile.parentFile?.mkdirs()
            PDDocument.load(inputFile).use { inputDoc ->
                PDDocument().use { outputDoc ->
                    // Copy pages in reordered order and apply highlights for each original page
                    val maxPage = inputDoc.numberOfPages - 1
                    val invalid = pageOrder.filter { it < 0 || it > maxPage }
                    if (invalid.isNotEmpty()) return SaveResult.Error("Invalid page indices: $invalid")
                    pageOrder.forEach { originalIndex ->
                        val sourcePage = inputDoc.getPage(originalIndex)
                        val imported = outputDoc.importPage(sourcePage)
                        if (outputDoc.pages.indexOf(imported) == -1) outputDoc.addPage(imported)
                        // Apply highlights that belong to this original page onto the imported page
                        val marks = highlightsByPage[originalIndex] ?: emptyList()
                        if (marks.isNotEmpty()) applyHighlightsToPage(imported, marks)
                    }
                    // Copy document-level metadata
                    copyDocumentMetadata(inputDoc, outputDoc)
                    // Also persist bookmarks metadata
                    val info = outputDoc.documentInformation ?: PDDocumentInformation().also { outputDoc.documentInformation = it }
                    info.setCustomMetadataValue("Bookmarks", serializeBookmarks(bookmarks))
                    outputDoc.save(outputFile)
                }
            }
            SaveResult.Success(outputPath, pageOrder.size)
        } catch (e: Exception) {
            SaveResult.Error("Unexpected error: ${e.message}")
        }
    }

    private fun applyHighlightsToPage(page: PDPage, marks: List<HighlightMark>) {
        val mediaBox: PDRectangle = page.mediaBox
        val pageW = mediaBox.width
        val pageH = mediaBox.height
        val annotations = page.annotations
        marks.forEach { mark ->
            // Merge rects per line to reduce fragmentation
            val mergedRects = mergeRectsOnLinesNormalized(mark.rects)
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

    private fun copyDocumentMetadata(source: PDDocument, destination: PDDocument) {
        try {
            val sourceInfo = source.documentInformation
            if (sourceInfo != null) {
                val destInfo = destination.documentInformation ?: run {
                    val newInfo = PDDocumentInformation()
                    destination.documentInformation = newInfo
                    newInfo
                }
                destInfo.title = sourceInfo.title
                destInfo.author = sourceInfo.author
                destInfo.subject = sourceInfo.subject
                destInfo.creator = sourceInfo.creator
                destInfo.producer = sourceInfo.producer
                destInfo.keywords = sourceInfo.keywords
                destInfo.creationDate = sourceInfo.creationDate
                destInfo.modificationDate = java.util.Calendar.getInstance()
            }
        } catch (_: Exception) {
        }
    }

    // ...existing code...
}
