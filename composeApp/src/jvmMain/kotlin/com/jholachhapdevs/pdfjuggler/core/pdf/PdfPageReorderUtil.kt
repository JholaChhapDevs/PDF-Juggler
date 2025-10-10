package com.jholachhapdevs.pdfjuggler.core.pdf

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import java.io.File
import java.io.IOException

/**
 * Utility class for handling PDF page reordering and saving operations
 * using Apache PDFBox.
 */
class PdfPageReorderUtil {
    
    /**
     * Save a PDF with pages reordered according to the specified order
     * 
     * @param inputFilePath Path to the input PDF file
     * @param outputFilePath Path where the reordered PDF will be saved
     * @param pageOrder List of original page indices in the desired new order
     * @return Result indicating success or failure
     */
    suspend fun saveReorderedPdf(
        inputFilePath: String,
        outputFilePath: String,
        pageOrder: List<Int>
    ): SaveResult = withContext(Dispatchers.IO) {
        try {
            val inputFile = File(inputFilePath)
            val outputFile = File(outputFilePath)
            
            if (!inputFile.exists()) {
                return@withContext SaveResult.Error("Input file does not exist: $inputFilePath")
            }
            
            // Ensure output directory exists
            outputFile.parentFile?.mkdirs()
            
            // Load the input PDF
            PDDocument.load(inputFile).use { inputDoc ->
                // Create a new document for the reordered pages
                PDDocument().use { outputDoc ->
                    
                    // Validate page order indices
                    val maxPageIndex = inputDoc.numberOfPages - 1
                    val invalidIndices = pageOrder.filter { it < 0 || it > maxPageIndex }
                    if (invalidIndices.isNotEmpty()) {
                        return@withContext SaveResult.Error("Invalid page indices: $invalidIndices")
                    }
                    
                    // Copy pages in the specified order
                    for (originalPageIndex in pageOrder) {
                        val sourcePage = inputDoc.getPage(originalPageIndex)
                        
                        // Import the page to the new document
                        val importedPage = outputDoc.importPage(sourcePage)
                        
                        // Ensure the page is properly added
                        if (outputDoc.pages.indexOf(importedPage) == -1) {
                            outputDoc.addPage(importedPage)
                        }
                    }
                    
                    // Copy document-level metadata if available
                    copyDocumentMetadata(inputDoc, outputDoc)
                    
                    // Save the reordered PDF
                    outputDoc.save(outputFile)
                    
                    return@withContext SaveResult.Success(
                        outputPath = outputFilePath,
                        pageCount = pageOrder.size
                    )
                }
            }
        } catch (e: IOException) {
            return@withContext SaveResult.Error("IO error: ${e.message}")
        } catch (e: Exception) {
            return@withContext SaveResult.Error("Unexpected error: ${e.message}")
        }
    }
    
    /**
     * Extract specific pages from a PDF and save them as a new PDF
     * 
     * @param inputFilePath Path to the input PDF file
     * @param outputFilePath Path where the extracted pages will be saved
     * @param pageIndices List of page indices to extract (0-based)
     * @return Result indicating success or failure
     */
    suspend fun extractPages(
        inputFilePath: String,
        outputFilePath: String,
        pageIndices: List<Int>
    ): SaveResult = withContext(Dispatchers.IO) {
        try {
            val inputFile = File(inputFilePath)
            val outputFile = File(outputFilePath)
            
            if (!inputFile.exists()) {
                return@withContext SaveResult.Error("Input file does not exist: $inputFilePath")
            }
            
            // Ensure output directory exists
            outputFile.parentFile?.mkdirs()
            
            PDDocument.load(inputFile).use { inputDoc ->
                PDDocument().use { outputDoc ->
                    
                    // Validate page indices
                    val maxPageIndex = inputDoc.numberOfPages - 1
                    val invalidIndices = pageIndices.filter { it < 0 || it > maxPageIndex }
                    if (invalidIndices.isNotEmpty()) {
                        return@withContext SaveResult.Error("Invalid page indices: $invalidIndices")
                    }
                    
                    // Extract specified pages
                    for (pageIndex in pageIndices.sorted()) {
                        val sourcePage = inputDoc.getPage(pageIndex)
                        val importedPage = outputDoc.importPage(sourcePage)
                        
                        if (outputDoc.pages.indexOf(importedPage) == -1) {
                            outputDoc.addPage(importedPage)
                        }
                    }
                    
                    // Copy document-level metadata
                    copyDocumentMetadata(inputDoc, outputDoc)
                    
                    // Save the extracted pages
                    outputDoc.save(outputFile)
                    
                    return@withContext SaveResult.Success(
                        outputPath = outputFilePath,
                        pageCount = pageIndices.size
                    )
                }
            }
        } catch (e: IOException) {
            return@withContext SaveResult.Error("IO error: ${e.message}")
        } catch (e: Exception) {
            return@withContext SaveResult.Error("Unexpected error: ${e.message}")
        }
    }
    
    /**
     * Get information about a PDF file
     * 
     * @param filePath Path to the PDF file
     * @return PDF information or error
     */
    suspend fun getPdfInfo(filePath: String): PdfInfoResult = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                return@withContext PdfInfoResult.Error("File does not exist: $filePath")
            }
            
            PDDocument.load(file).use { doc ->
                val info = PdfInfo(
                    pageCount = doc.numberOfPages,
                    fileSize = file.length(),
                    title = doc.documentInformation?.title,
                    author = doc.documentInformation?.author,
                    subject = doc.documentInformation?.subject,
                    creationDate = doc.documentInformation?.creationDate?.time?.time,
                    modificationDate = doc.documentInformation?.modificationDate?.time?.time
                )
                return@withContext PdfInfoResult.Success(info)
            }
        } catch (e: IOException) {
            return@withContext PdfInfoResult.Error("IO error: ${e.message}")
        } catch (e: Exception) {
            return@withContext PdfInfoResult.Error("Unexpected error: ${e.message}")
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
                    val newInfo = org.apache.pdfbox.pdmodel.PDDocumentInformation()
                    destination.documentInformation = newInfo
                    newInfo
                }
                
                // Copy common metadata fields
                destInfo.title = sourceInfo.title
                destInfo.author = sourceInfo.author
                destInfo.subject = sourceInfo.subject
                destInfo.creator = sourceInfo.creator
                destInfo.producer = sourceInfo.producer
                destInfo.keywords = sourceInfo.keywords
                destInfo.creationDate = sourceInfo.creationDate
                destInfo.modificationDate = java.util.Calendar.getInstance()
            }
        } catch (e: Exception) {
            // Metadata copying is not critical, so we just log and continue
            println("Warning: Could not copy document metadata: ${e.message}")
        }
    }
    
    /**
     * Validate that a file can be written to the specified path
     */
    fun validateOutputPath(outputPath: String): ValidationResult {
        return try {
            val file = File(outputPath)
            val parentDir = file.parentFile
            
            when {
                parentDir != null && !parentDir.exists() && !parentDir.mkdirs() -> {
                    ValidationResult.Error("Cannot create directory: ${parentDir.absolutePath}")
                }
                parentDir != null && !parentDir.canWrite() -> {
                    ValidationResult.Error("No write permission for directory: ${parentDir.absolutePath}")
                }
                file.exists() && !file.canWrite() -> {
                    ValidationResult.Error("Cannot overwrite existing file: $outputPath")
                }
                !outputPath.lowercase().endsWith(".pdf") -> {
                    ValidationResult.Warning("Output file should have .pdf extension")
                }
                else -> ValidationResult.Valid
            }
        } catch (e: Exception) {
            ValidationResult.Error("Invalid output path: ${e.message}")
        }
    }
}

/**
 * Result of a PDF save operation
 */
sealed class SaveResult {
    data class Success(
        val outputPath: String,
        val pageCount: Int,
        val message: String? = null
    ) : SaveResult()
    
    data class Error(
        val message: String
    ) : SaveResult()
}

/**
 * Result of getting PDF information
 */
sealed class PdfInfoResult {
    data class Success(
        val info: PdfInfo
    ) : PdfInfoResult()
    
    data class Error(
        val message: String
    ) : PdfInfoResult()
}

/**
 * Information about a PDF file
 */
data class PdfInfo(
    val pageCount: Int,
    val fileSize: Long,
    val title: String?,
    val author: String?,
    val subject: String?,
    val creationDate: Long?,
    val modificationDate: Long?
)

/**
 * Result of path validation
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Warning(val message: String) : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}
