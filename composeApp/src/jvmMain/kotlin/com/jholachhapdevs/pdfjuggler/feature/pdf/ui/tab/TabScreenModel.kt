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
import com.jholachhapdevs.pdfjuggler.feature.pdf.ui.PositionAwareTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

class TabScreenModel(
    val pdfFile: PdfFile,
    private val window: java.awt.Window? = null
) : ScreenModel {
    
    // High-quality PDF renderer
    private val pdfRenderer = HighQualityPdfRenderer()
    
    // PDF page reordering utility
    private val pdfReorderUtil = PdfPageReorderUtil()

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
    private var currentZoom by mutableStateOf(1f)
    private var currentViewport by mutableStateOf(IntSize.Zero)
    
    // Current rotation angle (0, 90, 180, 270 degrees)
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
     * Get the display index for a given original page index
     */
    fun getDisplayIndex(originalPageIndex: Int): Int {
        return pageOrder.indexOf(originalPageIndex)
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
}
