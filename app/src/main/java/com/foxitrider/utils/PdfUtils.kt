package com.foxitrider.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.multipdf.Splitter
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object PdfUtils {

    private const val TAG = "PdfUtils"

    fun initialize(context: Context) {
        PDFBoxResourceLoader.init(context)
    }

    fun getOutputDir(context: Context): File {
        val dir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "FoxitRider"
        )
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    // ─────────────── MERGE PDFs ───────────────
    suspend fun mergePdfs(
        context: Context,
        inputFiles: List<File>,
        outputFileName: String
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val outputFile = File(getOutputDir(context), outputFileName)
            val merger = PDFMergerUtility()
            inputFiles.forEach { file ->
                merger.addSource(file)
            }
            merger.destinationFileName = outputFile.absolutePath
            merger.mergeDocuments(null)
            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Merge error: ${e.message}")
            Result.failure(e)
        }
    }

    // ─────────────── SPLIT PDF ───────────────
    suspend fun splitPdf(
        context: Context,
        inputFile: File,
        startPage: Int,
        endPage: Int
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val document = PDDocument.load(inputFile)
            val splitter = Splitter()
            splitter.setStartPage(startPage)
            splitter.setEndPage(endPage)
            splitter.setSplitAtPage(endPage - startPage + 1)

            val pages = splitter.split(document)
            val outputName = "${inputFile.nameWithoutExtension}_pages_${startPage}_${endPage}.pdf"
            val outputFile = File(getOutputDir(context), outputName)

            if (pages.isNotEmpty()) {
                pages[0].save(outputFile)
                pages[0].close()
            }
            document.close()
            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Split error: ${e.message}")
            Result.failure(e)
        }
    }

    // ─────────────── COMPRESS PDF ───────────────
    suspend fun compressPdf(
        context: Context,
        inputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val document = PDDocument.load(inputFile)
            val outputName = "${inputFile.nameWithoutExtension}_compressed.pdf"
            val outputFile = File(getOutputDir(context), outputName)

            // Remove unused objects and compress
            document.isAllSecurityToBeRemoved = false
            document.save(outputFile)
            document.close()

            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Compress error: ${e.message}")
            Result.failure(e)
        }
    }

    // ─────────────── ADD WATERMARK ───────────────
    suspend fun addWatermark(
        context: Context,
        inputFile: File,
        watermarkText: String,
        opacity: Float = 0.3f
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val document = PDDocument.load(inputFile)
            val outputName = "${inputFile.nameWithoutExtension}_watermarked.pdf"
            val outputFile = File(getOutputDir(context), outputName)

            for (page in document.pages) {
                val contentStream = PDPageContentStream(
                    document, page,
                    PDPageContentStream.AppendMode.APPEND, true, true
                )
                contentStream.beginText()
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 60f)
                contentStream.setNonStrokingColor(0.8f, 0.8f, 0.8f)

                val mediaBox = page.mediaBox
                val tx = mediaBox.width / 2 - 120
                val ty = mediaBox.height / 2 - 30

                contentStream.newLineAtOffset(tx, ty)
                contentStream.showText(watermarkText)
                contentStream.endText()
                contentStream.close()
            }

            document.save(outputFile)
            document.close()
            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Watermark error: ${e.message}")
            Result.failure(e)
        }
    }

    // ─────────────── ENCRYPT PDF ───────────────
    suspend fun encryptPdf(
        context: Context,
        inputFile: File,
        userPassword: String,
        ownerPassword: String
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val document = PDDocument.load(inputFile)
            val outputName = "${inputFile.nameWithoutExtension}_encrypted.pdf"
            val outputFile = File(getOutputDir(context), outputName)

            val ap = AccessPermission()
            ap.setCanPrint(true)
            ap.setCanExtractContent(false)
            ap.setCanModify(false)

            val spp = StandardProtectionPolicy(ownerPassword, userPassword, ap)
            spp.encryptionKeyLength = 256

            document.protect(spp)
            document.save(outputFile)
            document.close()
            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Encrypt error: ${e.message}")
            Result.failure(e)
        }
    }

    // ─────────────── DECRYPT PDF ───────────────
    suspend fun decryptPdf(
        context: Context,
        inputFile: File,
        password: String
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val document = PDDocument.load(inputFile, password)
            val outputName = "${inputFile.nameWithoutExtension}_unlocked.pdf"
            val outputFile = File(getOutputDir(context), outputName)

            document.isAllSecurityToBeRemoved = true
            document.save(outputFile)
            document.close()
            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Decrypt error: ${e.message}")
            Result.failure(e)
        }
    }

    // ─────────────── ROTATE PAGES ───────────────
    suspend fun rotatePages(
        context: Context,
        inputFile: File,
        rotation: Int // 90, 180, 270
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val document = PDDocument.load(inputFile)
            val outputName = "${inputFile.nameWithoutExtension}_rotated.pdf"
            val outputFile = File(getOutputDir(context), outputName)

            for (page in document.pages) {
                val currentRotation = page.rotation
                page.rotation = (currentRotation + rotation) % 360
            }

            document.save(outputFile)
            document.close()
            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Rotate error: ${e.message}")
            Result.failure(e)
        }
    }

    // ─────────────── EXTRACT TEXT ───────────────
    suspend fun extractText(inputFile: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            val document = PDDocument.load(inputFile)
            val stripper = PDFTextStripper()
            val text = stripper.getText(document)
            document.close()
            Result.success(text)
        } catch (e: Exception) {
            Log.e(TAG, "Extract text error: ${e.message}")
            Result.failure(e)
        }
    }

    // ─────────────── ADD PAGE NUMBERS ───────────────
    suspend fun addPageNumbers(
        context: Context,
        inputFile: File,
        position: String = "bottom_center"
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val document = PDDocument.load(inputFile)
            val outputName = "${inputFile.nameWithoutExtension}_numbered.pdf"
            val outputFile = File(getOutputDir(context), outputName)

            var pageNum = 1
            val totalPages = document.numberOfPages

            for (page in document.pages) {
                val contentStream = PDPageContentStream(
                    document, page,
                    PDPageContentStream.AppendMode.APPEND, true, true
                )
                contentStream.beginText()
                contentStream.setFont(PDType1Font.HELVETICA, 10f)
                contentStream.setNonStrokingColor(0f, 0f, 0f)

                val mediaBox = page.mediaBox
                val text = "Halaman $pageNum dari $totalPages"
                val tx = when (position) {
                    "bottom_center" -> (mediaBox.width / 2) - 40
                    "bottom_left" -> 30f
                    "bottom_right" -> mediaBox.width - 80
                    else -> (mediaBox.width / 2) - 40
                }

                contentStream.newLineAtOffset(tx, 20f)
                contentStream.showText(text)
                contentStream.endText()
                contentStream.close()
                pageNum++
            }

            document.save(outputFile)
            document.close()
            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Page numbers error: ${e.message}")
            Result.failure(e)
        }
    }

    // ─────────────── CREATE PDF FROM TEXT ───────────────
    suspend fun createPdfFromText(
        context: Context,
        text: String,
        outputFileName: String,
        title: String = ""
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val document = PDDocument()
            val outputFile = File(getOutputDir(context), outputFileName)
            val font = PDType1Font.HELVETICA
            val fontSize = 12f
            val margin = 50f
            val lineHeight = fontSize * 1.5f

            var page = PDPage(PDRectangle.A4)
            document.addPage(page)
            var contentStream = PDPageContentStream(document, page)
            var yPosition = page.mediaBox.height - margin

            if (title.isNotEmpty()) {
                contentStream.beginText()
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18f)
                contentStream.newLineAtOffset(margin, yPosition)
                contentStream.showText(title)
                contentStream.endText()
                yPosition -= lineHeight * 2
            }

            val words = text.split(" ")
            val pageWidth = page.mediaBox.width - 2 * margin
            var currentLine = ""

            contentStream.beginText()
            contentStream.setFont(font, fontSize)
            contentStream.newLineAtOffset(margin, yPosition)

            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                val lineWidth = font.getStringWidth(testLine) / 1000 * fontSize

                if (lineWidth > pageWidth) {
                    contentStream.showText(currentLine)
                    contentStream.newLineAtOffset(0f, -lineHeight)
                    yPosition -= lineHeight
                    currentLine = word

                    if (yPosition < margin + lineHeight) {
                        contentStream.endText()
                        contentStream.close()

                        page = PDPage(PDRectangle.A4)
                        document.addPage(page)
                        contentStream = PDPageContentStream(document, page)
                        yPosition = page.mediaBox.height - margin
                        contentStream.beginText()
                        contentStream.setFont(font, fontSize)
                        contentStream.newLineAtOffset(margin, yPosition)
                    }
                } else {
                    currentLine = testLine
                }
            }

            if (currentLine.isNotEmpty()) {
                contentStream.showText(currentLine)
            }

            contentStream.endText()
            contentStream.close()

            document.save(outputFile)
            document.close()
            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Create PDF error: ${e.message}")
            Result.failure(e)
        }
    }

    // ─────────────── REORDER PAGES ───────────────
    suspend fun reorderPages(
        context: Context,
        inputFile: File,
        newOrder: List<Int>
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val sourceDoc = PDDocument.load(inputFile)
            val newDoc = PDDocument()
            val outputName = "${inputFile.nameWithoutExtension}_reordered.pdf"
            val outputFile = File(getOutputDir(context), outputName)

            for (pageIndex in newOrder) {
                if (pageIndex >= 0 && pageIndex < sourceDoc.numberOfPages) {
                    val page = sourceDoc.getPage(pageIndex)
                    newDoc.addPage(page)
                }
            }

            newDoc.save(outputFile)
            newDoc.close()
            sourceDoc.close()
            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Reorder error: ${e.message}")
            Result.failure(e)
        }
    }

    // ─────────────── GET PAGE COUNT ───────────────
    fun getPageCount(file: File): Int {
        return try {
            val document = PDDocument.load(file)
            val count = document.numberOfPages
            document.close()
            count
        } catch (e: Exception) {
            0
        }
    }

    // ─────────────── GET FILE SIZE FORMATTED ───────────────
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }

    // ─────────────── DELETE PAGE ───────────────
    suspend fun deletePage(
        context: Context,
        inputFile: File,
        pageIndex: Int
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val document = PDDocument.load(inputFile)
            val outputName = "${inputFile.nameWithoutExtension}_edited.pdf"
            val outputFile = File(getOutputDir(context), outputName)

            if (pageIndex >= 0 && pageIndex < document.numberOfPages) {
                document.removePage(pageIndex)
            }

            document.save(outputFile)
            document.close()
            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Delete page error: ${e.message}")
            Result.failure(e)
        }
    }
}
