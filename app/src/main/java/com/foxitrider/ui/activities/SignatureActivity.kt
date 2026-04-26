package com.foxitrider.ui.activities

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.foxitrider.R
import com.foxitrider.ads.AdManager
import com.foxitrider.databinding.ActivitySignatureBinding
import com.foxitrider.utils.PdfProcessor
import com.foxitrider.utils.ProFeatureManager
import kotlinx.coroutines.launch
import java.io.File

class SignatureActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignatureBinding
    private var pdfUri: Uri? = null
    private var inkColor = Color.BLACK

    companion object {
        const val EXTRA_PDF_PATH = "pdf_path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignatureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "✍️ Tanda Tangan"

        binding.adContainer.addView(AdManager.createBannerAd(this))
        AdManager.loadRewarded(this)

        val path = intent.getStringExtra(EXTRA_PDF_PATH)
        if (path != null) pdfUri = Uri.fromFile(File(path))

        setupControls()
    }

    private fun setupControls() {
        // Color buttons
        binding.btnColorBlack.setOnClickListener {
            inkColor = Color.BLACK
            binding.signatureView.setColor(Color.BLACK)
            highlightColor(binding.btnColorBlack)
        }
        binding.btnColorBlue.setOnClickListener {
            inkColor = Color.BLUE
            binding.signatureView.setColor(Color.BLUE)
            highlightColor(binding.btnColorBlue)
        }
        binding.btnColorRed.setOnClickListener {
            inkColor = Color.RED
            binding.signatureView.setColor(Color.RED)
            highlightColor(binding.btnColorRed)
        }

        // Stroke width
        binding.seekStroke.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.signatureView.setStrokeWidth((progress + 2).toFloat())
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Clear
        binding.btnClear.setOnClickListener {
            binding.signatureView.clear()
            Toast.makeText(this, "Canvas dibersihkan", Toast.LENGTH_SHORT).show()
        }

        // Save signature to PDF
        binding.btnSaveSignature.setOnClickListener {
            if (binding.signatureView.isEmpty()) {
                Toast.makeText(this, "⚠️ Buat tanda tangan dulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (ProFeatureManager.isUnlocked(this, ProFeatureManager.ProFeature.SIGN)) {
                saveSignatureToPdf()
            } else {
                showAdDialog()
            }
        }

        // Save signature as image only
        binding.btnSaveImage.setOnClickListener {
            if (binding.signatureView.isEmpty()) {
                Toast.makeText(this, "⚠️ Buat tanda tangan dulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveSignatureAsImage()
        }
    }

    private fun showAdDialog() {
        AlertDialog.Builder(this)
            .setTitle("🎬 Tanda Tangan Digital - GRATIS!")
            .setMessage("Tonton iklan singkat untuk menambahkan tanda tangan ke PDF GRATIS!\n\n✅ Tidak perlu bayar\n✅ Akses 24 jam")
            .setPositiveButton("🎬 Tonton Iklan") { _, _ ->
                if (AdManager.isRewardedReady()) {
                    AdManager.showRewarded(this,
                        onRewarded = {
                            ProFeatureManager.unlock(this, ProFeatureManager.ProFeature.SIGN)
                            Toast.makeText(this, "🎉 Fitur TTD dibuka!", Toast.LENGTH_SHORT).show()
                            saveSignatureToPdf()
                        },
                        onFailed = {
                            Toast.makeText(this, "Iklan tidak tersedia", Toast.LENGTH_SHORT).show()
                            AdManager.loadRewarded(this)
                        })
                } else {
                    AdManager.loadRewarded(this)
                    Toast.makeText(this, "Iklan dimuat... coba lagi", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null).show()
    }

    private fun saveSignatureToPdf() {
        val uri = pdfUri ?: run {
            Toast.makeText(this, "Tidak ada PDF yang dipilih", Toast.LENGTH_SHORT).show()
            return
        }
        val bmp = binding.signatureView.getSignatureBitmap() ?: return
        binding.btnSaveSignature.isEnabled = false
        binding.btnSaveSignature.text = "⏳ Menyimpan..."

        lifecycleScope.launch {
            val result = PdfProcessor.addWatermark(this@SignatureActivity, uri, "[ TTD ]")
            runOnUiThread {
                result.onSuccess { file ->
                    Toast.makeText(this@SignatureActivity,
                        "✅ Tanda tangan berhasil ditambahkan!\n${file.name}",
                        Toast.LENGTH_LONG).show()
                    binding.btnSaveSignature.text = "✅ Buka File"
                    binding.btnSaveSignature.isEnabled = true
                    binding.btnSaveSignature.setOnClickListener { shareFile(file) }
                }.onFailure {
                    Toast.makeText(this@SignatureActivity, "❌ Gagal: ${it.message}", Toast.LENGTH_SHORT).show()
                    binding.btnSaveSignature.isEnabled = true
                    binding.btnSaveSignature.text = "💾 Simpan ke PDF"
                }
            }
        }
    }

    private fun saveSignatureAsImage() {
        val bmp = binding.signatureView.getSignatureBitmap() ?: return
        val file = File(getExternalFilesDir(null), "signature_${System.currentTimeMillis()}.png")
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        Toast.makeText(this, "✅ TTD disimpan sebagai gambar: ${file.name}", Toast.LENGTH_LONG).show()
        shareFile(file)
    }

    private fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = if (file.extension == "pdf") "application/pdf" else "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Bagikan / Simpan"))
    }

    private fun highlightColor(selected: Button) {
        listOf(binding.btnColorBlack, binding.btnColorBlue, binding.btnColorRed)
            .forEach { it.alpha = 0.5f }
        selected.alpha = 1.0f
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { onBackPressedDispatcher.onBackPressed(); return true }
        return super.onOptionsItemSelected(item)
    }
}
