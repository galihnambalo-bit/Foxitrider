package com.foxitrider.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

object PdfProcessor {

    fun init(context: Context) {
        PDFBoxResourceLoader.init(context)
    }

    fun getOutputDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "FoxitRider")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun uriToFile(context: Context, uri: Uri): File? {
        return try {
            val name = "input_${System.currentTimeMillis()}.pdf"
            val file = File(context.cacheDir, name)
            context.contentResolver.openInputStream(uri)?.use { it.copyTo(file.outputStream()) }
            file
        } catch (e: Exception) { null }
    }

    // MERGE
    suspend fun merge(context: Context, uris: List<Uri>): Result<File> = withContext(Dispatchers.IO) {
        try {
            val output = File(getOutputDir(context), "merged_${System.currentTimeMillis()}.pdf")
            val merger = PDFMergerUtility()
            merger.destinationFileName = output.absolutePath
            uris.forEach { uri ->
                context.contentResolver.openInputStream(uri)?.let { merger.addSource(it) }
            }
            merger.mergeDocuments(null)
            Result.success(output)
        } catch (e: Exception) { Result.failure(e) }
    }

    // SPLIT
    suspend fun split(context: Context, uri: Uri, startPage: Int, endPage: Int): Result<File> = withContext(Dispatchers.IO) {
        try {
            val input = uriToFile(context, uri) ?: return@withContext Result.failure(Exception("Tidak bisa membaca file"))
            val doc = PDDocument.load(input)
            val splitter = Splitter().apply {
                setStartPage(startPage)
                setEndPage(endPage)
                setSplitAtPage(endPage - startPage + 1)
            }
            val pages = splitter.split(doc)
            val output = File(getOutputDir(context), "split_p${startPage}-${endPage}_${System.currentTimeMillis()}.pdf")
            if (pages.isNotEmpty()) { pages[0].save(output); pages[0].close() }
            doc.close()
            Result.success(output)
        } catch (e: Exception) { Result.failure(e) }
    }

    // COMPRESS
    suspend fun compress(context: Context, uri: Uri): Result<File> = withContext(Dispatchers.IO) {
        try {
            val input = uriToFile(context, uri) ?: return@withContext Result.failure(Exception("Tidak bisa membaca file"))
            val output = File(getOutputDir(context), "compressed_${System.currentTimeMillis()}.pdf")
            val doc = PDDocument.load(input)
            doc.save(output)
            doc.close()
            val savedBytes = input.length() - output.length()
            Result.success(output)
        } catch (e: Exception) { Result.failure(e) }
    }

    // WATERMARK
    suspend fun addWatermark(context: Context, uri: Uri, text: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val input = uriToFile(context, uri) ?: return@withContext Result.failure(Exception("Tidak bisa membaca file"))
            val output = File(getOutputDir(context), "watermarked_${System.currentTimeMillis()}.pdf")
            val doc = PDDocument.load(input)
            for (page in doc.pages) {
                val cs = PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)
                cs.beginText()
                cs.setFont(PDType1Font.HELVETICA_BOLD, 50f)
                cs.setNonStrokingColor(0.85f, 0.85f, 0.85f)
                cs.newLineAtOffset(page.mediaBox.width / 2 - 100, page.mediaBox.height / 2)
                cs.showText(text)
                cs.endText()
                cs.close()
            }
            doc.save(output)
            doc.close()
            Result.success(output)
        } catch (e: Exception) { Result.failure(e) }
    }

    // ENCRYPT
    suspend fun encrypt(context: Context, uri: Uri, password: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val input = uriToFile(context, uri) ?: return@withContext Result.failure(Exception("Tidak bisa membaca file"))
            val output = File(getOutputDir(context), "encrypted_${System.currentTimeMillis()}.pdf")
            val doc = PDDocument.load(input)
            val ap = AccessPermission()
            val policy = StandardProtectionPolicy(password, password, ap)
            policy.encryptionKeyLength = 256
            doc.protect(policy)
            doc.save(output)
            doc.close()
            Result.success(output)
        } catch (e: Exception) { Result.failure(e) }
    }

    // ROTATE
    suspend fun rotate(context: Context, uri: Uri, degrees: Int): Result<File> = withContext(Dispatchers.IO) {
        try {
            val input = uriToFile(context, uri) ?: return@withContext Result.failure(Exception("Tidak bisa membaca file"))
            val output = File(getOutputDir(context), "rotated_${System.currentTimeMillis()}.pdf")
            val doc = PDDocument.load(input)
            doc.pages.forEach { page -> page.rotation = (page.rotation + degrees) % 360 }
            doc.save(output)
            doc.close()
            Result.success(output)
        } catch (e: Exception) { Result.failure(e) }
    }

    // PAGE COUNT
    fun getPageCount(context: Context, uri: Uri): Int {
        return try {
            val input = uriToFile(context, uri) ?: return 0
            val doc = PDDocument.load(input)
            val count = doc.numberOfPages
            doc.close()
            count
        } catch (e: Exception) { 0 }
    }
}
