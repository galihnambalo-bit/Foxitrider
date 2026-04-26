package com.foxitrider.ui.activities

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
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
import com.foxitrider.ui.views.DrawingTool
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
    private var renderer: android.graphics.pdf.PdfRenderer? = null
    private var pfd: ParcelFileDescriptor? = null
    private var currentPage = 0
    private var totalPages = 0
    private var currentBitmap: Bitmap? = null
    private var currentColor = Color.RED
    private var colorPickerVisible = false

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
        setupColors()
    }

    private fun openPdf(file: File) {
        try {
            binding.progressBar.visibility = View.VISIBLE
            pfd?.close(); renderer?.close()
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = android.graphics.pdf.PdfRenderer(pfd!!)
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
        p.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        p.close()
        currentBitmap = bmp
        binding.imgPdfPage.setImageBitmap(bmp)
        binding.progressBar.visibility = View.GONE
        binding.drawingView.clear()
        binding.overlayEditor.clearAll()
        currentPage = page
        binding.tvPageNum.text = "${page + 1}/$totalPages"
    }

    private fun setupColors() {
        binding.colorRed.setOnClickListener { setColor(Color.RED) }
        binding.colorBlue.setOnClickListener { setColor(Color.BLUE) }
        binding.colorBlack.setOnClickListener { setColor(Color.BLACK) }
        binding.colorGreen.setOnClickListener { setColor(Color.parseColor("#2E7D32")) }
        binding.colorYellow.setOnClickListener { setColor(Color.parseColor("#FDD835")) }
        binding.colorOrange.setOnClickListener { setColor(Color.parseColor("#FF6F00")) }
    }

    private fun setColor(color: Int) {
        currentColor = color
        binding.drawingView.setColor(color)
        binding.colorPickerRow.visibility = View.GONE
        colorPickerVisible = false
    }

    private fun setupTools() {
        binding.toolAnnotate.setOnClickListener {
            binding.drawingView.setTool(DrawingTool.PEN)
            binding.drawingView.setColor(currentColor)
            toggleColorPicker()
            updateToolIndicator("pen")
            toast("✏️ Mode Pen - pilih warna")
        }
        binding.toolHighlight.setOnClickListener {
            binding.drawingView.setTool(DrawingTool.HIGHLIGHTER)
            binding.drawingView.setColor(Color.parseColor("#FDD835"))
            updateToolIndicator("highlight")
            toast("🟡 Mode Highlight")
        }
        binding.toolText.setOnClickListener { showAddTextDialog() }
        binding.toolEraser.setOnClickListener {
            binding.drawingView.setTool(DrawingTool.ERASER)
            updateToolIndicator("eraser")
            toast("🗑️ Mode Hapus")
        }
        binding.toolDraw.setOnClickListener {
            binding.drawingView.undo()
            toast("↩️ Dibatalkan")
        }
        binding.toolRotate.setOnClickListener {
            binding.drawingView.redo()
            toast("↪️ Diulang")
        }
        binding.toolPageNumber.setOnClickListener {
            binding.drawingView.toggleVisibility()
            val msg = if (binding.drawingView.isDrawingVisible()) "👁️ Ditampilkan" else "🙈 Disembunyikan"
            toast(msg)
        }
    }

    private fun toggleColorPicker() {
        colorPickerVisible = !colorPickerVisible
        binding.colorPickerRow.visibility = if (colorPickerVisible) View.VISIBLE else View.GONE
    }

    private fun updateToolIndicator(tool: String) {
        binding.penIndicator.visibility = if (tool == "pen") View.VISIBLE else View.GONE
        binding.highlightIndicator.visibility = if (tool == "highlight") View.VISIBLE else View.GONE
    }

    private fun showAddTextDialog() {
        val et = EditText(this).apply { hint = "Ketik teks untuk ditimpa..." }
        AlertDialog.Builder(this)
            .setTitle("➕ Tambah Teks")
            .setMessage("Teks akan muncul di tengah.\nGeser ke posisi yang diinginkan.\nPinch 2 jari untuk ubah ukuran.\nTap 2x untuk edit/hapus.")
            .setView(et)
            .setPositiveButton("Tambahkan") { _, _ ->
                val text = et.text.toString()
                if (text.isNotEmpty()) {
                    val w = binding.overlayEditor.width.toFloat()
                    val h = binding.overlayEditor.height.toFloat()
                    binding.overlayEditor.addTextOverlay(text, w / 2 - 50, h / 2, 40f, currentColor)
                    toast("✅ Teks ditambahkan!\n👆 Drag geser | Pinch ubah ukuran | 2x tap edit")
                }
            }
            .setNegativeButton("Batal", null).show()
    }

    private fun saveWithMarkup() {
        if (binding.drawingView.isEmpty() && !binding.overlayEditor.hasOverlays()) {
            toast("⚠️ Belum ada markup untuk disimpan")
            return
        }
        binding.progressBar.visibility = View.VISIBLE
        toast("💾 Menyimpan...")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bmp = currentBitmap ?: return@launch
                val result = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(result)
                canvas.drawBitmap(bmp, 0f, 0f, null)

                // Draw markup layer scaled to PDF size
                val drawingBmp = binding.drawingView.getBitmap(
                    binding.drawingView.width, binding.drawingView.height
                )
                val scaleX = bmp.width.toFloat() / binding.drawingView.width.coerceAtLeast(1)
                val scaleY = bmp.height.toFloat() / binding.drawingView.height.coerceAtLeast(1)
                val matrix = Matrix().apply { setScale(scaleX, scaleY) }
                canvas.drawBitmap(drawingBmp, matrix, null)

                // Save as PDF
                val outputDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "FoxitRider"
                ).also { it.mkdirs() }
                val outputFile = File(outputDir, "edited_${System.currentTimeMillis()}.pdf")

                val pdfDoc = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(result.width / 2, result.height / 2, 1).create()
                val page = pdfDoc.startPage(pageInfo)
                val m = Matrix().apply { setScale(0.5f, 0.5f) }
                page.canvas.drawBitmap(result, m, null)
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
        R.id.action_save -> { saveWithMarkup(); true }
        R.id.action_undo -> { binding.drawingView.undo(); true }
        else -> super.onOptionsItemSelected(item)
    }
}
