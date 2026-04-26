package com.foxitrider.ui.activities

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PointF
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.foxitrider.R
import com.foxitrider.ads.AdManager
import com.foxitrider.databinding.ActivityPdfEditorBinding
import com.foxitrider.utils.PdfProcessor
import com.foxitrider.utils.ProFeatureManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PdfEditorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPdfEditorBinding
    private var pdfFile: File? = null
    private var pdfUri: Uri? = null
    private var renderer: PdfRenderer? = null
    private var pfd: ParcelFileDescriptor? = null
    private var currentPage = 0
    private var totalPages = 0
    private var currentBitmap: Bitmap? = null
    private var currentFontSize = 40f
    private var currentTextColor = Color.BLACK

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
        setupOverlayEditor()
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
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPage(page: Int) {
        val r = renderer ?: return
        if (page < 0 || page >= totalPages) return
        val p = r.openPage(page)
        val bmp = Bitmap.createBitmap(p.width * 2, p.height * 2, Bitmap.Config.ARGB_8888)
        p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        p.close()
        currentBitmap = bmp
        binding.imgPdfPage.setImageBitmap(bmp)
        binding.progressBar.visibility = View.GONE
        binding.overlayEditor.clearAll()
        currentPage = page
        binding.tvPageNum.text = "${page + 1}/$totalPages"
    }

    private fun setupOverlayEditor() {
        binding.overlayEditor.setOnOverlayClickListener { overlay ->
            showEditOverlayDialog(overlay)
        }
    }

    private fun setupTools() {
        binding.toolAnnotate.setOnClickListener {
            showAddTextDialog()
        }
        binding.toolHighlight.setOnClickListener {
            // Add highlight overlay
            val w = binding.overlayEditor.width.toFloat()
            val h = binding.overlayEditor.height.toFloat()
            binding.overlayEditor.addTextOverlay(
                "████████████",
                w / 4, h / 2,
                30f
            )
            // Change last overlay color to yellow highlight
            binding.overlayEditor.overlays.lastOrNull()?.let {
                it.color = Color.parseColor("#FFFF00")
                it.bgColor = Color.parseColor("#FFFF00")
            }
            binding.overlayEditor.invalidate()
            toast("🟡 Highlight ditambahkan - geser ke posisi yang diinginkan")
        }
        binding.toolDraw.setOnClickListener {
            showAddTextDialog()
        }
        binding.toolText.setOnClickListener {
            showAddTextDialog()
        }
        binding.toolEraser.setOnClickListener {
            val selected = binding.overlayEditor.getSelectedOverlay()
            if (selected != null) {
                binding.overlayEditor.deleteSelected()
                toast("🗑️ Teks dihapus")
            } else {
                toast("Tap teks yang ingin dihapus dulu, lalu tekan hapus")
            }
        }
        binding.toolRotate.setOnClickListener {
            checkPro(ProFeatureManager.ProFeature.CONVERT) { rotatePdf() }
        }
        binding.toolPageNumber.setOnClickListener {
            checkPro(ProFeatureManager.ProFeature.WATERMARK) {
                addWatermarkToPdf("Halaman ${currentPage + 1}")
            }
        }
    }

    private fun showAddTextDialog() {
        val et = EditText(this).apply { hint = "Ketik teks untuk ditimpa di PDF..." }
        AlertDialog.Builder(this)
            .setTitle("➕ Tambah Teks ke PDF")
            .setMessage("Teks akan muncul di tengah halaman.\nGeser ke posisi tulisan yang ingin ditimpa.")
            .setView(et)
            .setPositiveButton("Tambahkan") { _, _ ->
                val text = et.text.toString()
                if (text.isNotEmpty()) {
                    val w = binding.overlayEditor.width.toFloat()
                    val h = binding.overlayEditor.height.toFloat()
                    binding.overlayEditor.addTextOverlay(text, w / 2 - 100, h / 2, currentFontSize)
                    binding.overlayEditor.overlays.lastOrNull()?.color = currentTextColor
                    toast("✅ Teks ditambahkan!\n👆 Drag untuk geser ke posisi yang diinginkan\n👇 Tap untuk edit/hapus")
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showEditOverlayDialog(overlay: com.foxitrider.ui.views.TextOverlay) {
        val options = arrayOf("✏️ Edit Teks", "🔠 Ubah Ukuran Font", "🎨 Ubah Warna", "🗑️ Hapus Teks ini")
        AlertDialog.Builder(this)
            .setTitle("Edit: \"${overlay.text}\"")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val et = EditText(this).apply { setText(overlay.text) }
                        AlertDialog.Builder(this).setTitle("Edit Teks")
                            .setView(et)
                            .setPositiveButton("Simpan") { _, _ ->
                                overlay.text = et.text.toString()
                                binding.overlayEditor.invalidate()
                            }.setNegativeButton("Batal", null).show()
                    }
                    1 -> {
                        val sizes = arrayOf("Kecil (24)", "Normal (40)", "Besar (56)", "Sangat Besar (72)")
                        val fontSizes = floatArrayOf(24f, 40f, 56f, 72f)
                        AlertDialog.Builder(this).setTitle("Ukuran Font")
                            .setItems(sizes) { _, i ->
                                overlay.fontSize = fontSizes[i]
                                binding.overlayEditor.invalidate()
                            }.show()
                    }
                    2 -> {
                        val colors = arrayOf("⚫ Hitam", "🔵 Biru", "🔴 Merah", "⬜ Putih")
                        val colorValues = intArrayOf(Color.BLACK, Color.BLUE, Color.RED, Color.WHITE)
                        AlertDialog.Builder(this).setTitle("Warna Teks")
                            .setItems(colors) { _, i ->
                                overlay.color = colorValues[i]
                                overlay.bgColor = if (colorValues[i] == Color.WHITE) Color.BLACK else Color.WHITE
                                binding.overlayEditor.invalidate()
                            }.show()
                    }
                    3 -> {
                        binding.overlayEditor.deleteSelected()
                        toast("🗑️ Teks dihapus")
                    }
                }
            }.show()
    }

    private fun rotatePdf() {
        val uri = pdfUri ?: return
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = PdfProcessor.rotate(this@PdfEditorActivity, uri, 90)
            runOnUiThread {
                result.onSuccess { file ->
                    pdfFile = file; pdfUri = Uri.fromFile(file)
                    openPdf(file); toast("✅ Halaman diputar 90°")
                }.onFailure { toast("❌ Gagal: ${it.message}") }
            }
        }
    }

    private fun addWatermarkToPdf(text: String) {
        val uri = pdfUri ?: return
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = PdfProcessor.addWatermark(this@PdfEditorActivity, uri, text)
            runOnUiThread {
                result.onSuccess { file ->
                    pdfFile = file; pdfUri = Uri.fromFile(file)
                    openPdf(file); toast("✅ Teks ditambahkan ke PDF")
                }.onFailure { toast("❌ Gagal: ${it.message}") }
            }
        }
    }

    private fun checkPro(feature: ProFeatureManager.ProFeature, action: () -> Unit) {
        if (ProFeatureManager.isUnlocked(this, feature)) { action(); return }
        AlertDialog.Builder(this)
            .setTitle("🎬 Fitur Pro Gratis!")
            .setMessage("Tonton iklan untuk membuka '${feature.displayName}'!")
            .setPositiveButton("🎬 Tonton") { _, _ ->
                if (AdManager.isRewardedReady()) {
                    AdManager.showRewarded(this,
                        onRewarded = { ProFeatureManager.unlock(this, feature); action() },
                        onFailed = { toast("Iklan tidak tersedia") })
                } else { AdManager.loadRewarded(this); toast("Iklan dimuat...") }
            }.setNegativeButton("Batal", null).show()
    }

    private fun saveWithOverlays() {
        if (!binding.overlayEditor.hasOverlays()) {
            toast("Tidak ada perubahan untuk disimpan")
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        toast("💾 Menyimpan dengan overlay teks...")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bmp = currentBitmap ?: return@launch
                val overlayBmp = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
                    bgPaint.color = overlay.bgColor

                    val bounds = android.graphics.Rect()
                    paint.getTextBounds(overlay.text, 0, overlay.text.length, bounds)
                    val padding = 10f
                    val bgRect = android.graphics.RectF(
                        point.x - padding,
                        point.y - bounds.height() - padding,
                        point.x + bounds.width() + padding,
                        point.y + padding
                    )
                    canvas.drawRect(bgRect, bgPaint)
                    canvas.drawText(overlay.text, point.x, point.y, paint)
                }

                // Save as new PDF
                val outputFile = File(
                    android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                    "FoxitRider"
                ).also { it.mkdirs() }.let {
                    File(it, "edited_${System.currentTimeMillis()}.pdf")
                }

                // Convert bitmap to PDF
                val pdfDoc = android.graphics.pdf.PdfDocument()
                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(
                    overlayBmp.width / 2, overlayBmp.height / 2, 1
                ).create()
                val page = pdfDoc.startPage(pageInfo)
                val matrix = android.graphics.Matrix()
                matrix.setScale(0.5f, 0.5f)
                page.canvas.drawBitmap(overlayBmp, matrix, null)
                pdfDoc.finishPage(page)
                outputFile.outputStream().use { pdfDoc.writeTo(it) }
                pdfDoc.close()

                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    toast("✅ Disimpan di Downloads/FoxitRider/\n${outputFile.name}")
                    AdManager.showInterstitial(this@PdfEditorActivity) {}
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    toast("❌ Gagal: ${e.message}")
                }
            }
        }
    }

    private fun setupPageNav() {
        binding.btnPrevPage.setOnClickListener { if (currentPage > 0) showPage(currentPage - 1) }
        binding.btnNextPage.setOnClickListener { if (currentPage < totalPages - 1) showPage(currentPage + 1) }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onDestroy() { super.onDestroy(); renderer?.close(); pfd?.close() }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu); return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> { onBackPressedDispatcher.onBackPressed(); true }
        R.id.action_save -> { saveWithOverlays(); true }
        R.id.action_undo -> {
            binding.overlayEditor.deleteSelected()
            toast("↩️ Dihapus")
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
