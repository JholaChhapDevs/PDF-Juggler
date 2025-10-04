package com.jholachhapdevs.pdfjuggler.ui.pdf

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.rendering.ImageType
import org.jetbrains.skia.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

object PdfUtils {

    // Load PDF and count pages
    fun getTotalPages(filePath: String): Int {
        PDDocument.load(File(filePath)).use { doc ->
            return doc.numberOfPages
        }
    }

    // Render a single page to ImageBitmap
    fun renderPage(filePath: String, pageIndex: Int, dpi: Float = 150f): ImageBitmap? {
        PDDocument.load(File(filePath)).use { document ->
            val renderer = PDFRenderer(document)
            val bufferedImage: BufferedImage = renderer.renderImageWithDPI(pageIndex, dpi, ImageType.RGB)
            return bufferedImage.toImageBitmap()
        }
    }

    // Render all thumbnails (low-res)
    fun renderThumbnails(filePath: String, maxPages: Int = 10): List<ImageBitmap?> {
        PDDocument.load(File(filePath)).use { document ->
            val renderer = PDFRenderer(document)
            val totalPages = document.numberOfPages
            val count = minOf(totalPages, maxPages)

            return (0 until count).map { index ->
                val bufferedImage: BufferedImage = renderer.renderImageWithDPI(index, 50f, ImageType.RGB)
                bufferedImage.toImageBitmap()
            }
        }
    }

    // Convert BufferedImage → ImageBitmap (for Compose)
    private fun BufferedImage.toImageBitmap(): ImageBitmap {
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(this, "png", outputStream)
        val byteArray = outputStream.toByteArray()
        val skiaImage = Image.makeFromEncoded(byteArray)
        return skiaImage.toComposeImageBitmap()
    }
}
