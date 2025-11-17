package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.IntSize
import com.jholachhapdevs.pdfjuggler.core.pdf.HighQualityPdfRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.min
import androidx.compose.ui.graphics.toComposeImageBitmap

/**
 * Manages PDF rendering operations for the PDF viewer
 */
class PdfRenderManager(
    private val filePath: String
) {
    // High-quality PDF renderer
    private val pdfRenderer = HighQualityPdfRenderer()
    
    var thumbnails by mutableStateOf<List<ImageBitmap>>(emptyList())
        private set

    var currentPageImage by mutableStateOf<ImageBitmap?>(null)
        private set

    var pageImages = mutableStateListOf<ImageBitmap?>()
        private set
    var thumbnailsList = mutableStateListOf<ImageBitmap?>()
        private set

    // Current zoom and viewport state for adaptive rendering
    private var currentViewport by mutableStateOf(IntSize.Zero)
    
    var currentZoom by mutableStateOf(1f)
        private set

    var currentRotation by mutableStateOf(0f)
        private set

    // Remove dummy thumbnail and ensure thumbnailsList is always sized correctly
    init {
        // No dummy thumbnail
    }

    /**
     * Render page with high quality using adaptive DPI
     */
    suspend fun renderPageHighQuality(pageIndex: Int): ImageBitmap? {
        return if (currentViewport.width > 0 && currentViewport.height > 0) {
            // Use adaptive rendering based on viewport and zoom
            pdfRenderer.renderPageAdaptiveWithRotation(
                filePath,
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
                filePath,
                pageIndex,
                HighQualityPdfRenderer.RenderOptions(dpi = adaptiveDPI, rotation = currentRotation)
            )
        }
    }

    /**
     * Update current page image
     */
    fun updateCurrentPageImage(image: ImageBitmap?) {
        currentPageImage = image
    }

    /**
     * Render thumbnails for all pages
     */
    suspend fun renderThumbnails(maxPages: Int): List<ImageBitmap> = 
        withContext(Dispatchers.IO) {
            try {
                PDDocument.load(File(filePath)).use { document ->
                    val totalPages = document.numberOfPages
                    val count = min(totalPages, maxPages)

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

    /**
     * Render thumbnails for all pages progressively and update the UI as each is ready
     */
    suspend fun renderThumbnailsProgressively(maxPages: Int, onThumbnailRendered: ((Int, ImageBitmap?) -> Unit)? = null) =
        withContext(Dispatchers.IO) {
            try {
                PDDocument.load(File(filePath)).use { document ->
                    val totalPages = document.numberOfPages
                    val count = min(totalPages, maxPages)
                    val newThumbnails = MutableList<ImageBitmap?>(count) { null }
                    for (index in 0 until count) {
                        try {
                            val thumbnail = pdfRenderer.renderPage(
                                filePath,
                                index,
                                HighQualityPdfRenderer.RenderOptions(
                                    dpi = 48f, // Lower DPI for faster, lighter thumbnails
                                    highQuality = false
                                )
                            )
                            newThumbnails[index] = thumbnail
                            withContext(Dispatchers.Main) {
                                // Update the public thumbnails list as each is ready
                                thumbnails = newThumbnails.filterNotNull()
                                onThumbnailRendered?.invoke(index, thumbnail)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    /**
     * Update thumbnails order based on page order
     */
    suspend fun updateThumbnailOrder(pageOrder: List<Int>) {
        try {
            // Re-render thumbnails in new order
            val reorderedThumbnails = mutableListOf<ImageBitmap>()
            for (originalPageIndex in pageOrder) {
                val thumbnail = pdfRenderer.renderPage(
                    filePath,
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

    /**
     * Initialize thumbnails directly
     */
    fun initializeThumbnails(newThumbnails: List<ImageBitmap>) {
        thumbnails = newThumbnails
    }

    /**
     * Zoom in
     */
    suspend fun zoomIn(pageIndex: Int, onPageRerender: suspend (ImageBitmap?) -> Unit) {
        currentZoom = (currentZoom * 1.25f).coerceAtMost(5f)
        try {
            val newImage = renderPageHighQuality(pageIndex)
            onPageRerender(newImage)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Zoom out
     */
    suspend fun zoomOut(pageIndex: Int, onPageRerender: suspend (ImageBitmap?) -> Unit) {
        currentZoom = (currentZoom / 1.25f).coerceAtLeast(0.25f)
        try {
            val newImage = renderPageHighQuality(pageIndex)
            onPageRerender(newImage)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Reset zoom
     */
    suspend fun resetZoom(pageIndex: Int, onPageRerender: suspend (ImageBitmap?) -> Unit) {
        currentZoom = 1f
        try {
            val newImage = renderPageHighQuality(pageIndex)
            onPageRerender(newImage)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Rotate clockwise
     */
    suspend fun rotateClockwise(pageIndex: Int, onPageRerender: suspend (ImageBitmap?) -> Unit) {
        currentRotation = (currentRotation + 90f) % 360f
        // Re-render current page with new rotation
        try {
            val newImage = renderPageHighQuality(pageIndex)
            onPageRerender(newImage)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Rotate counter-clockwise
     */
    suspend fun rotateCounterClockwise(pageIndex: Int, onPageRerender: suspend (ImageBitmap?) -> Unit) {
        currentRotation = (currentRotation - 90f + 360f) % 360f
        // Re-render current page with new rotation
        try {
            val newImage = renderPageHighQuality(pageIndex)
            onPageRerender(newImage)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Reset rotation
     */
    suspend fun resetRotation(pageIndex: Int, onPageRerender: suspend (ImageBitmap?) -> Unit) {
        currentRotation = 0f
        // Re-render current page with no rotation
        try {
            val newImage = renderPageHighQuality(pageIndex)
            onPageRerender(newImage)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Called when zoom level changes to re-render at appropriate quality
     */
    suspend fun onZoomChanged(zoomFactor: Float, pageIndex: Int, onPageRerender: suspend (ImageBitmap?) -> Unit) {
        if (zoomFactor != currentZoom) {
            currentZoom = zoomFactor
            // Re-render current page at new zoom level for better quality
            try {
                val newImage = renderPageHighQuality(pageIndex)
                onPageRerender(newImage)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Called when viewport size changes
     */
    suspend fun onViewportChanged(viewport: IntSize, pageIndex: Int, onPageRerender: suspend (ImageBitmap?) -> Unit) {
        if (viewport != currentViewport && viewport.width > 0 && viewport.height > 0) {
            currentViewport = viewport
            // Re-render for new viewport if significant change
            try {
                val newImage = renderPageHighQuality(pageIndex)
                onPageRerender(newImage)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Render all pages progressively, updating the provided lists and invoking callbacks
     */
    suspend fun renderAllPagesProgressively(totalPages: Int, onPageRendered: ((Int, ImageBitmap?) -> Unit)? = null, onThumbnailRendered: ((Int, ImageBitmap?) -> Unit)? = null) {
        // Ensure lists are the correct size
        if (pageImages.size != totalPages) {
            pageImages.clear()
            repeat(totalPages) { pageImages.add(null) }
        }
        if (thumbnailsList.size != totalPages) {
            thumbnailsList.clear()
            repeat(totalPages) { thumbnailsList.add(null) }
        }
        println("[PdfRenderManager] Starting progressive rendering for $totalPages pages")
        for (pageIndex in 0 until totalPages) {
            try {
                val image = renderPageHighQuality(pageIndex)
                pageImages[pageIndex] = image
                onPageRendered?.invoke(pageIndex, image)
                if (image != null) {
                    val thumbnail = pdfRenderer.renderPage(
                        filePath,
                        pageIndex,
                        HighQualityPdfRenderer.RenderOptions(
                            dpi = 96f,
                            highQuality = true
                        )
                    )
                    thumbnailsList[pageIndex] = thumbnail
                    println("[Thumbnail] Page $pageIndex thumbnail set: ${thumbnail != null}")
                    onThumbnailRendered?.invoke(pageIndex, thumbnail)
                } else {
                    thumbnailsList[pageIndex] = null
                    println("[Thumbnail] Page $pageIndex image null, thumbnail not set")
                }
                println("[PdfRenderManager] thumbnailsList[$pageIndex] updated, now: ${thumbnailsList.count { it != null }} non-null thumbnails")
            } catch (e: Exception) {
                e.printStackTrace()
                thumbnailsList[pageIndex] = null
                println("[Thumbnail] Exception for page $pageIndex: ${e.message}")
            }
        }
    }

    /**
     * Deprecated fallback render method
     */
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

    /**
     * Extension function to convert BufferedImage to ImageBitmap (custom implementation)
     */
    private fun BufferedImage.toImageBitmap(): ImageBitmap {
        return this.toComposeImageBitmap()
    }

    /**
     * Get current zoom level
     */
    fun getZoomLevel(): Float = currentZoom

    /**
     * Get current rotation angle
     */
    fun getRotationAngle(): Float = currentRotation
}