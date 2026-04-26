package com.foxitrider.ui.activities

import android.app.AlertDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.foxitrider.R
import com.foxitrider.ads.AdManager
import com.foxitrider.databinding.ActivityPdfEditorBinding
import com.foxitrider.utils.ProFeatureManager
import java.io.File

class PdfEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfEditorBinding
    private var pdfFile: File? = null

    companion object {
        const val EXTRA_PDF_PATH = "pdf_path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupBannerAd()

        val pdfPath = intent.getStringExtra(EXTRA_PDF_PATH)
        if (pdfPath != null) {
            pdfFile = File(pdfPath)
            supportActionBar?.title = "Edit: ${pdfFile?.name}"
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

    private fun setupEditorTools() {
        binding.toolAnnotate.setOnClickListener {
            Toast.makeText(this, "Mode Anotasi aktif", Toast.LENGTH_SHORT).show()
        }
        binding.toolHighlight.setOnClickListener {
            Toast.makeText(this, "Mode Highlight aktif", Toast.LENGTH_SHORT).show()
        }
        binding.toolDraw.setOnClickListener {
            Toast.makeText(this, "Mode Gambar aktif", Toast.LENGTH_SHORT).show()
        }
        binding.toolText.setOnClickListener {
            showAddTextDialog()
        }
        binding.toolEraser.setOnClickListener {
            Toast.makeText(this, "Mode Hapus aktif", Toast.LENGTH_SHORT).show()
        }
        binding.toolRotate.setOnClickListener {
            checkProAndExecute(ProFeatureManager.ProFeature.CONVERT) {
                Toast.makeText(this, "Fitur putar halaman aktif", Toast.LENGTH_SHORT).show()
            }
        }
        binding.toolPageNumber.setOnClickListener {
            checkProAndExecute(ProFeatureManager.ProFeature.WATERMARK) {
                Toast.makeText(this, "Nomor halaman ditambahkan", Toast.LENGTH_SHORT).show()
            }
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
                    Toast.makeText(this, "Teks ditambahkan: $text", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun checkProAndExecute(feature: ProFeatureManager.ProFeature, action: () -> Unit) {
        if (ProFeatureManager.isFeatureUnlocked(this, feature)) {
            action()
        } else {
            AlertDialog.Builder(this)
                .setTitle("🎬 Fitur Pro")
                .setMessage("Tonton iklan singkat untuk membuka '${feature.displayName}' secara GRATIS!")
                .setPositiveButton("Tonton Iklan") { _, _ ->
                    if (AdManager.isRewardedAdReady()) {
                        AdManager.showRewardedAd(this,
                            onRewarded = {
                                ProFeatureManager.unlockFeature(this, feature)
                                Toast.makeText(this, "✅ Fitur berhasil dibuka!", Toast.LENGTH_SHORT).show()
                                action()
                            },
                            onAdFailed = {
                                Toast.makeText(this, "Iklan tidak tersedia", Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        AdManager.loadRewardedAd(this)
                        Toast.makeText(this, "Iklan sedang dimuat...", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_pdf_editor, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { onBackPressedDispatcher.onBackPressed(); true }
            R.id.action_save -> { Toast.makeText(this, "Disimpan", Toast.LENGTH_SHORT).show(); true }
            R.id.action_undo -> { Toast.makeText(this, "Dibatalkan", Toast.LENGTH_SHORT).show(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
