package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
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

    // Current zoom and viewport state for adaptive rendering
    private var currentViewport by mutableStateOf(IntSize.Zero)
    
    var currentZoom by mutableStateOf(1f)
        private set

    var currentRotation by mutableStateOf(0f)
        private set

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
    suspend fun zoomIn(onPageRerender: suspend (ImageBitmap?) -> Unit) {
        currentZoom = (currentZoom * 1.25f).coerceAtMost(5f)
        try {
            val newImage = renderPageHighQuality(0) // Note: This needs the correct page index
            onPageRerender(newImage)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Zoom out
     */
    suspend fun zoomOut(onPageRerender: suspend (ImageBitmap?) -> Unit) {
        currentZoom = (currentZoom / 1.25f).coerceAtLeast(0.25f)
        try {
            val newImage = renderPageHighQuality(0) // Note: This needs the correct page index
            onPageRerender(newImage)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Reset zoom
     */
    suspend fun resetZoom(onPageRerender: suspend (ImageBitmap?) -> Unit) {
        currentZoom = 1f
        try {
            val newImage = renderPageHighQuality(0) // Note: This needs the correct page index
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
     * Extension function to convert BufferedImage to ImageBitmap
     */
    private fun BufferedImage.toImageBitmap(): ImageBitmap {
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(this, "png", outputStream)
        val byteArray = outputStream.toByteArray()
        val skiaImage = org.jetbrains.skia.Image.makeFromEncoded(byteArray)
        return skiaImage.toComposeImageBitmap()
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