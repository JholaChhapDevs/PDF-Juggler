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
) : ScreenModel {
    
    // High-quality PDF renderer
    private val pdfRenderer = HighQualityPdfRenderer()

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

    init {
        loadPdf()
    }

    private fun loadPdf() {
        screenModelScope.launch {
            isLoading = true
            try {
                totalPages = getTotalPages(pdfFile.path)
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
        if (pageIndex < 0 || pageIndex >= totalPages) return
        selectedPageIndex = pageIndex
        screenModelScope.launch {
            try {
                currentPageImage = renderPageHighQuality(pageIndex)
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
            pdfRenderer.renderPageAdaptive(
                pdfFile.path,
                pageIndex,
                currentViewport.width,
                currentViewport.height,
                currentZoom
            )
        } else {
            // Fallback to high-quality rendering with adaptive DPI
            val adaptiveDPI = HighQualityPdfRenderer.calculateAdaptiveDPI(currentZoom)
            pdfRenderer.renderPage(
                pdfFile.path,
                pageIndex,
                HighQualityPdfRenderer.RenderOptions(dpi = adaptiveDPI)
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
