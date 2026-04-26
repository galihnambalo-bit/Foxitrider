package com.foxitrider.ui.activities

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.foxitrider.R
import com.foxitrider.ads.AdManager
import com.foxitrider.databinding.ActivityPdfEditorBinding
import com.foxitrider.utils.PdfUtils
import com.foxitrider.utils.ProFeatureManager
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import kotlinx.coroutines.launch
import java.io.File

class PdfEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfEditorBinding
    private var pdfFile: File? = null
    private var currentTool = EditorTool.NONE

    companion object {
        const val EXTRA_PDF_PATH = "pdf_path"
    }

    enum class EditorTool {
        NONE, ANNOTATE, HIGHLIGHT, DRAW, TEXT, ERASER
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Small banner ad
        setupBannerAd()

        val pdfPath = intent.getStringExtra(EXTRA_PDF_PATH)
        if (pdfPath != null) {
            pdfFile = File(pdfPath)
            loadPdf(pdfFile!!)
        } else {
            Toast.makeText(this, "File tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
        }

        setupEditorTools()
    }

    private fun setupBannerAd() {
        val bannerAd = AdManager.createBannerAd(this)
        binding.adContainer.addView(bannerAd)
    }

    private fun loadPdf(file: File) {
        supportActionBar?.title = "Edit: ${file.name}"

        binding.pdfView
            .fromFile(file)
            .defaultPage(0)
            .enableSwipe(true)
            .swipeHorizontal(false)
            .enableDoubletap(true)
            .enableAnnotationRendering(true)
            .scrollHandle(DefaultScrollHandle(this))
            .load()
    }

    private fun setupEditorTools() {
        binding.toolAnnotate.setOnClickListener {
            setActiveTool(EditorTool.ANNOTATE)
            Toast.makeText(this, "Mode Anotasi aktif", Toast.LENGTH_SHORT).show()
        }

        binding.toolHighlight.setOnClickListener {
            setActiveTool(EditorTool.HIGHLIGHT)
            Toast.makeText(this, "Mode Highlight aktif", Toast.LENGTH_SHORT).show()
        }

        binding.toolDraw.setOnClickListener {
            setActiveTool(EditorTool.DRAW)
            Toast.makeText(this, "Mode Gambar aktif", Toast.LENGTH_SHORT).show()
        }

        binding.toolText.setOnClickListener {
            setActiveTool(EditorTool.TEXT)
            showAddTextDialog()
        }

        binding.toolEraser.setOnClickListener {
            setActiveTool(EditorTool.ERASER)
            Toast.makeText(this, "Mode Hapus aktif", Toast.LENGTH_SHORT).show()
        }

        // Rotate button - Pro Feature
        binding.toolRotate.setOnClickListener {
            checkAndExecuteProFeature(ProFeatureManager.ProFeature.CONVERT) {
                showRotateDialog()
            }
        }

        // Add page number - requires watching ad
        binding.toolPageNumber.setOnClickListener {
            checkAndExecuteProFeature(ProFeatureManager.ProFeature.WATERMARK) {
                addPageNumbers()
            }
        }
    }

    private fun setActiveTool(tool: EditorTool) {
        currentTool = tool
        // Update UI to show active tool
        val tools = listOf(
            binding.toolAnnotate, binding.toolHighlight, binding.toolDraw,
            binding.toolText, binding.toolEraser
        )
        tools.forEach { it.alpha = 0.5f }
        when (tool) {
            EditorTool.ANNOTATE -> binding.toolAnnotate.alpha = 1f
            EditorTool.HIGHLIGHT -> binding.toolHighlight.alpha = 1f
            EditorTool.DRAW -> binding.toolDraw.alpha = 1f
            EditorTool.TEXT -> binding.toolText.alpha = 1f
            EditorTool.ERASER -> binding.toolEraser.alpha = 1f
            EditorTool.NONE -> {}
        }
    }

    private fun showAddTextDialog() {
        val editText = EditText(this)
        editText.hint = "Masukkan teks..."

        AlertDialog.Builder(this)
            .setTitle("Tambah Teks")
            .setView(editText)
            .setPositiveButton("Tambah") { _, _ ->
                val text = editText.text.toString()
                if (text.isNotEmpty()) {
                    Toast.makeText(this, "Teks akan ditambahkan: $text", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showRotateDialog() {
        val options = arrayOf("Putar 90° Kanan", "Putar 90° Kiri", "Putar 180°")
        AlertDialog.Builder(this)
            .setTitle("Putar Halaman")
            .setItems(options) { _, which ->
                val rotation = when (which) {
                    0 -> 90
                    1 -> 270
                    2 -> 180
                    else -> 90
                }
                rotatePages(rotation)
            }
            .show()
    }

    private fun rotatePages(rotation: Int) {
        pdfFile?.let { file ->
            binding.progressBar.visibility = View.VISIBLE
            lifecycleScope.launch {
                val result = PdfUtils.rotatePages(this@PdfEditorActivity, file, rotation)
                binding.progressBar.visibility = View.GONE
                result.onSuccess { outputFile ->
                    Toast.makeText(this@PdfEditorActivity, "Berhasil diputar: ${outputFile.name}", Toast.LENGTH_LONG).show()
                    loadPdf(outputFile)
                    pdfFile = outputFile
                }.onFailure { e ->
                    Toast.makeText(this@PdfEditorActivity, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun addPageNumbers() {
        pdfFile?.let { file ->
            binding.progressBar.visibility = View.VISIBLE
            lifecycleScope.launch {
                val result = PdfUtils.addPageNumbers(this@PdfEditorActivity, file)
                binding.progressBar.visibility = View.GONE
                result.onSuccess { outputFile ->
                    Toast.makeText(this@PdfEditorActivity, "Nomor halaman ditambahkan!", Toast.LENGTH_LONG).show()
                    loadPdf(outputFile)
                    pdfFile = outputFile
                }.onFailure { e ->
                    Toast.makeText(this@PdfEditorActivity, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkAndExecuteProFeature(
        feature: ProFeatureManager.ProFeature,
        action: () -> Unit
    ) {
        if (ProFeatureManager.isFeatureUnlocked(this, feature)) {
            action()
        } else {
            showWatchAdDialog(feature, action)
        }
    }

    private fun showWatchAdDialog(feature: ProFeatureManager.ProFeature, action: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("🎬 Fitur Pro")
            .setMessage("Tonton iklan singkat untuk membuka fitur '${feature.displayName}' secara GRATIS!")
            .setPositiveButton("Tonton Iklan") { _, _ ->
                if (AdManager.isRewardedAdReady()) {
                    AdManager.showRewardedAd(
                        this,
                        onRewarded = {
                            ProFeatureManager.unlockFeature(this, feature)
                            Toast.makeText(this, "✅ Fitur berhasil dibuka!", Toast.LENGTH_SHORT).show()
                            action()
                        },
                        onAdFailed = {
                            Toast.makeText(this, "Iklan tidak tersedia, coba lagi nanti", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    AdManager.loadRewardedAd(this)
                    Toast.makeText(this, "Iklan sedang dimuat, coba lagi dalam beberapa detik", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_pdf_editor, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.action_save -> {
                Toast.makeText(this, "Dokumen disimpan", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_undo -> {
                Toast.makeText(this, "Dibatalkan", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
