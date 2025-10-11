package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.jholachhapdevs.pdfjuggler.core.pdf.SaveResult
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.TableOfContentData
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.PdfFile
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.TextPositionData
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.BookmarkData
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.HighlightMark
import com.jholachhapdevs.pdfjuggler.feature.pdf.ui.PositionAwareTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import java.io.File

/**
 * Refactored TabScreenModel that coordinates between various managers
 * following separation of concerns and single responsibility principles
 */
class TabScreenModel(
    val pdfFile: PdfFile,
    private val window: java.awt.Window? = null
) : ScreenModel {
    
    // Core managers
    private val bookmarkManager = BookmarkManager(pdfFile)
    private val highlightManager = HighlightManager()
    private val renderManager = PdfRenderManager(pdfFile.path)
    private val pageManager = PageManager { updateThumbnailOrder() }
    private val saveManager = SaveManager(pdfFile, bookmarkManager, highlightManager)
    
    // Search manager - initialized after core state is set up
    private lateinit var searchManager: SearchManager
    
    // Core state
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

    // Map of page index -> page size in PDF points (mediaBox width/height)
    var pageSizesPoints by mutableStateOf<Map<Int, Size>>(emptyMap())
        private set
        
    // Auto-scroll trigger for search matches
    private var _scrollToMatchTrigger by mutableStateOf(0)
    val scrollToMatchTrigger: Int get() = _scrollToMatchTrigger
    
    // Fullscreen state
    var isFullscreen by mutableStateOf(false)
        private set

    // Pending AI chat request from selection (dictionary/translate)
    var pendingAiRequest by mutableStateOf<AiRequest?>(null)
        private set
    
    // Delegate properties to managers
    val searchQuery: String get() = if (::searchManager.isInitialized) searchManager.searchQuery else ""
    val searchMatches: List<SearchMatch> get() = if (::searchManager.isInitialized) searchManager.searchMatches else emptyList()
    val currentSearchIndex: Int get() = if (::searchManager.isInitialized) searchManager.currentSearchIndex else -1
    val thumbnails: List<ImageBitmap> get() = renderManager.thumbnails
    val currentPageImage: ImageBitmap? get() = renderManager.currentPageImage
    val selectedPageIndex: Int get() = pageManager.getCurrentSelectedPageIndex()
    val pageOrder: List<Int> get() = pageManager.pageOrder
    val hasPageChanges: Boolean get() = pageManager.hasUnsavedPageChanges()
    val bookmarks: List<BookmarkData> get() = bookmarkManager.bookmarks
    val hasUnsavedBookmarks: Boolean get() = bookmarkManager.hasUnsavedBookmarks
    val highlightsByPage: Map<Int, List<HighlightMark>> get() = highlightManager.highlightsByPage
    val hasUnsavedHighlights: Boolean get() = highlightManager.hasUnsavedHighlights
    val currentZoom: Float get() = renderManager.getZoomLevel()
    val currentRotation: Float get() = renderManager.getRotationAngle()
    val isSaving: Boolean get() = saveManager.isCurrentlySaving()
    val saveResult: SaveResult? get() = saveManager.getCurrentSaveResult()
    
    init {
        initializeSearchManager()
        loadPdf()
    }
    
    private fun initializeSearchManager() {
        searchManager = SearchManager(
            getAllTextDataWithCoordinates = { allTextDataWithCoordinates },
            getTotalPages = { totalPages },
            getPageOrder = { pageManager.pageOrder },
            onNavigateToPage = { displayIndex -> selectPage(displayIndex) },
            onScrollToMatch = { scrollToCurrentMatch() }
        )
    }

    private fun loadPdf() {
        screenModelScope.launch {
            isLoading = true
            try {
                totalPages = getTotalPages(pdfFile.path)
                // Initialize page order with original sequence
                pageManager.initializePageOrder(totalPages)
                
                // Load thumbnails and current page image
                val thumbnailsList = renderManager.renderThumbnails(totalPages)
                renderManager.initializeThumbnails(thumbnailsList)
                val currentImage = renderManager.renderPageHighQuality(0)
                renderManager.updateCurrentPageImage(currentImage)
                
                // Load text data
                allTextDataWithCoordinates = extractAllTextData(pdfFile.path)
                allTextData = getTextOnlyData()
                tableOfContent = getTableOfContents(pdfFile.path)
                
                // Load bookmarks from PDF metadata
                val loadedBookmarks = bookmarkManager.loadBookmarksFromMetadata()
                bookmarkManager.initializeBookmarks(loadedBookmarks)
                
                // Extract page sizes in points for proper coordinate mapping
                pageSizesPoints = extractPageSizesPoints(pdfFile.path)

                // Clear previous search (if any) and recompute based on current query
                if (searchManager.searchQuery.isNotBlank()) {
                    searchManager.recomputeSearchMatches()
                } else {
                    searchManager.clearSearch()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    // ============ Navigation Methods ============
    fun selectPage(pageIndex: Int) {
        if (pageManager.selectPage(pageIndex)) {
            screenModelScope.launch {
                try {
                    // Get the original page index for rendering
                    val originalPageIndex = getOriginalPageIndex(pageIndex)
                    val newImage = renderManager.renderPageHighQuality(originalPageIndex)
                    renderManager.updateCurrentPageImage(newImage)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun getOriginalPageIndex(displayIndex: Int): Int {
        return pageManager.getOriginalPageIndex(displayIndex)
    }

    fun getPageSizePointsForDisplayIndex(displayIndex: Int): Size? {
        val original = getOriginalPageIndex(displayIndex)
        return pageSizesPoints[original]
    }

    // ============ Text Access Methods ============
    fun getCurrentPageText(): String {
        val originalPageIndex = getOriginalPageIndex(selectedPageIndex)
        return allTextData[originalPageIndex] ?: ""
    }

    fun getAllDocumentText(): String {
        return allTextData.values.joinToString("\n\n")
    }

    // ============ Search Methods ============
    fun updateSearchQuery(query: String) = searchManager.updateSearchQuery(query)
    fun clearSearch() = searchManager.clearSearch()
    fun goToNextMatch() = searchManager.goToNextMatch()
    fun goToPreviousMatch() = searchManager.goToPreviousMatch()
    
    fun currentMatchForDisplayedPage(): List<TextPositionData> {
        return searchManager.getCurrentMatchForDisplayedPage(selectedPageIndex) { getOriginalPageIndex(it) }
    }
    
    private fun scrollToCurrentMatch() {
        _scrollToMatchTrigger++
    }

    // ============ Bookmark Management Methods ============
    fun addBookmark(bookmark: BookmarkData) = bookmarkManager.addBookmark(bookmark)
    fun removeBookmark(bookmarkIndex: Int) = bookmarkManager.removeBookmark(bookmarkIndex)
    fun removeBookmarkForPage(pageIndex: Int) = bookmarkManager.removeBookmarkForPage(pageIndex)
    fun isPageBookmarked(pageIndex: Int): Boolean = bookmarkManager.isPageBookmarked(pageIndex)
    fun getBookmarkForPage(pageIndex: Int): BookmarkData? = bookmarkManager.getBookmarkForPage(pageIndex)
    fun clearAllBookmarks() = bookmarkManager.clearAllBookmarks()
    
    fun saveBookmarksToMetadata() {
        screenModelScope.launch {
            val result = bookmarkManager.saveBookmarksToMetadata()
            // Update save result through private access since SaveManager doesn't expose setter
            saveManager.clearSaveResult() 
        }
    }
    
    fun exportBookmarksToFile(outputPath: String) {
        screenModelScope.launch {
            val result = bookmarkManager.exportBookmarksToFile(outputPath)
            saveManager.clearSaveResult()
        }
    }

    // ============ Highlight Management Methods ============
    fun addHighlightForDisplayedPage(displayedRects: List<Rect>, colorArgb: Long, pageRotation: Float) {
        val original = getOriginalPageIndex(selectedPageIndex)
        highlightManager.addHighlightForDisplayedPage(original, displayedRects, colorArgb, pageRotation)
    }

    fun getHighlightsForDisplayedPage(): List<HighlightMark> {
        val original = getOriginalPageIndex(selectedPageIndex)
        return highlightManager.getHighlightsForDisplayedPage(original)
    }

    // ============ Rendering Methods ============
    fun zoomIn() {
        screenModelScope.launch {
            renderManager.zoomIn { newImage ->
                renderManager.updateCurrentPageImage(newImage)
            }
        }
    }

    fun zoomOut() {
        screenModelScope.launch {
            renderManager.zoomOut { newImage ->
                renderManager.updateCurrentPageImage(newImage)
            }
        }
    }

    fun resetZoom() {
        screenModelScope.launch {
            renderManager.resetZoom { newImage ->
                renderManager.updateCurrentPageImage(newImage)
            }
        }
    }

    fun rotateClockwise() {
        screenModelScope.launch {
            val originalIndex = getOriginalPageIndex(selectedPageIndex)
            renderManager.rotateClockwise(originalIndex) { newImage ->
                renderManager.updateCurrentPageImage(newImage)
            }
        }
    }
    
    fun rotateCounterClockwise() {
        screenModelScope.launch {
            val originalIndex = getOriginalPageIndex(selectedPageIndex)
            renderManager.rotateCounterClockwise(originalIndex) { newImage ->
                renderManager.updateCurrentPageImage(newImage)
            }
        }
    }
    
    fun resetRotation() {
        screenModelScope.launch {
            val originalIndex = getOriginalPageIndex(selectedPageIndex)
            renderManager.resetRotation(originalIndex) { newImage ->
                renderManager.updateCurrentPageImage(newImage)
            }
        }
    }

    fun onZoomChanged(zoomFactor: Float) {
        screenModelScope.launch {
            val originalIndex = getOriginalPageIndex(selectedPageIndex)
            renderManager.onZoomChanged(zoomFactor, originalIndex) { newImage ->
                renderManager.updateCurrentPageImage(newImage)
            }
        }
    }
    
    fun onViewportChanged(viewport: IntSize) {
        screenModelScope.launch {
            val originalIndex = getOriginalPageIndex(selectedPageIndex)
            renderManager.onViewportChanged(viewport, originalIndex) { newImage ->
                renderManager.updateCurrentPageImage(newImage)
            }
        }
    }

    // ============ Page Management Methods ============
    fun movePageUp(displayIndex: Int) {
        screenModelScope.launch {
            pageManager.movePageUp(displayIndex)
        }
    }
    
    fun movePageDown(displayIndex: Int) {
        screenModelScope.launch {
            pageManager.movePageDown(displayIndex)
        }
    }
    
    fun movePageToPosition(fromIndex: Int, toIndex: Int) {
        screenModelScope.launch {
            pageManager.movePageToPosition(fromIndex, toIndex)
        }
    }
    
    fun resetPageOrder() {
        screenModelScope.launch {
            pageManager.resetPageOrder(totalPages)
        }
    }

    private suspend fun updateThumbnailOrder() {
        renderManager.updateThumbnailOrder(pageManager.pageOrder)
    }

    // ============ Save Methods ============
    fun savePdfAs(outputPath: String) {
        screenModelScope.launch {
            saveManager.savePdfAs(outputPath, pageManager.pageOrder)
            if (saveResult is SaveResult.Success) {
                pageManager.markPageChangesSaved()
            }
        }
    }
    
    fun savePdf() {
        screenModelScope.launch {
            saveManager.savePdf(pageManager.pageOrder)
            if (saveResult is SaveResult.Success) {
                pageManager.markPageChangesSaved()
            }
        }
    }

    fun saveChangesAs(outputPath: String) {
        screenModelScope.launch {
            saveManager.saveChangesAs(outputPath, pageManager.pageOrder)
            if (saveResult is SaveResult.Success) {
                pageManager.markPageChangesSaved()
            }
        }
    }

    fun saveChanges() {
        screenModelScope.launch {
            saveManager.saveChanges(pageManager.pageOrder)
            if (saveResult is SaveResult.Success) {
                pageManager.markPageChangesSaved()
            }
        }
    }
    
    fun clearSaveResult() = saveManager.clearSaveResult()
    fun validateSavePath(outputPath: String) = saveManager.validateSavePath(outputPath)

    // ============ Fullscreen Methods ============
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

    // ============ AI Request Methods ============
    fun requestAiDictionary(text: String) {
        if (text.isNotBlank()) pendingAiRequest = AiRequest(text, AiRequestMode.Dictionary)
    }
    
    fun requestAiTranslate(text: String) {
        if (text.isNotBlank()) pendingAiRequest = AiRequest(text, AiRequestMode.Translate)
    }
    
    fun clearPendingAiRequest() { 
        pendingAiRequest = null 
    }

    // ============ Utility Methods ============
    private suspend fun getTotalPages(filePath: String): Int = withContext(Dispatchers.IO) {
        PDDocument.load(File(filePath)).use { doc ->
            doc.numberOfPages
        }
    }

    private fun extractAllTextData(filePath: String): Map<Int, List<TextPositionData>> {
        PDDocument.load(File(filePath)).use { document ->
            val stripper = PositionAwareTextStripper()
            stripper.getText(document)
            return stripper.allPageTextData.mapValues { it.value.toList() }
        }
    }

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

            // Process the children of the root outline
            return@withContext outline.children().mapNotNull {
                if (it is PDOutlineItem) processOutlineItem(it, document) else null
            }
        }
    }

    // Helper function to recursively process the PDOutline tree
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
}