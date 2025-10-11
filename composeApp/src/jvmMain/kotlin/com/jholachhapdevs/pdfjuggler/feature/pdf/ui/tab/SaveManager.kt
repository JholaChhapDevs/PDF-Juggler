package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jholachhapdevs.pdfjuggler.core.pdf.PdfPageReorderUtil
import com.jholachhapdevs.pdfjuggler.core.pdf.SaveResult
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.PdfFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDDocumentInformation
import java.io.File

/**
 * Manages save operations for the PDF viewer
 */
class SaveManager(
    private val pdfFile: PdfFile,
    private val bookmarkManager: BookmarkManager,
    private val highlightManager: HighlightManager
) {
    // PDF page reordering utility
    private val pdfReorderUtil = PdfPageReorderUtil()

    var isSaving by mutableStateOf(false)
        private set
        
    private var _saveResult by mutableStateOf<SaveResult?>(null)
    val saveResult: SaveResult? get() = _saveResult

    /**
     * Save the PDF with current page ordering to a new file
     */
    suspend fun savePdfAs(outputPath: String, pageOrder: List<Int>) {
        if (isSaving) return
        
        isSaving = true
        _saveResult = null
        
        try {
            val result = pdfReorderUtil.saveReorderedPdf(
                inputFilePath = pdfFile.path,
                outputFilePath = outputPath,
                pageOrder = pageOrder
            )
            
            _saveResult = result
            
        } catch (e: Exception) {
            _saveResult = SaveResult.Error("Save failed: ${e.message}")
            e.printStackTrace()
        } finally {
            isSaving = false
        }
    }
    
    /**
     * Save the PDF with current page ordering, overwriting the original file
     */
    suspend fun savePdf(pageOrder: List<Int>) {
        // Create a temporary file with reordered pages
        val tempOutputPath = "${pdfFile.path}.tmp"
        
        isSaving = true
        _saveResult = null
        
        try {
            // Save to temporary file first
            val result = pdfReorderUtil.saveReorderedPdf(
                inputFilePath = pdfFile.path,
                outputFilePath = tempOutputPath,
                pageOrder = pageOrder
            )
            
            if (result is SaveResult.Success) {
                // Replace original file with the reordered version
                val originalFile = File(pdfFile.path)
                val tempFile = File(tempOutputPath)
                
                if (tempFile.exists()) {
                    // Backup original file
                    val backupPath = "${pdfFile.path}.backup"
                    val backupFile = File(backupPath)
                    if (backupFile.exists()) backupFile.delete()
                    originalFile.renameTo(backupFile)
                    
                    // Move temp file to original location
                    if (tempFile.renameTo(originalFile)) {
                        // Delete backup on successful replacement
                        backupFile.delete()
                        _saveResult = SaveResult.Success(pdfFile.path, pageOrder.size)
                    } else {
                        // Restore backup if replacement failed
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
            _saveResult = SaveResult.Error("Save failed: ${e.message}")
            e.printStackTrace()
        } finally {
            // Clean up temp file
            File(tempOutputPath).delete()
            isSaving = false
        }
    }

    /**
     * Save all current changes (page order, bookmarks metadata, highlights) to a new file
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
            _saveResult = SaveResult.Error("Save failed: ${e.message}")
            e.printStackTrace()
        } finally {
            isSaving = false
        }
    }

    /**
     * Save all current changes (page order, bookmarks metadata, highlights) over the original file
     */
    suspend fun saveChanges(pageOrder: List<Int>) {
        val tempOutputPath = "${pdfFile.path}.tmp"
        
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
                    val backupPath = "${pdfFile.path}.backup"
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
            _saveResult = SaveResult.Error("Save failed: ${e.message}")
            e.printStackTrace()
        } finally {
            File(tempOutputPath).delete()
            isSaving = false
        }
    }

    /**
     * Internal method to save combined changes
     */
    private fun saveCombined(inputPath: String, outputPath: String, pageOrder: List<Int>): SaveResult {
        return try {
            val inputFile = File(inputPath)
            val outputFile = File(outputPath)
            if (!inputFile.exists()) return SaveResult.Error("Input file does not exist: $inputPath")
            outputFile.parentFile?.mkdirs()
            
            PDDocument.load(inputFile).use { inputDoc ->
                PDDocument().use { outputDoc ->
                    // Copy pages in reordered order and apply highlights for each original page
                    val maxPage = inputDoc.numberOfPages - 1
                    val invalid = pageOrder.filter { it < 0 || it > maxPage }
                    if (invalid.isNotEmpty()) return SaveResult.Error("Invalid page indices: $invalid")
                    
                    pageOrder.forEach { originalIndex ->
                        val sourcePage = inputDoc.getPage(originalIndex)
                        val imported = outputDoc.importPage(sourcePage)
                        if (outputDoc.pages.indexOf(imported) == -1) outputDoc.addPage(imported)
                        
                        // Apply highlights that belong to this original page onto the imported page
                        val marks = highlightManager.getHighlightsForDisplayedPage(originalIndex)
                        if (marks.isNotEmpty()) {
                            highlightManager.applyHighlightsToPage(imported, marks)
                        }
                    }
                    // Copy document-level metadata
                    copyDocumentMetadata(inputDoc, outputDoc)
                    
                    // Also persist bookmarks metadata
                    val info = outputDoc.documentInformation ?: PDDocumentInformation().also { 
                        outputDoc.documentInformation = it 
                    }
                    info.setCustomMetadataValue("Bookmarks", TabScreenUtils.serializeBookmarks(bookmarkManager.bookmarks))
                    outputDoc.save(outputFile)
                }
            }
            SaveResult.Success(outputPath, pageOrder.size)
        } catch (e: Exception) {
            SaveResult.Error("Unexpected error: ${e.message}")
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
     * Validate an output path for saving
     */
    fun validateSavePath(outputPath: String) = pdfReorderUtil.validateOutputPath(outputPath)

    /**
     * Check if currently saving
     */
    fun isCurrentlySaving(): Boolean = isSaving

    /**
     * Get current save result
     */
    fun getCurrentSaveResult(): SaveResult? = _saveResult
}