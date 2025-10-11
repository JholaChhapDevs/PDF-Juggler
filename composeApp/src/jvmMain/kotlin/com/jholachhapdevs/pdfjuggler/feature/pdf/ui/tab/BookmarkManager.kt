package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jholachhapdevs.pdfjuggler.core.pdf.SaveResult
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.BookmarkData
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.PdfFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDDocumentInformation
import java.io.File

/**
 * Manages bookmark operations for the PDF viewer
 */
class BookmarkManager(
    private val pdfFile: PdfFile
) {
    var bookmarks by mutableStateOf<List<BookmarkData>>(emptyList())
        private set

    var hasUnsavedBookmarks by mutableStateOf(false)
        private set

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
     * Clear all bookmarks
     */
    fun clearAllBookmarks() {
        bookmarks = emptyList()
        hasUnsavedBookmarks = true
    }

    /**
     * Load bookmarks from PDF metadata
     */
    suspend fun loadBookmarksFromMetadata(): List<BookmarkData> {
        return withContext(Dispatchers.IO) {
            try {
                PDDocument.load(File(pdfFile.path)).use { document ->
                    val info = document.documentInformation
                    val bookmarksJson = info?.getCustomMetadataValue("Bookmarks")

                    println("Loading bookmarks from metadata: $bookmarksJson")

                    if (bookmarksJson != null) {
                        val loadedBookmarks = TabScreenUtils.deserializeBookmarks(bookmarksJson)
                        println("Loaded ${loadedBookmarks.size} bookmarks")
                        bookmarks = loadedBookmarks
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
     * Save bookmarks to PDF metadata
     */
    suspend fun saveBookmarksToMetadata(): SaveResult {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(pdfFile.path)

                // Create a temporary file to save to first
                val tempFile = File("${file.absolutePath}.tmp")

                PDDocument.load(file).use { document ->
                    // Get or create document information
                    val info = document.documentInformation ?: PDDocumentInformation()

                    // Serialize ALL bookmarks to a custom metadata field
                    val bookmarksJson = TabScreenUtils.serializeBookmarks(bookmarks)

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

                hasUnsavedBookmarks = false
                SaveResult.Success(pdfFile.path, bookmarks.size, "${bookmarks.size} bookmark(s) saved successfully")

            } catch (e: Exception) {
                e.printStackTrace()
                SaveResult.Error("Failed to save bookmarks: ${e.message}")
            }
        }
    }

    /**
     * Export bookmarks to a text file
     */
    suspend fun exportBookmarksToFile(outputPath: String): SaveResult {
        return withContext(Dispatchers.IO) {
            try {
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

                SaveResult.Success(outputPath, bookmarks.size, "Bookmarks exported successfully")

            } catch (e: Exception) {
                e.printStackTrace()
                SaveResult.Error("Failed to export bookmarks: ${e.message}")
            }
        }
    }

    /**
     * Initialize the bookmarks list directly (used during initialization)
     */
    fun initializeBookmarks(newBookmarks: List<BookmarkData>) {
        bookmarks = newBookmarks
        hasUnsavedBookmarks = false
    }

    /**
     * Mark bookmarks as saved
     */
    fun markBookmarksSaved() {
        hasUnsavedBookmarks = false
    }
}