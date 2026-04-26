package com.foxitrider.ui.activities

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.foxitrider.databinding.ActivityFileManagerBinding
import com.foxitrider.ui.adapters.FileAdapter
import com.foxitrider.utils.FileUtils
import java.io.File

class FileManagerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFileManagerBinding
    private lateinit var adapter: FileAdapter
    private var currentDir = Environment.getExternalStorageDirectory()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFileManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "📂 File Manager"

        adapter = FileAdapter(
            onFileClick = { file ->
                if (file.isDirectory) loadDir(file)
                else if (file.extension.lowercase() == "pdf") {
                    startActivity(Intent(this, PdfViewerActivity::class.java).apply {
                        putExtra(PdfViewerActivity.EXTRA_PATH, file.absolutePath)
                    })
                }
            },
            onFileLongClick = { file -> showOptions(file) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        loadDir(currentDir)
    }

    private fun loadDir(dir: File) {
        currentDir = dir
        supportActionBar?.title = dir.name.ifEmpty { "Storage" }
        binding.tvPath.text = dir.absolutePath
        val files = dir.listFiles()
            ?.filter { it.isDirectory || it.extension.lowercase() == "pdf" }
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
            ?: emptyList()
        adapter.submitList(files)
        binding.tvEmpty.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (files.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showOptions(file: File) {
        val items = if (file.isDirectory) arrayOf("Buka") else arrayOf("Buka", "Info", "Hapus")
        android.app.AlertDialog.Builder(this).setTitle(file.name).setItems(items) { _, i ->
            when {
                file.isDirectory -> loadDir(file)
                i == 0 -> startActivity(Intent(this, PdfViewerActivity::class.java).apply {
                    putExtra(PdfViewerActivity.EXTRA_PATH, file.absolutePath)
                })
                i == 1 -> showInfo(file)
                i == 2 -> confirmDelete(file)
            }
        }.show()
    }

    private fun showInfo(file: File) {
        android.app.AlertDialog.Builder(this).setTitle("Info File")
            .setMessage("Nama: ${file.name}\nUkuran: ${FileUtils.formatSize(file.length())}\nPath: ${file.parent}")
            .setPositiveButton("OK", null).show()
    }

    private fun confirmDelete(file: File) {
        android.app.AlertDialog.Builder(this).setTitle("Hapus?")
            .setMessage("Hapus '${file.name}'?")
            .setPositiveButton("Hapus") { _, _ ->
                if (file.delete()) { loadDir(currentDir); Toast.makeText(this, "Dihapus", Toast.LENGTH_SHORT).show() }
            }.setNegativeButton("Batal", null).show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val parent = currentDir.parentFile
        if (parent != null && currentDir != Environment.getExternalStorageDirectory()) loadDir(parent)
        else super.onBackPressed()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { onBackPressedDispatcher.onBackPressed(); return true }
        return super.onOptionsItemSelected(item)
    }
}
