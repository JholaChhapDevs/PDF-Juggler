package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jholachhapdevs.pdfjuggler.core.pdf.SaveResult
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.PdfFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDDocumentInformation
import java.io.File

/**
 * Manages save operations for the PDF viewer. Page reordering feature has been removed;
 * save APIs still accept a pageOrder parameter but it is expected to be the identity mapping.
 */
class SaveManager(
    private val pdfFile: PdfFile,
    private val bookmarkManager: BookmarkManager,
    private val highlightManager: HighlightManager
) {
    var isSaving by mutableStateOf(false)
        private set
        
    private var _saveResult by mutableStateOf<SaveResult?>(null)
    val saveResult: SaveResult? get() = _saveResult

    /**
     * Save the PDF to a new file (pageOrder parameter is accepted for compatibility).
     */
    suspend fun savePdfAs(outputPath: String, pageOrder: List<Int>) {
        if (isSaving) return
        isSaving = true
        _saveResult = null
        try {
            val result = withContext(Dispatchers.IO) {
                saveCombined(pdfFile.path, outputPath, pageOrder)
            }
            _saveResult = result
        } catch (e: Exception) {
            _saveResult = SaveResult.Error("Save failed: ${'$'}{e.message}")
            e.printStackTrace()
        } finally {
            isSaving = false
        }
    }
    
    /**
     * Save the PDF overwriting the original file (uses temp file replacement). Page reordering disabled.
     */
    suspend fun savePdf(pageOrder: List<Int>) {
        val tempOutputPath = "${'$'}{pdfFile.path}.tmp"
        isSaving = true
        _saveResult = null
        try {
            val result = withContext(Dispatchers.IO) {
                saveCombined(pdfFile.path, tempOutputPath, pageOrder)
            }
            if (result is SaveResult.Success) {
                val originalFile = File(pdfFile.path)
                val tempFile = File(tempOutputPath)
                if (tempFile.exists()) {
                    val backupPath = "${'$'}{pdfFile.path}.backup"
                    val backupFile = File(backupPath)
                    if (backupFile.exists()) backupFile.delete()
                    originalFile.renameTo(backupFile)
                    if (tempFile.renameTo(originalFile)) {
                        backupFile.delete()
                        _saveResult = SaveResult.Success(pdfFile.path, pageOrder.size)
                    } else {
                        backupFile.renameTo(originalFile)
                        _saveResult = SaveResult.Error("Failed to replace original file")
                    }
                } else {
                    _saveResult = SaveResult.Error("Temporary file was not created")
                }
            } else {
                _saveResult = result
            }
        } catch (e: Exception) {
            _saveResult = SaveResult.Error("Save failed: ${'$'}{e.message}")
            e.printStackTrace()
        } finally {
            File(tempOutputPath).delete()
            isSaving = false
        }
    }

    /**
     * Save all current changes (bookmarks/highlights and page order) to a new file
     */
    suspend fun saveChangesAs(outputPath: String, pageOrder: List<Int>) {
        if (isSaving) return
        isSaving = true
        _saveResult = null
        try {
            val result = withContext(Dispatchers.IO) {
                saveCombined(pdfFile.path, outputPath, pageOrder)
            }
            _saveResult = result
            if (result is SaveResult.Success) {
                bookmarkManager.markBookmarksSaved()
                highlightManager.markHighlightsSaved()
            }
        } catch (e: Exception) {
            _saveResult = SaveResult.Error("Save failed: ${'$'}{e.message}")
            e.printStackTrace()
        } finally {
            isSaving = false
        }
    }

    /**
     * Save all current changes over the original file
     */
    suspend fun saveChanges(pageOrder: List<Int>) {
        val tempOutputPath = "${'$'}{pdfFile.path}.tmp"
        isSaving = true
        _saveResult = null
        try {
            val result = withContext(Dispatchers.IO) {
                saveCombined(pdfFile.path, tempOutputPath, pageOrder)
            }
            if (result is SaveResult.Success) {
                val originalFile = File(pdfFile.path)
                val tempFile = File(tempOutputPath)
                if (tempFile.exists()) {
                    val backupPath = "${'$'}{pdfFile.path}.backup"
                    val backupFile = File(backupPath)
                    if (backupFile.exists()) backupFile.delete()
                    originalFile.renameTo(backupFile)
                    if (tempFile.renameTo(originalFile)) {
                        backupFile.delete()
                        _saveResult = SaveResult.Success(pdfFile.path, pageOrder.size)
                        bookmarkManager.markBookmarksSaved()
                        highlightManager.markHighlightsSaved()
                    } else {
                        backupFile.renameTo(originalFile)
                        _saveResult = SaveResult.Error("Failed to replace original file")
                    }
                } else {
                    _saveResult = SaveResult.Error("Temporary file was not created")
                }
            } else {
                _saveResult = result
            }
        } catch (e: Exception) {
            _saveResult = SaveResult.Error("Save failed: ${'$'}{e.message}")
            e.printStackTrace()
        } finally {
            File(tempOutputPath).delete()
            isSaving = false
        }
    }

    /**
     * Internal method to save combined changes (uses provided pageOrder but does not perform reordering feature)
     */
    private fun saveCombined(inputPath: String, outputPath: String, pageOrder: List<Int>): SaveResult {
        return try {
            val inputFile = File(inputPath)
            val outputFile = File(outputPath)
            if (!inputFile.exists()) return SaveResult.Error("Input file does not exist: $inputPath")
            outputFile.parentFile?.mkdirs()

            PDDocument.load(inputFile).use { inputDoc ->
                PDDocument().use { outputDoc ->
                    // Validate pageOrder indices
                    val maxPage = inputDoc.numberOfPages - 1
                    val invalid = pageOrder.filter { it < 0 || it > maxPage }
                    if (invalid.isNotEmpty()) return SaveResult.Error("Invalid page indices: $invalid")

                    // If pageOrder appears to be identity mapping, simply copy pages in order
                    val effectiveOrder = if (pageOrder.size == inputDoc.numberOfPages && pageOrder.withIndex().all { (i, v) -> i == v }) {
                        (0..maxPage).toList()
                    } else {
                        // Even though reordering UI is removed, we still respect the provided pageOrder for compatibility
                        pageOrder
                    }

                    effectiveOrder.forEach { originalIndex ->
                        val sourcePage = inputDoc.getPage(originalIndex)
                        val imported = outputDoc.importPage(sourcePage)
                        if (outputDoc.pages.indexOf(imported) == -1) outputDoc.addPage(imported)

                        // Apply highlights for the original page
                        val marks = highlightManager.getHighlightsForDisplayedPage(originalIndex)
                        if (marks.isNotEmpty()) {
                            highlightManager.applyHighlightsToPage(imported, marks)
                        }
                    }

                    // Copy metadata and bookmarks
                    copyDocumentMetadata(inputDoc, outputDoc)
                    val info = outputDoc.documentInformation ?: PDDocumentInformation().also { outputDoc.documentInformation = it }
                    info.setCustomMetadataValue("Bookmarks", TabScreenUtils.serializeBookmarks(bookmarkManager.bookmarks))
                    outputDoc.save(outputFile)
                }
            }
            SaveResult.Success(outputPath, pageOrder.size)
        } catch (e: Exception) {
            SaveResult.Error("Unexpected error: ${'$'}{e.message}")
        }
    }

    /**
     * Copy document metadata from source to destination
     */
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
            // Ignore metadata copy failures
        }
    }
    
    /**
     * Clear the last save result
     */
    fun clearSaveResult() {
        _saveResult = null
    }
    
    /**
     * Validate an output path for saving (basic checks)
     */
    fun validateSavePath(outputPath: String): com.jholachhapdevs.pdfjuggler.core.pdf.ValidationResult {
        return try {
            val file = File(outputPath)
            val parentDir = file.parentFile
            when {
                parentDir != null && !parentDir.exists() && !parentDir.mkdirs() -> com.jholachhapdevs.pdfjuggler.core.pdf.ValidationResult.Error("Cannot create directory: ${'$'}{parentDir.absolutePath}")
                parentDir != null && !parentDir.canWrite() -> com.jholachhapdevs.pdfjuggler.core.pdf.ValidationResult.Error("No write permission for directory: ${'$'}{parentDir.absolutePath}")
                file.exists() && !file.canWrite() -> com.jholachhapdevs.pdfjuggler.core.pdf.ValidationResult.Error("Cannot overwrite existing file: $outputPath")
                !outputPath.lowercase().endsWith(".pdf") -> com.jholachhapdevs.pdfjuggler.core.pdf.ValidationResult.Warning("Output file should have .pdf extension")
                else -> com.jholachhapdevs.pdfjuggler.core.pdf.ValidationResult.Valid
            }
        } catch (e: Exception) {
            com.jholachhapdevs.pdfjuggler.core.pdf.ValidationResult.Error("Invalid output path: ${'$'}{e.message}")
        }
    }

    /**
     * Check if currently saving
     */
    fun isCurrentlySaving(): Boolean = isSaving

    /**
     * Get current save result
     */
    fun getCurrentSaveResult(): SaveResult? = _saveResult
}