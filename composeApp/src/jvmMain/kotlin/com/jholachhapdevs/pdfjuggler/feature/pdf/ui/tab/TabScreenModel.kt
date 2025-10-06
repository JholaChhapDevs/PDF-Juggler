package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.PdfFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

class TabScreenModel(
    val pdfFile: PdfFile,
) : ScreenModel {

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

    init {
        loadPdf()
    }

    private fun loadPdf() {
        screenModelScope.launch {
            isLoading = true
            try {
                totalPages = getTotalPages(pdfFile.path)
                thumbnails = renderThumbnails(pdfFile.path, totalPages)
                currentPageImage = renderPage(pdfFile.path, 0, 150f)
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
                currentPageImage = renderPage(pdfFile.path, pageIndex, 150f)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun getTotalPages(filePath: String): Int = withContext(Dispatchers.IO) {
        PDDocument.load(File(filePath)).use { doc ->
            doc.numberOfPages
        }
    }

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
                    val renderer = PDFRenderer(document)
                    val totalPages = document.numberOfPages
                    val count = minOf(totalPages, maxPages)

                    (0 until count).mapNotNull { index ->
                        try {
                            val bufferedImage: BufferedImage = renderer.renderImageWithDPI(index, 72f, ImageType.RGB)
                            bufferedImage.toImageBitmap()
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
}