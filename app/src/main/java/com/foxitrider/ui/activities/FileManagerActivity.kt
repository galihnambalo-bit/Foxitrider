package com.foxitrider.ui.activities

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.foxitrider.databinding.ActivityFileManagerBinding
import com.foxitrider.ui.adapters.FileAdapter
import java.io.File

class FileManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFileManagerBinding
    private lateinit var fileAdapter: FileAdapter
    private var currentDirectory: File = Environment.getExternalStorageDirectory()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFileManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "📂 Manajer File"

        setupRecyclerView()
        loadDirectory(currentDirectory)
        setupSearch()
    }

    private fun setupRecyclerView() {
        fileAdapter = FileAdapter(
            onFileClick = { file ->
                if (file.isDirectory) {
                    loadDirectory(file)
                } else if (file.extension.lowercase() == "pdf") {
                    val intent = Intent(this, PdfViewerActivity::class.java)
                    intent.putExtra(PdfViewerActivity.EXTRA_PDF_PATH, file.absolutePath)
                    startActivity(intent)
                }
            },
            onFileLongClick = { file ->
                showFileOptions(file)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@FileManagerActivity)
            adapter = fileAdapter
        }
    }

    private fun loadDirectory(directory: File) {
        currentDirectory = directory
        supportActionBar?.title = directory.name.ifEmpty { "Penyimpanan Utama" }
        binding.tvCurrentPath.text = directory.absolutePath

        val files = directory.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: emptyList()
        val filteredFiles = files.filter { it.isDirectory || it.extension.lowercase() == "pdf" }

        fileAdapter.submitList(filteredFiles)

        if (filteredFiles.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterFiles(newText ?: "")
                return true
            }
        })
    }

    private fun filterFiles(query: String) {
        val allFiles = currentDirectory.listFiles()
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
            ?.filter { it.isDirectory || it.extension.lowercase() == "pdf" }
            ?: emptyList()

        val filtered = if (query.isEmpty()) {
            allFiles
        } else {
            allFiles.filter { it.name.contains(query, ignoreCase = true) }
        }

        fileAdapter.submitList(filtered)
    }

    private fun showFileOptions(file: File) {
        val options = if (file.isDirectory) {
            arrayOf("Buka Folder")
        } else {
            arrayOf("Buka", "Bagikan", "Hapus", "Info")
        }

        android.app.AlertDialog.Builder(this)
            .setTitle(file.name)
            .setItems(options) { _, which ->
                when {
                    file.isDirectory -> loadDirectory(file)
                    which == 0 -> {
                        val intent = Intent(this, PdfViewerActivity::class.java)
                        intent.putExtra(PdfViewerActivity.EXTRA_PDF_PATH, file.absolutePath)
                        startActivity(intent)
                    }
                    which == 1 -> shareFile(file)
                    which == 2 -> deleteFile(file)
                    which == 3 -> showFileInfo(file)
                }
            }
            .show()
    }

    private fun shareFile(file: File) {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            this, "$packageName.fileprovider", file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Bagikan ${file.name}"))
    }

    private fun deleteFile(file: File) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Hapus File")
            .setMessage("Hapus '${file.name}'?")
            .setPositiveButton("Hapus") { _, _ ->
                if (file.delete()) {
                    loadDirectory(currentDirectory)
                    android.widget.Toast.makeText(this, "File dihapus", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showFileInfo(file: File) {
        val size = com.foxitrider.utils.PdfUtils.formatFileSize(file.length())
        val pageCount = com.foxitrider.utils.PdfUtils.getPageCount(file)
        val info = """
            📄 Nama: ${file.name}
            📁 Path: ${file.parent}
            📦 Ukuran: $size
            📃 Halaman: $pageCount
            📅 Diubah: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(file.lastModified()))}
        """.trimIndent()

        android.app.AlertDialog.Builder(this)
            .setTitle("Info File")
            .setMessage(info)
            .setPositiveButton("OK", null)
            .show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (currentDirectory != Environment.getExternalStorageDirectory()) {
            loadDirectory(currentDirectory.parentFile ?: Environment.getExternalStorageDirectory())
        } else {
            super.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
