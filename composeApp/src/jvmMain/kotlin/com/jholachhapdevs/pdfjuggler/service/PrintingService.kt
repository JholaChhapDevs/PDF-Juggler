package com.jholachhapdevs.pdfjuggler.service

import com.jholachhapdevs.pdfjuggler.feature.pdf.ui.component.PrintOptions
import org.apache.pdfbox.multipdf.LayerUtility
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject
import org.apache.pdfbox.printing.PDFPageable
import org.apache.pdfbox.util.Matrix
import java.awt.print.PrinterJob
import java.io.File
import javax.print.PrintServiceLookup
import javax.print.attribute.HashPrintRequestAttributeSet
import javax.print.attribute.standard.Copies
import javax.print.attribute.standard.Sides

class PdfGenerationService {

    fun generateAndPrint(sourcePath: String, options: PrintOptions, copies: Int = 1, duplex: Boolean = false) {
        val sourceDoc = PDDocument.load(File(sourcePath))
        val outputDoc = PDDocument()

        try {
            if (!options.includeAnnotations) {
                sourceDoc.pages.forEach { it.annotations = emptyList() }
            }

            if (options.bookletFormat) {
                generateBooklet(sourceDoc, outputDoc)
            } else {
                generatePagesPerSheet(sourceDoc, outputDoc, options.pagesPerSheet)
            }

            printDocument(outputDoc, copies, duplex)

        } finally {
            sourceDoc.close()
            outputDoc.close()
        }
    }

    fun generateSaveAndPrint(sourcePath: String, outputPath: String, options: PrintOptions, copies: Int = 1, duplex: Boolean = false) {
        generatePdf(sourcePath, outputPath, options)
        printPdf(outputPath, copies, duplex)
    }

    private fun printDocument(document: PDDocument, copies: Int = 1, duplex: Boolean = false): Boolean {
        return try {
            val printerJob = PrinterJob.getPrinterJob()
            val attributes = HashPrintRequestAttributeSet()
            attributes.add(Copies(copies))

            if (duplex) {
                attributes.add(Sides.DUPLEX)
            }

            printerJob.setPageable(PDFPageable(document))

            if (printerJob.printDialog(attributes)) {
                printerJob.print(attributes)
                return true
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun printPdf(pdfPath: String, copies: Int = 1, duplex: Boolean = false): Boolean {
        val document = PDDocument.load(File(pdfPath))

        return try {
            printDocument(document, copies, duplex)
        } finally {
            document.close()
        }
    }

    fun printPdfSilently(pdfPath: String, copies: Int = 1, duplex: Boolean = false): Boolean {
        val document = PDDocument.load(File(pdfPath))

        return try {
            val printerJob = PrinterJob.getPrinterJob()
            val printService = PrintServiceLookup.lookupDefaultPrintService()
            if (printService == null) {
                println("No default printer found")
                return false
            }

            printerJob.printService = printService
            val attributes = HashPrintRequestAttributeSet()
            attributes.add(Copies(copies))

            if (duplex) {
                attributes.add(Sides.DUPLEX)
            }

            printerJob.setPageable(PDFPageable(document))
            printerJob.print(attributes)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            document.close()
        }
    }

    fun printPdfToSpecificPrinter(pdfPath: String, printerName: String, copies: Int = 1, duplex: Boolean = false): Boolean {
        val document = PDDocument.load(File(pdfPath))

        return try {
            val printerJob = PrinterJob.getPrinterJob()
            val printServices = PrintServiceLookup.lookupPrintServices(null, null)
            val printService = printServices.find { it.name.equals(printerName, ignoreCase = true) }

            if (printService == null) {
                println("Printer '$printerName' not found")
                return false
            }

            printerJob.printService = printService
            val attributes = HashPrintRequestAttributeSet()
            attributes.add(Copies(copies))

            if (duplex) {
                attributes.add(Sides.DUPLEX)
            }

            printerJob.setPageable(PDFPageable(document))
            printerJob.print(attributes)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            document.close()
        }
    }

    fun getAvailablePrinters(): List<String> {
        val printServices = PrintServiceLookup.lookupPrintServices(null, null)
        return printServices.map { it.name }
    }

    fun generatePdf(sourcePath: String, outputPath: String, options: PrintOptions) {
        println("Starting PDF generation: source=$sourcePath, output=$outputPath")
        println("Options: pagesPerSheet=${options.pagesPerSheet}, booklet=${options.bookletFormat}")

        var sourceDoc: PDDocument? = null
        var outputDoc: PDDocument? = null

        try {
            sourceDoc = PDDocument.load(File(sourcePath))
            println("Source document loaded successfully. Pages: ${sourceDoc.numberOfPages}")

            outputDoc = PDDocument()

            if (!options.includeAnnotations) {
                sourceDoc.pages.forEach { it.annotations = emptyList() }
            }

            if (options.bookletFormat) {
                println("Generating booklet format...")
                generateBooklet(sourceDoc, outputDoc)
            } else {
                println("Generating ${options.pagesPerSheet} pages per sheet...")
                generatePagesPerSheet(sourceDoc, outputDoc, options.pagesPerSheet)
            }

            println("Output document has ${outputDoc.numberOfPages} pages")

            // Ensure output directory exists
            val outputFile = File(outputPath)
            outputFile.parentFile?.mkdirs()

            println("Saving to: ${outputFile.absolutePath}")
            outputDoc.save(outputFile)
            println("PDF saved successfully. File size: ${outputFile.length()} bytes")

        } catch (e: Exception) {
            println("Error generating PDF: ${e.message}")
            e.printStackTrace()
            throw e
        } finally {
            sourceDoc?.close()
            outputDoc?.close()
            println("Documents closed")
        }
    }

    private fun generatePagesPerSheet(sourceDoc: PDDocument, outputDoc: PDDocument, pagesPerSheet: Int) {
        val sourcePages = sourceDoc.pages.toList()
        println("generatePagesPerSheet: sourcePages=${sourcePages.size}, pagesPerSheet=$pagesPerSheet")

        if (sourcePages.isEmpty()) {
            println("ERROR: No pages found in source document")
            return
        }

        // Create a temporary document to flatten problematic PDFs
        val tempDoc = PDDocument()
        val layerUtility = LayerUtility(tempDoc)

        // Import all pages to temp doc first (this handles complex PDFs better)
        sourcePages.forEachIndexed { index, page ->
            try {
                val form = layerUtility.importPageAsForm(sourceDoc, index)
                val tempPage = PDPage(page.mediaBox)
                tempDoc.addPage(tempPage)

                val tempStream = PDPageContentStream(tempDoc, tempPage, PDPageContentStream.AppendMode.APPEND, true)
                tempStream.drawForm(form)
                tempStream.close()
            } catch (e: Exception) {
                println("Error importing page $index: ${e.message}")
                // If import fails, try to copy the page directly
                tempDoc.addPage(page)
            }
        }

        val outputLayerUtility = LayerUtility(outputDoc)

        // Handle single page per sheet
        if (pagesPerSheet <= 1) {
            println("Processing 1 page per sheet (simple copy)")
            tempDoc.pages.forEachIndexed { index, page ->
                try {
                    outputDoc.addPage(page)
                    println("Added page ${index + 1}")
                } catch (e: Exception) {
                    println("Error processing page $index: ${e.message}")
                    e.printStackTrace()
                }
            }
            tempDoc.close()
            return
        }

        val tempPages = tempDoc.pages.toList()
        val pageGroups = tempPages.chunked(pagesPerSheet)
        println("Created ${pageGroups.size} groups")

        // Determine layout based on pages per sheet
        val (cols, rows) = when (pagesPerSheet) {
            2 -> Pair(2, 1)
            3, 4 -> Pair(2, 2)
            6 -> Pair(3, 2)
            8, 9 -> Pair(3, 3)
            else -> Pair(2, 2)
        }
        println("Layout: ${cols}x${rows}")

        pageGroups.forEachIndexed { groupIndex, group ->
            try {
                val targetPageSize = PDRectangle.A4
                val targetPage = PDPage(targetPageSize)
                outputDoc.addPage(targetPage)

                val contentStream = PDPageContentStream(
                    outputDoc,
                    targetPage,
                    PDPageContentStream.AppendMode.APPEND,
                    true
                )

                val cellWidth = targetPageSize.width / cols
                val cellHeight = targetPageSize.height / rows
                println("Group $groupIndex: cellWidth=$cellWidth, cellHeight=$cellHeight")

                group.forEachIndexed { index, _ ->
                    try {
                        val pageIndex = groupIndex * pagesPerSheet + index
                        val form = outputLayerUtility.importPageAsForm(tempDoc, pageIndex)

                        val col = index % cols
                        val row = index / cols

                        val sourceSize = tempPages[pageIndex].mediaBox
                        val scaleX = cellWidth / sourceSize.width
                        val scaleY = cellHeight / sourceSize.height
                        val scale = minOf(scaleX, scaleY) * 0.90f

                        val scaledWidth = sourceSize.width * scale
                        val scaledHeight = sourceSize.height * scale

                        val xOffset = col * cellWidth + (cellWidth - scaledWidth) / 2
                        val yOffset = targetPageSize.height - (row + 1) * cellHeight + (cellHeight - scaledHeight) / 2

                        println("Page $pageIndex: pos=($col,$row), offset=($xOffset,$yOffset), scale=$scale")

                        contentStream.saveGraphicsState()
                        contentStream.transform(Matrix.getTranslateInstance(xOffset, yOffset))
                        contentStream.transform(Matrix.getScaleInstance(scale, scale))
                        contentStream.drawForm(form)
                        contentStream.restoreGraphicsState()

                    } catch (e: Exception) {
                        println("Error drawing page at index $index in group $groupIndex: ${e.message}")
                        e.printStackTrace()
                    }
                }

                contentStream.close()
                println("Completed group $groupIndex")

            } catch (e: Exception) {
                println("Error processing group $groupIndex: ${e.message}")
                e.printStackTrace()
            }
        }

        tempDoc.close()
    }

    private fun generateBooklet(sourceDoc: PDDocument, outputDoc: PDDocument) {
        val sourcePages = sourceDoc.pages.toList()
        println("generateBooklet: sourcePages=${sourcePages.size}")

        if (sourcePages.isEmpty()) {
            println("ERROR: No pages found in source document")
            return
        }

        // Create a temporary document to flatten problematic PDFs
        val tempDoc = PDDocument()
        val layerUtility = LayerUtility(tempDoc)

        // Import all pages to temp doc first
        sourcePages.forEachIndexed { index, page ->
            try {
                val form = layerUtility.importPageAsForm(sourceDoc, index)
                val tempPage = PDPage(page.mediaBox)
                tempDoc.addPage(tempPage)

                val tempStream = PDPageContentStream(tempDoc, tempPage, PDPageContentStream.AppendMode.APPEND, true)
                tempStream.drawForm(form)
                tempStream.close()
                println("Successfully flattened page $index for booklet")
            } catch (e: Exception) {
                println("Error importing page $index, trying direct copy: ${e.message}")
                // If import fails, try to copy the page directly
                try {
                    tempDoc.addPage(page)
                } catch (e2: Exception) {
                    println("Failed to copy page $index: ${e2.message}")
                    e2.printStackTrace()
                }
            }
        }

        val totalPages = tempDoc.numberOfPages
        val sheets = (totalPages + 3) / 4
        val bookletPages = sheets * 4
        println("Booklet: totalPages=$totalPages, sheets=$sheets, bookletPages=$bookletPages")

        if (totalPages == 0) {
            println("ERROR: No pages in temp document after flattening")
            tempDoc.close()
            return
        }

        val outputLayerUtility = LayerUtility(outputDoc)

        for (i in 0 until sheets) {
            // Calculate page indices for booklet ordering
            val page1Index = bookletPages - 1 - (i * 2)
            val page2Index = i * 2
            val page3Index = i * 2 + 1
            val page4Index = bookletPages - 2 - (i * 2)

            println("Sheet $i: pages [$page2Index, $page1Index] and [$page3Index, $page4Index]")

            // First sheet (outer pages) - Use landscape A4
            val targetPage1 = PDPage(PDRectangle(PDRectangle.A4.height, PDRectangle.A4.width))
            outputDoc.addPage(targetPage1)

            val contentStream1 = PDPageContentStream(
                outputDoc,
                targetPage1,
                PDPageContentStream.AppendMode.APPEND,
                true
            )

            if (page2Index < totalPages) {
                println("Drawing left page: $page2Index")
                drawPageForBooklet(outputLayerUtility, contentStream1, tempDoc, page2Index, targetPage1.mediaBox, false)
            }
            if (page1Index < totalPages) {
                println("Drawing right page: $page1Index")
                drawPageForBooklet(outputLayerUtility, contentStream1, tempDoc, page1Index, targetPage1.mediaBox, true)
            }
            contentStream1.close()

            // Second sheet (inner pages) - Use landscape A4
            val targetPage2 = PDPage(PDRectangle(PDRectangle.A4.height, PDRectangle.A4.width))
            outputDoc.addPage(targetPage2)

            val contentStream2 = PDPageContentStream(
                outputDoc,
                targetPage2,
                PDPageContentStream.AppendMode.APPEND,
                true
            )

            if (page3Index < totalPages) {
                println("Drawing left page: $page3Index")
                drawPageForBooklet(outputLayerUtility, contentStream2, tempDoc, page3Index, targetPage2.mediaBox, false)
            }
            if (page4Index < totalPages) {
                println("Drawing right page: $page4Index")
                drawPageForBooklet(outputLayerUtility, contentStream2, tempDoc, page4Index, targetPage2.mediaBox, true)
            }
            contentStream2.close()
        }

        tempDoc.close()
    }

    private fun drawPageForBooklet(
        layerUtility: LayerUtility,
        contentStream: PDPageContentStream,
        sourceDoc: PDDocument,
        pageIndex: Int,
        targetMediaBox: PDRectangle,
        isRight: Boolean
    ) {
        try {
            val form = layerUtility.importPageAsForm(sourceDoc, pageIndex)
            val sourcePage = sourceDoc.getPage(pageIndex)
            val sourceSize = sourcePage.mediaBox

            // Each page occupies half of the landscape page (left or right)
            val cellWidth = targetMediaBox.width / 2
            val cellHeight = targetMediaBox.height

            val scaleX = cellWidth / sourceSize.width
            val scaleY = cellHeight / sourceSize.height
            val scale = minOf(scaleX, scaleY) * 0.98f

            val scaledWidth = sourceSize.width * scale
            val scaledHeight = sourceSize.height * scale

            // Center horizontally in the half, and vertically on the page
            val xOffset = if (isRight) {
                cellWidth + (cellWidth - scaledWidth) / 2
            } else {
                (cellWidth - scaledWidth) / 2
            }
            val yOffset = (cellHeight - scaledHeight) / 2

            println("Booklet page $pageIndex: isRight=$isRight, offset=($xOffset,$yOffset), scale=$scale, cell=${cellWidth}x${cellHeight}")

            contentStream.saveGraphicsState()
            contentStream.transform(Matrix.getTranslateInstance(xOffset, yOffset))
            contentStream.transform(Matrix.getScaleInstance(scale, scale))
            contentStream.drawForm(form)
            contentStream.restoreGraphicsState()

        } catch (e: Exception) {
            println("Error drawing booklet page $pageIndex: ${e.message}")
            e.printStackTrace()
        }
    }
}