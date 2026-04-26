package com.foxitrider.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File

object PdfUtils {

    private const val TAG = "PdfUtils"

    fun initialize(context: Context) {
        // Initialization placeholder
    }

    fun getOutputDir(context: Context): File {
        val dir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "FoxitRider"
        )
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getPageCount(file: File): Int {
        return try {
            // Basic PDF page count by reading binary
            val bytes = file.readBytes()
            val content = String(bytes, Charsets.ISO_8859_1)
            val count = content.split("/Page").size - 1
            if (count > 0) count else 1
        } catch (e: Exception) {
            1
        }
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }
}
