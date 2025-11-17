package com.jholachhapdevs.pdfjuggler.core.pdf

import java.io.File

/**
 * Stub for PDF page reordering utilities.
 *
 * The interactive page reordering feature has been removed. This class remains as a
 * compatibility stub so callers referencing its types (e.g. SaveResult, ValidationResult)
 * keep compiling. All operations return Error results indicating the feature was removed.
 */
class PdfPageReorderUtil {

    suspend fun saveReorderedPdf(inputFilePath: String, outputFilePath: String, pageOrder: List<Int>): SaveResult {
        return SaveResult.Error("Page reordering feature removed in this build")
    }

    suspend fun extractPages(inputFilePath: String, outputFilePath: String, pageIndices: List<Int>): SaveResult {
        return SaveResult.Error("Page extraction / reordering feature removed in this build")
    }

    suspend fun getPdfInfo(filePath: String): PdfInfoResult {
        return PdfInfoResult.Error("Pdf info unavailable from reorder util stub")
    }

    fun validateOutputPath(outputPath: String): ValidationResult {
        return try {
            val file = File(outputPath)
            val parentDir = file.parentFile
            when {
                parentDir != null && !parentDir.exists() && !parentDir.mkdirs() -> ValidationResult.Error("Cannot create directory: ${parentDir.absolutePath}")
                parentDir != null && !parentDir.canWrite() -> ValidationResult.Error("No write permission for directory: ${parentDir.absolutePath}")
                file.exists() && !file.canWrite() -> ValidationResult.Error("Cannot overwrite existing file: $outputPath")
                !outputPath.lowercase().endsWith(".pdf") -> ValidationResult.Warning("Output file should have .pdf extension")
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
