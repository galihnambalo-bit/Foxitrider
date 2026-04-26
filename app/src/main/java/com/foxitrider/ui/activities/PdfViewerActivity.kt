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
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.foxitrider.R
import com.foxitrider.ads.AdManager
import com.foxitrider.databinding.ActivityPdfViewerBinding
import java.io.File

class PdfViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfViewerBinding
    private var pdfFile: File? = null
    private var pdfRenderer: PdfRenderer? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private var currentPage = 0
    private var totalPages = 0

    companion object {
        const val EXTRA_PDF_PATH = "pdf_path"
        const val EXTRA_PDF_URI = "pdf_uri"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupBannerAd()

        val pdfPath = intent.getStringExtra(EXTRA_PDF_PATH)
        val pdfUri = intent.getStringExtra(EXTRA_PDF_URI)

        when {
            pdfPath != null -> {
                pdfFile = File(pdfPath)
                supportActionBar?.title = pdfFile?.name ?: "PDF Viewer"
                openPdfFile(pdfFile!!)
            }
            pdfUri != null -> {
                supportActionBar?.title = "Dokumen PDF"
                copyUriToFile(Uri.parse(pdfUri))
            }
            else -> {
                Toast.makeText(this, "File tidak ditemukan", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        setupControls()
    }

    private fun setupBannerAd() {
        val bannerAd = AdManager.createBannerAd(this)
        binding.adBannerContainer.addView(bannerAd)
    }

    private fun copyUriToFile(uri: Uri) {
        try {
            val tempFile = File(cacheDir, "temp_view.pdf")
            contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            pdfFile = tempFile
            openPdfFile(tempFile)
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openPdfFile(file: File) {
        try {
            parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(parcelFileDescriptor!!)
            totalPages = pdfRenderer!!.pageCount
            binding.progressLoading.visibility = View.GONE
            showPage(0)
            updatePageInfo()
        } catch (e: Exception) {
            Toast.makeText(this, "Error membuka PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPage(pageNum: Int) {
        if (pageNum < 0 || pageNum >= totalPages) return
        val renderer = pdfRenderer ?: return

        val page = renderer.openPage(pageNum)
        val bitmap = Bitmap.createBitmap(
            page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888
        )
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()

        binding.pdfView.setImageBitmap(bitmap)
        currentPage = pageNum
        updatePageInfo()
    }

    private fun updatePageInfo() {
        binding.tvPageInfo.text = "${currentPage + 1} / $totalPages"
    }

    private fun setupControls() {
        binding.fabEdit.setOnClickListener {
            pdfFile?.let { file ->
                AdManager.showInterstitialAd(this) {
                    val intent = Intent(this, PdfEditorActivity::class.java)
                    intent.putExtra(PdfEditorActivity.EXTRA_PDF_PATH, file.absolutePath)
                    startActivity(intent)
                }
            }
        }
        binding.btnPrevPage.setOnClickListener {
            if (currentPage > 0) showPage(currentPage - 1)
        }
        binding.btnNextPage.setOnClickListener {
            if (currentPage < totalPages - 1) showPage(currentPage + 1)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pdfRenderer?.close()
        parcelFileDescriptor?.close()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_pdf_viewer, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { onBackPressedDispatcher.onBackPressed(); true }
            R.id.action_share -> { sharePdf(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun sharePdf() {
        pdfFile?.let { file ->
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Bagikan PDF"))
        }
    }
}
