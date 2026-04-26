package com.foxitrider.ui.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.foxitrider.R
import com.foxitrider.ads.AdManager
import com.foxitrider.databinding.ActivityPdfViewerBinding
import java.io.File

class PdfViewerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPdfViewerBinding
    private var renderer: PdfRenderer? = null
    private var pfd: ParcelFileDescriptor? = null
    private var currentPage = 0
    private var totalPages = 0
    private var pdfFile: File? = null

    companion object {
        const val EXTRA_PATH = "pdf_path"
        const val EXTRA_URI = "pdf_uri"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.adBannerContainer.addView(AdManager.createBannerAd(this))

        val path = intent.getStringExtra(EXTRA_PATH)
        val uriStr = intent.getStringExtra(EXTRA_URI) ?: intent.data?.toString()

        when {
            path != null -> openFile(File(path))
            uriStr != null -> copyAndOpen(Uri.parse(uriStr))
            else -> { Toast.makeText(this, "File tidak ditemukan", Toast.LENGTH_SHORT).show(); finish() }
        }

        binding.btnPrev.setOnClickListener { if (currentPage > 0) showPage(currentPage - 1) }
        binding.btnNext.setOnClickListener { if (currentPage < totalPages - 1) showPage(currentPage + 1) }
        binding.fabEdit.setOnClickListener {
            pdfFile?.let { f ->
                AdManager.showInterstitial(this) {
                    startActivity(Intent(this, PdfEditorActivity::class.java).apply {
                        putExtra(PdfEditorActivity.EXTRA_PATH, f.absolutePath)
                    })
                }
            }
        }
    }

    private fun copyAndOpen(uri: Uri) {
        try {
            val tmp = File(cacheDir, "view_${System.currentTimeMillis()}.pdf")
            contentResolver.openInputStream(uri)?.use { it.copyTo(tmp.outputStream()) }
            openFile(tmp)
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openFile(file: File) {
        try {
            pdfFile = file
            supportActionBar?.title = file.name
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd!!)
            totalPages = renderer!!.pageCount
            binding.progressBar.visibility = View.GONE
            showPage(0)
        } catch (e: Exception) {
            Toast.makeText(this, "Tidak bisa membuka PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPage(page: Int) {
        val r = renderer ?: return
        if (page < 0 || page >= totalPages) return
        val p = r.openPage(page)
        val bmp = Bitmap.createBitmap(p.width * 2, p.height * 2, Bitmap.Config.ARGB_8888)
        p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        p.close()
        binding.imgPage.setImageBitmap(bmp)
        currentPage = page
        binding.tvPage.text = "${page + 1} / $totalPages"
    }

    override fun onDestroy() {
        super.onDestroy()
        renderer?.close()
        pfd?.close()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_viewer, menu); return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> { onBackPressedDispatcher.onBackPressed(); true }
        R.id.action_share -> { sharePdf(); true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun sharePdf() {
        pdfFile?.let {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", it)
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Bagikan PDF"))
        }
    }
}
