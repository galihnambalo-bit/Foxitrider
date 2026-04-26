package com.foxitrider.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.foxitrider.R
import com.foxitrider.ads.AdManager
import com.foxitrider.databinding.ActivityPdfViewerBinding
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import java.io.File

class PdfViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfViewerBinding
    private var currentPage = 0
    private var totalPages = 0
    private var pdfFile: File? = null

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

        // Small banner ad at top (non-intrusive)
        setupBannerAd()

        loadPdf()
        setupControls()
    }

    private fun setupBannerAd() {
        val bannerAd = AdManager.createBannerAd(this)
        binding.adBannerContainer.addView(bannerAd)
    }

    private fun loadPdf() {
        val pdfPath = intent.getStringExtra(EXTRA_PDF_PATH)
        val pdfUri = intent.getStringExtra(EXTRA_PDF_URI)

        when {
            pdfPath != null -> {
                pdfFile = File(pdfPath)
                loadPdfFromFile(pdfFile!!)
            }
            pdfUri != null -> {
                val uri = Uri.parse(pdfUri)
                loadPdfFromUri(uri)
            }
        }
    }

    private fun loadPdfFromFile(file: File) {
        supportActionBar?.title = file.name
        binding.progressLoading.visibility = View.VISIBLE

        binding.pdfView
            .fromFile(file)
            .defaultPage(0)
            .onLoad(object : OnLoadCompleteListener {
                override fun loadComplete(nbPages: Int) {
                    totalPages = nbPages
                    binding.progressLoading.visibility = View.GONE
                    updatePageInfo()
                }
            })
            .onPageChange(object : OnPageChangeListener {
                override fun onPageChanged(page: Int, pageCount: Int) {
                    currentPage = page
                    updatePageInfo()
                }
            })
            .onError { t ->
                binding.progressLoading.visibility = View.GONE
                Toast.makeText(this, "Error membuka PDF: ${t.message}", Toast.LENGTH_SHORT).show()
            }
            .onPageError(object : OnPageErrorListener {
                override fun onPageError(page: Int, t: Throwable?) {
                    Toast.makeText(this@PdfViewerActivity, "Error pada halaman $page", Toast.LENGTH_SHORT).show()
                }
            })
            .scrollHandle(DefaultScrollHandle(this))
            .enableSwipe(true)
            .swipeHorizontal(false)
            .enableDoubletap(true)
            .enableAnnotationRendering(true)
            .load()
    }

    private fun loadPdfFromUri(uri: Uri) {
        supportActionBar?.title = "Dokumen PDF"
        binding.progressLoading.visibility = View.VISIBLE

        binding.pdfView
            .fromUri(uri)
            .defaultPage(0)
            .onLoad(object : OnLoadCompleteListener {
                override fun loadComplete(nbPages: Int) {
                    totalPages = nbPages
                    binding.progressLoading.visibility = View.GONE
                    updatePageInfo()
                }
            })
            .onPageChange(object : OnPageChangeListener {
                override fun onPageChanged(page: Int, pageCount: Int) {
                    currentPage = page
                    updatePageInfo()
                }
            })
            .onError { t ->
                binding.progressLoading.visibility = View.GONE
                Toast.makeText(this, "Error membuka PDF: ${t.message}", Toast.LENGTH_SHORT).show()
            }
            .scrollHandle(DefaultScrollHandle(this))
            .enableSwipe(true)
            .swipeHorizontal(false)
            .enableDoubletap(true)
            .load()
    }

    private fun setupControls() {
        binding.fabEdit.setOnClickListener {
            pdfFile?.let { file ->
                // Show interstitial before editing
                AdManager.showInterstitialAd(this) {
                    val intent = Intent(this, PdfEditorActivity::class.java)
                    intent.putExtra(PdfEditorActivity.EXTRA_PDF_PATH, file.absolutePath)
                    startActivity(intent)
                }
            } ?: Toast.makeText(this, "File tidak tersedia untuk diedit", Toast.LENGTH_SHORT).show()
        }

        binding.btnPrevPage.setOnClickListener {
            if (currentPage > 0) {
                binding.pdfView.jumpTo(currentPage - 1, true)
            }
        }

        binding.btnNextPage.setOnClickListener {
            if (currentPage < totalPages - 1) {
                binding.pdfView.jumpTo(currentPage + 1, true)
            }
        }
    }

    private fun updatePageInfo() {
        binding.tvPageInfo.text = "${currentPage + 1} / $totalPages"
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_pdf_viewer, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.action_share -> {
                sharePdf()
                true
            }
            R.id.action_edit -> {
                pdfFile?.let { file ->
                    AdManager.showInterstitialAd(this) {
                        val intent = Intent(this, PdfEditorActivity::class.java)
                        intent.putExtra(PdfEditorActivity.EXTRA_PDF_PATH, file.absolutePath)
                        startActivity(intent)
                    }
                }
                true
            }
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
