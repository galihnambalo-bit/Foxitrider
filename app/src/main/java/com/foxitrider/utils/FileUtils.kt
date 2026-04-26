package com.foxitrider.utils

import android.content.Context
import android.os.Environment
import java.io.File

object FileUtils {
    fun getOutputDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "FoxitRider")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun formatSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }

    fun getPdfPageCount(file: File): Int {
        return try {
            val content = file.readBytes().toString(Charsets.ISO_8859_1)
            val matches = Regex("/Type\\s*/Page[^s]").findAll(content).count()
            if (matches > 0) matches else 1
        } catch (e: Exception) { 1 }
    }
}
