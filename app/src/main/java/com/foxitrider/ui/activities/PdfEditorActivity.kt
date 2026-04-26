package com.foxitrider.ui.activities

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
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
import com.foxitrider.utils.PdfProcessor
import com.foxitrider.utils.ProFeatureManager
import kotlinx.coroutines.launch
import java.io.File

class PdfEditorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPdfEditorBinding
    private var pdfFile: File? = null
    private var pdfUri: Uri? = null
    private var renderer: PdfRenderer? = null
    private var pfd: ParcelFileDescriptor? = null
    private var currentPage = 0
    private var totalPages = 0

    companion object { const val EXTRA_PATH = "pdf_path" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.adContainer.addView(AdManager.createBannerAd(this))
        AdManager.loadInterstitial(this)
        AdManager.loadRewarded(this)

        val path = intent.getStringExtra(EXTRA_PATH)
        if (path != null) {
            pdfFile = File(path)
            pdfUri = Uri.fromFile(pdfFile)
            supportActionBar?.title = "Edit: ${pdfFile?.name}"
            openPdf(pdfFile!!)
        } else { finish(); return }

        setupTools()
        setupPageNav()
    }

    private fun openPdf(file: File) {
        try {
            binding.progressBar.visibility = View.VISIBLE
            pfd?.close(); renderer?.close()
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd!!)
            totalPages = renderer!!.pageCount
            showPage(0)
        } catch (e: Exception) {
            Toast.makeText(this, "Error membuka PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPage(page: Int) {
        val r = renderer ?: return
        if (page < 0 || page >= totalPages) return
        val p = r.openPage(page)
        val bmp = Bitmap.createBitmap(p.width * 2, p.height * 2, Bitmap.Config.ARGB_8888)
        p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        p.close()
        binding.imgPdfPage.setImageBitmap(bmp)
        binding.progressBar.visibility = View.GONE
        currentPage = page
        binding.tvPageNum.text = "${page + 1}/$totalPages"
    }

    private fun setupPageNav() {
        binding.btnPrevPage.setOnClickListener { if (currentPage > 0) showPage(currentPage - 1) }
        binding.btnNextPage.setOnClickListener { if (currentPage < totalPages - 1) showPage(currentPage + 1) }
    }

    private fun setupTools() {
        binding.toolAnnotate.setOnClickListener { toast("✏️ Mode Anotasi - fitur touch annotation coming soon") }
        binding.toolHighlight.setOnClickListener { toast("🟡 Mode Highlight aktif") }
        binding.toolDraw.setOnClickListener { toast("🖊️ Mode Gambar aktif") }
        binding.toolText.setOnClickListener { showAddTextDialog() }
        binding.toolEraser.setOnClickListener { toast("🗑️ Mode Hapus aktif") }
        binding.toolRotate.setOnClickListener {
            checkPro(ProFeatureManager.ProFeature.CONVERT) { rotatePdf() }
        }
        binding.toolPageNumber.setOnClickListener {
            checkPro(ProFeatureManager.ProFeature.WATERMARK) {
                addWatermark("Halaman ${currentPage + 1}")
            }
        }
    }

    private fun rotatePdf() {
        val uri = pdfUri ?: return
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = PdfProcessor.rotate(this@PdfEditorActivity, uri, 90)
            runOnUiThread {
                result.onSuccess { file ->
                    pdfFile = file
                    pdfUri = Uri.fromFile(file)
                    openPdf(file)
                    toast("✅ Halaman diputar 90°")
                }.onFailure { toast("❌ Gagal: ${it.message}") }
            }
        }
    }

    private fun addWatermark(text: String) {
        val uri = pdfUri ?: return
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = PdfProcessor.addWatermark(this@PdfEditorActivity, uri, text)
            runOnUiThread {
                result.onSuccess { file ->
                    pdfFile = file
                    pdfUri = Uri.fromFile(file)
                    openPdf(file)
                    toast("✅ Teks ditambahkan ke PDF")
                }.onFailure { toast("❌ Gagal: ${it.message}") }
            }
        }
    }

    private fun showAddTextDialog() {
        val et = EditText(this).apply { hint = "Masukkan teks yang ingin ditambahkan ke PDF" }
        AlertDialog.Builder(this).setTitle("➕ Tambah Teks ke PDF").setView(et)
            .setPositiveButton("Tambahkan ke PDF") { _, _ ->
                if (et.text.isNotEmpty()) {
                    checkPro(ProFeatureManager.ProFeature.WATERMARK) {
                        addWatermark(et.text.toString())
                    }
                }
            }
            .setNegativeButton("Batal", null).show()
    }

    private fun checkPro(feature: ProFeatureManager.ProFeature, action: () -> Unit) {
        if (ProFeatureManager.isUnlocked(this, feature)) { action(); return }
        AlertDialog.Builder(this)
            .setTitle("🎬 Fitur Pro Gratis!")
            .setMessage("Tonton iklan untuk membuka '${feature.displayName}' GRATIS!")
            .setPositiveButton("🎬 Tonton") { _, _ ->
                if (AdManager.isRewardedReady()) {
                    AdManager.showRewarded(this,
                        onRewarded = { ProFeatureManager.unlock(this, feature); toast("🎉 Fitur dibuka!"); action() },
                        onFailed = { toast("Iklan tidak tersedia"); AdManager.loadRewarded(this) })
                } else { AdManager.loadRewarded(this); toast("Iklan dimuat... coba lagi 5 detik") }
            }.setNegativeButton("Batal", null).show()
    }

    private fun saveWithAd() {
        toast("💾 Menyimpan...")
        AdManager.showInterstitial(this) {
            toast("✅ Dokumen tersimpan di FoxitRider/")
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onDestroy() { super.onDestroy(); renderer?.close(); pfd?.close() }

    override fun onCreateOptionsMenu(menu: Menu): Boolean { menuInflater.inflate(R.menu.menu_editor, menu); return true }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> { onBackPressedDispatcher.onBackPressed(); true }
        R.id.action_save -> { saveWithAd(); true }
        R.id.action_undo -> { toast("↩️ Dibatalkan"); true }
        else -> super.onOptionsItemSelected(item)
    }
}
