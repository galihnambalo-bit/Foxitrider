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

    companion object { const val EXTRA_PATH = "pdf_path" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.adContainer.addView(AdManager.createBannerAd(this))

        val path = intent.getStringExtra(EXTRA_PATH)
        if (path != null) {
            pdfFile = File(path)
            supportActionBar?.title = "Edit: ${pdfFile?.name}"
        } else { finish(); return }

        setupTools()
    }

    private fun setupTools() {
        binding.toolAnnotate.setOnClickListener { toast("Mode Anotasi aktif") }
        binding.toolHighlight.setOnClickListener { toast("Mode Highlight aktif") }
        binding.toolDraw.setOnClickListener { toast("Mode Gambar aktif") }
        binding.toolText.setOnClickListener { showTextDialog() }
        binding.toolEraser.setOnClickListener { toast("Mode Hapus aktif") }
        binding.toolRotate.setOnClickListener {
            checkPro(ProFeatureManager.ProFeature.CONVERT) { toast("Fitur Putar aktif") }
        }
        binding.toolPageNumber.setOnClickListener {
            checkPro(ProFeatureManager.ProFeature.WATERMARK) { toast("Nomor halaman ditambahkan") }
        }
    }

    private fun showTextDialog() {
        val et = EditText(this).apply { hint = "Masukkan teks..." }
        AlertDialog.Builder(this).setTitle("Tambah Teks").setView(et)
            .setPositiveButton("Tambah") { _, _ -> if (et.text.isNotEmpty()) toast("Teks: ${et.text}") }
            .setNegativeButton("Batal", null).show()
    }

    private fun checkPro(feature: ProFeatureManager.ProFeature, action: () -> Unit) {
        if (ProFeatureManager.isUnlocked(this, feature)) { action(); return }
        AlertDialog.Builder(this)
            .setTitle("🎬 Fitur Pro Gratis!")
            .setMessage("Tonton iklan singkat untuk membuka '${feature.displayName}'")
            .setPositiveButton("Tonton Iklan") { _, _ ->
                if (AdManager.isRewardedReady()) {
                    AdManager.showRewarded(this,
                        onRewarded = { ProFeatureManager.unlock(this, feature); action() },
                        onFailed = { toast("Iklan tidak tersedia") })
                } else { AdManager.loadRewarded(this); toast("Iklan dimuat...") }
            }
            .setNegativeButton("Batal", null).show()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu); return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> { onBackPressedDispatcher.onBackPressed(); true }
        R.id.action_save -> { toast("Disimpan"); true }
        R.id.action_undo -> { toast("Dibatalkan"); true }
        else -> super.onOptionsItemSelected(item)
    }
}
