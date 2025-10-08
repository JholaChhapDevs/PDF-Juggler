package com.jholachhapdevs.pdfjuggler.service

import com.jholachhapdevs.pdfjuggler.feature.pdf.ui.component.PrintOptions
import org.apache.pdfbox.multipdf.LayerUtility
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.printing.PDFPageable
import org.apache.pdfbox.util.Matrix
import java.awt.print.PrinterJob
import java.io.File
import javax.print.PrintServiceLookup
import javax.print.attribute.HashPrintRequestAttributeSet
import javax.print.attribute.standard.Copies
import javax.print.attribute.standard.Sides

class PdfGenerationService {

    /**
     * Generate PDF and directly open print dialog without saving
     */
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

            // Print directly without saving to file
            printDocument(outputDoc, copies, duplex)

        } finally {
            sourceDoc.close()
            outputDoc.close()
        }
    }

    /**
     * Generate PDF, save it, then print
     */
    fun generateSaveAndPrint(sourcePath: String, outputPath: String, options: PrintOptions, copies: Int = 1, duplex: Boolean = false) {
        // Generate and save PDF
        generatePdf(sourcePath, outputPath, options)

        // Print the saved PDF
        printPdf(outputPath, copies, duplex)
    }

    /**
     * Print a PDDocument directly (opens print dialog)
     */
    private fun printDocument(document: PDDocument, copies: Int = 1, duplex: Boolean = false): Boolean {
        return try {
            val printerJob = PrinterJob.getPrinterJob()

            // Set up print attributes
            val attributes = HashPrintRequestAttributeSet()
            attributes.add(Copies(copies))

            if (duplex) {
                attributes.add(Sides.DUPLEX)
            }

            // Use PDFPageable for better page handling
            printerJob.setPageable(PDFPageable(document))

            // Show print dialog
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

    /**
     * Print an existing PDF file (opens print dialog)
     */
    fun printPdf(pdfPath: String, copies: Int = 1, duplex: Boolean = false): Boolean {
        val document = PDDocument.load(File(pdfPath))

        return try {
            printDocument(document, copies, duplex)
        } finally {
            document.close()
        }
    }

    /**
     * Print directly to default printer without dialog
     */
    fun printPdfSilently(pdfPath: String, copies: Int = 1, duplex: Boolean = false): Boolean {
        val document = PDDocument.load(File(pdfPath))

        return try {
            val printerJob = PrinterJob.getPrinterJob()

            // Get default printer
            val printService = PrintServiceLookup.lookupDefaultPrintService()
            if (printService == null) {
                println("No default printer found")
                return false
            }

            printerJob.printService = printService

            // Set up print attributes
            val attributes = HashPrintRequestAttributeSet()
            attributes.add(Copies(copies))

            if (duplex) {
                attributes.add(Sides.DUPLEX)
            }

            printerJob.setPageable(PDFPageable(document))

            // Print without dialog
            printerJob.print(attributes)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            document.close()
        }
    }

    /**
     * Print to a specific printer without dialog
     */
    fun printPdfToSpecificPrinter(pdfPath: String, printerName: String, copies: Int = 1, duplex: Boolean = false): Boolean {
        val document = PDDocument.load(File(pdfPath))

        return try {
            val printerJob = PrinterJob.getPrinterJob()

            // Find the printer
            val printServices = PrintServiceLookup.lookupPrintServices(null, null)
            val printService = printServices.find { it.name.equals(printerName, ignoreCase = true) }

            if (printService == null) {
                println("Printer '$printerName' not found")
                return false
            }

            printerJob.printService = printService

            // Set up print attributes
            val attributes = HashPrintRequestAttributeSet()
            attributes.add(Copies(copies))

            if (duplex) {
                attributes.add(Sides.DUPLEX)
            }

            printerJob.setPageable(PDFPageable(document))

            // Print without dialog
            printerJob.print(attributes)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            document.close()
        }
    }

    /**
     * Get list of available printers
     */
    fun getAvailablePrinters(): List<String> {
        val printServices = PrintServiceLookup.lookupPrintServices(null, null)
        return printServices.map { it.name }
    }

    fun generatePdf(sourcePath: String, outputPath: String, options: PrintOptions) {
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
            outputDoc.save(outputPath)
        } finally {
            sourceDoc.close()
            outputDoc.close()
        }
    }

    private fun generatePagesPerSheet(sourceDoc: PDDocument, outputDoc: PDDocument, pagesPerSheet: Int) {
        val sourcePages = sourceDoc.pages.toList()

        if (pagesPerSheet <= 1) {
            sourcePages.forEach { outputDoc.addPage(it) }
            return
        }

        val pageGroups = sourcePages.chunked(pagesPerSheet)
        val layerUtility = LayerUtility(outputDoc)

        for (group in pageGroups) {
            val targetPageSize = PDRectangle.A4
            val targetPage = PDPage(targetPageSize)
            outputDoc.addPage(targetPage)

            val contentStream = PDPageContentStream(outputDoc, targetPage)

            val cols = if (pagesPerSheet > 1) 2 else 1
            val rows = (pagesPerSheet + cols - 1) / cols

            val cellWidth = targetPageSize.width / cols
            val cellHeight = targetPageSize.height / rows

            group.forEachIndexed { index, sourcePage ->
                val form = layerUtility.importPageAsForm(sourceDoc, sourcePage)

                val col = index % cols
                val row = rows - 1 - (index / cols)

                val targetX = col * cellWidth
                val targetY = row * cellHeight

                val sourceSize = sourcePage.mediaBox
                val scale = minOf(cellWidth / sourceSize.width, cellHeight / sourceSize.height)

                val matrix = Matrix()
                matrix.translate(targetX, targetY)
                matrix.scale(scale, scale)

                contentStream.saveGraphicsState()
                contentStream.transform(matrix)
                contentStream.drawForm(form)
                contentStream.restoreGraphicsState()
            }
            contentStream.close()
        }
    }

    private fun generateBooklet(sourceDoc: PDDocument, outputDoc: PDDocument) {
        val sourcePages = sourceDoc.pages.toList()
        val totalPages = sourcePages.size
        val sheets = (totalPages + 3) / 4
        val bookletPages = sheets * 4

        val layerUtility = LayerUtility(outputDoc)

        for (i in 0 until sheets) {
            val page1Index = i * 2
            val page2Index = bookletPages - 1 - (i * 2)
            val page3Index = i * 2 + 1
            val page4Index = bookletPages - 2 - (i * 2)

            val targetPage = PDPage(PDRectangle.A4)
            outputDoc.addPage(targetPage)

            val contentStream = PDPageContentStream(outputDoc, targetPage)

            if (page2Index < totalPages) {
                drawPageForBooklet(layerUtility, contentStream, sourceDoc, sourcePages[page2Index], targetPage.mediaBox, true)
            }

            if (page1Index < totalPages) {
                drawPageForBooklet(layerUtility, contentStream, sourceDoc, sourcePages[page1Index], targetPage.mediaBox, false)
            }

            contentStream.close()

            val targetPage2 = PDPage(PDRectangle.A4)
            outputDoc.addPage(targetPage2)

            val contentStream2 = PDPageContentStream(outputDoc, targetPage2)

            if (page3Index < totalPages) {
                drawPageForBooklet(layerUtility, contentStream2, sourceDoc, sourcePages[page3Index], targetPage2.mediaBox, false)
            }

            if (page4Index < totalPages) {
                drawPageForBooklet(layerUtility, contentStream2, sourceDoc, sourcePages[page4Index], targetPage2.mediaBox, true)
            }

            contentStream2.close()
        }
    }

    private fun drawPageForBooklet(layerUtility: LayerUtility, contentStream: PDPageContentStream, sourceDoc: PDDocument, sourcePage: PDPage, targetMediaBox: PDRectangle, isRight: Boolean) {
        val form = layerUtility.importPageAsForm(sourceDoc, sourcePage)
        val sourceSize = sourcePage.mediaBox

        val cellWidth = targetMediaBox.width / 2
        val cellHeight = targetMediaBox.height

        val scale = minOf(cellWidth / sourceSize.width, cellHeight / sourceSize.height)

        val scaledWidth = sourceSize.width * scale
        val scaledHeight = sourceSize.height * scale

        val xOffset = if (isRight) cellWidth + (cellWidth - scaledWidth) / 2 else (cellWidth - scaledWidth) / 2
        val yOffset = (cellHeight - scaledHeight) / 2

        val matrix = Matrix()
        matrix.translate(xOffset, yOffset)
        matrix.scale(scale, scale)

        contentStream.saveGraphicsState()
        contentStream.transform(matrix)
        contentStream.drawForm(form)
        contentStream.restoreGraphicsState()
    }
}