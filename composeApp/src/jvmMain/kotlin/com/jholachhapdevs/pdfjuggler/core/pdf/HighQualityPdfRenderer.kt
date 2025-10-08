package com.jholachhapdevs.pdfjuggler.core.pdf

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

/**
 * High-quality PDF renderer that adapts DPI based on zoom level and viewport size
 * for crisp rendering similar to Adobe Reader or Chrome's PDF viewer.
 */
class HighQualityPdfRenderer {
    
    // Cache for rendered pages at different scales
    private val pageCache = ConcurrentHashMap<PageCacheKey, ImageBitmap>()
    
    // Maximum cache size (in number of pages)
    private val maxCacheSize = 50
    
    data class PageCacheKey(
        val filePath: String,
        val pageIndex: Int,
        val dpi: Float,
        val rotation: Float,
        val hash: String // Hash of file modification time for cache invalidation
    )
    
    data class RenderOptions(
        val dpi: Float = calculateAdaptiveDPI(),
        val imageType: ImageType = ImageType.RGB,
        val antiAliasing: Boolean = true,
        val highQuality: Boolean = true,
        val rotation: Float = 0f
    )
    
    companion object {
        // Base DPI for standard screens (96 DPI is Windows standard)
        private const val BASE_DPI = 96f
        
        // Minimum and maximum DPI limits
        private const val MIN_DPI = 72f
        private const val MAX_DPI = 600f
        
        /**
         * Calculate adaptive DPI based on system DPI and desired quality
         */
        fun calculateAdaptiveDPI(zoomFactor: Float = 1f): Float {
            // Get system DPI (defaulting to 96 if unavailable)
            val systemDPI = try {
                java.awt.Toolkit.getDefaultToolkit().screenResolution.toFloat()
            } catch (e: Exception) {
                BASE_DPI
            }
            
            // Calculate target DPI based on zoom and system DPI
            val targetDPI = systemDPI * zoomFactor * 2f // 2x multiplier for crisp rendering
            
            return targetDPI.coerceIn(MIN_DPI, MAX_DPI)
        }
        
        /**
         * Calculate optimal DPI for a given viewport size and PDF page dimensions
         */
        fun calculateOptimalDPI(
            pageWidthPt: Float,
            pageHeightPt: Float,
            viewportWidthPx: Int,
            viewportHeightPx: Int,
            zoomFactor: Float = 1f
        ): Float {
            // Convert points to inches (72 points = 1 inch)
            val pageWidthInches = pageWidthPt / 72f
            val pageHeightInches = pageHeightPt / 72f
            
            // Calculate DPI needed to fit viewport
            val dpiForWidth = (viewportWidthPx / pageWidthInches) * zoomFactor
            val dpiForHeight = (viewportHeightPx / pageHeightInches) * zoomFactor
            
            // Use the smaller DPI to ensure page fits, then apply quality multiplier
            val baseDPI = minOf(dpiForWidth, dpiForHeight) * 1.5f // 1.5x for extra quality
            
            return baseDPI.coerceIn(MIN_DPI, MAX_DPI)
        }
    }
    
    /**
     * Render a single page with high quality and caching
     */
    suspend fun renderPage(
        filePath: String,
        pageIndex: Int,
        options: RenderOptions = RenderOptions()
    ): ImageBitmap? = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) return@withContext null
            
            val fileHash = "${file.lastModified()}-${file.length()}"
            val cacheKey = PageCacheKey(filePath, pageIndex, options.dpi, options.rotation, fileHash)
            
            // Check cache first
            pageCache[cacheKey]?.let { return@withContext it }
            
            // Render the page
            PDDocument.load(file).use { document ->
                if (pageIndex >= document.numberOfPages) return@withContext null
                
                val renderer = PDFRenderer(document)
                val bufferedImage = renderer.renderImageWithDPI(
                    pageIndex,
                    options.dpi,
                    options.imageType
                )
                
                // Apply high-quality rendering hints if requested
                val qualityImage = if (options.highQuality) {
                    applyHighQualityHints(bufferedImage)
                } else {
                    bufferedImage
                }
                
                // Apply rotation if needed
                val finalImage = if (options.rotation != 0f) {
                    rotateImage(qualityImage, options.rotation)
                } else {
                    qualityImage
                }
                
                val imageBitmap = finalImage.toComposeImageBitmap()
                
                // Cache the result
                cacheResult(cacheKey, imageBitmap)
                
                imageBitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Render page with adaptive DPI based on viewport size
     */
    suspend fun renderPageAdaptive(
        filePath: String,
        pageIndex: Int,
        viewportWidthPx: Int,
        viewportHeightPx: Int,
        zoomFactor: Float = 1f
    ): ImageBitmap? = withContext(Dispatchers.IO) {
        try {
            // First, get page dimensions
            PDDocument.load(File(filePath)).use { document ->
                if (pageIndex >= document.numberOfPages) return@withContext null
                
                val page = document.getPage(pageIndex)
                val mediaBox = page.mediaBox
                
                val optimalDPI = calculateOptimalDPI(
                    mediaBox.width,
                    mediaBox.height,
                    viewportWidthPx,
                    viewportHeightPx,
                    zoomFactor
                )
                
                renderPage(
                    filePath,
                    pageIndex,
                    RenderOptions(dpi = optimalDPI)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Render page with adaptive DPI and rotation support
     */
    suspend fun renderPageAdaptiveWithRotation(
        filePath: String,
        pageIndex: Int,
        viewportWidthPx: Int,
        viewportHeightPx: Int,
        zoomFactor: Float = 1f,
        rotation: Float = 0f
    ): ImageBitmap? = withContext(Dispatchers.IO) {
        try {
            // First, get page dimensions
            PDDocument.load(File(filePath)).use { document ->
                if (pageIndex >= document.numberOfPages) return@withContext null
                
                val page = document.getPage(pageIndex)
                val mediaBox = page.mediaBox
                
                val optimalDPI = calculateOptimalDPI(
                    mediaBox.width,
                    mediaBox.height,
                    viewportWidthPx,
                    viewportHeightPx,
                    zoomFactor
                )
                
                renderPage(
                    filePath,
                    pageIndex,
                    RenderOptions(dpi = optimalDPI, rotation = rotation)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Apply high-quality rendering hints to improve image quality
     */
    private fun applyHighQualityHints(source: BufferedImage): BufferedImage {
        val width = source.width
        val height = source.height
        
        val enhanced = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g2d = enhanced.createGraphics()
        
        try {
            // Apply high-quality rendering hints
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY)
            g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY)
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
            
            g2d.drawImage(source, 0, 0, null)
            
        } finally {
            g2d.dispose()
        }
        
        return enhanced
    }
    
    /**
     * Rotate image by the specified angle (in degrees)
     */
    private fun rotateImage(source: BufferedImage, angleDegrees: Float): BufferedImage {
        val angleRadians = Math.toRadians(angleDegrees.toDouble())
        val sin = Math.abs(Math.sin(angleRadians))
        val cos = Math.abs(Math.cos(angleRadians))
        
        val originalWidth = source.width
        val originalHeight = source.height
        
        // Calculate new dimensions after rotation
        val newWidth = (originalWidth * cos + originalHeight * sin).toInt()
        val newHeight = (originalWidth * sin + originalHeight * cos).toInt()
        
        val rotated = BufferedImage(newWidth, newHeight, source.type)
        val g2d = rotated.createGraphics()
        
        try {
            // Apply high-quality rendering hints for rotation
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            
            // Set up the transform to rotate around center
            val transform = AffineTransform()
            transform.translate(newWidth / 2.0, newHeight / 2.0)
            transform.rotate(angleRadians)
            transform.translate(-originalWidth / 2.0, -originalHeight / 2.0)
            
            g2d.transform = transform
            g2d.drawImage(source, 0, 0, null)
            
        } finally {
            g2d.dispose()
        }
        
        return rotated
    }
    
    /**
     * Cache a rendered result with size management
     */
    private fun cacheResult(key: PageCacheKey, image: ImageBitmap) {
        if (pageCache.size >= maxCacheSize) {
            // Remove oldest entries (simple FIFO strategy)
            val keysToRemove = pageCache.keys.take(10)
            keysToRemove.forEach { pageCache.remove(it) }
        }
        
        pageCache[key] = image
    }
    
    /**
     * Clear cache for a specific file (useful when file is modified)
     */
    fun clearCacheForFile(filePath: String) {
        pageCache.keys.removeIf { it.filePath == filePath }
    }
    
    /**
     * Clear entire cache
     */
    fun clearCache() {
        pageCache.clear()
    }
    
    /**
     * Get cache statistics
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            size = pageCache.size,
            maxSize = maxCacheSize
        )
    }
    
    data class CacheStats(val size: Int, val maxSize: Int)
    
    /**
     * Convert BufferedImage to ImageBitmap with better compression
     */
    private fun BufferedImage.toComposeImageBitmap(): ImageBitmap {
        val outputStream = ByteArrayOutputStream()
        
        // Use PNG for better quality (though larger file size)
        ImageIO.write(this, "PNG", outputStream)
        val byteArray = outputStream.toByteArray()
        
        val skiaImage = org.jetbrains.skia.Image.makeFromEncoded(byteArray)
        return skiaImage.toComposeImageBitmap()
    }
}
