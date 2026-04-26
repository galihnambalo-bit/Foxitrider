package com.foxitrider.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.foxitrider.R
import com.foxitrider.ads.AdManager
import com.foxitrider.databinding.ActivityPdfViewerBinding
import com.rajat.pdfviewer.PdfViewerActivity as PdfLib
import java.io.File

class PdfViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfViewerBinding
    private var pdfFile: File? = null
    private var currentPage = 0
    private var totalPages = 1

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
                openWithPdfLib(null, pdfPath)
            }
            pdfUri != null -> {
                supportActionBar?.title = "Dokumen PDF"
                openWithPdfLib(pdfUri, null)
            }
        }

        setupControls()
    }

    private fun setupBannerAd() {
        val bannerAd = AdManager.createBannerAd(this)
        binding.adBannerContainer.addView(bannerAd)
    }

    private fun openWithPdfLib(uriStr: String?, path: String?) {
        try {
            val intent = if (path != null) {
                PdfLib.launchPdfFromPath(
                    this,
                    path,
                    pdfFile?.name ?: "PDF",
                    "FoxitRider",
                    enableDownload = false
                )
            } else {
                PdfLib.launchPdfFromUrl(
                    this,
                    uriStr ?: "",
                    "PDF",
                    "FoxitRider",
                    enableDownload = false
                )
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error membuka PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupControls() {
        binding.fabEdit.setOnClickListener {
            pdfFile?.let { file ->
                AdManager.showInterstitialAd(this) {
                    val intent = Intent(this, PdfEditorActivity::class.java)
                    intent.putExtra(PdfEditorActivity.EXTRA_PDF_PATH, file.absolutePath)
                    startActivity(intent)
                }
            } ?: Toast.makeText(this, "File tidak tersedia untuk diedit", Toast.LENGTH_SHORT).show()
        }

        binding.btnPrevPage.setOnClickListener { }
        binding.btnNextPage.setOnClickListener { }
        binding.tvPageInfo.text = "PDF"
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
