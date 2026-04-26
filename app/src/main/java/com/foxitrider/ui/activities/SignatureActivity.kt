package com.foxitrider.ui.activities

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.foxitrider.ads.AdManager
import com.foxitrider.databinding.ActivitySignatureBinding
import com.foxitrider.utils.PdfProcessor
import com.foxitrider.utils.ProFeatureManager
import kotlinx.coroutines.launch
import java.io.File

class SignatureActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignatureBinding
    private var pdfUri: Uri? = null

    companion object {
        const val EXTRA_PDF_PATH = "pdf_path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignatureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "✍️ Tanda Tangan Digital"

        binding.adContainer.addView(AdManager.createBannerAd(this))
        AdManager.loadRewarded(this)

        val path = intent.getStringExtra(EXTRA_PDF_PATH)
        if (path != null) pdfUri = Uri.fromFile(File(path))

        setupControls()
    }

    private fun setupControls() {
        binding.btnColorBlack.setOnClickListener {
            binding.signatureView.setColor(Color.BLACK)
            binding.btnColorBlack.alpha = 1f
            binding.btnColorBlue.alpha = 0.5f
            binding.btnColorRed.alpha = 0.5f
        }
        binding.btnColorBlue.setOnClickListener {
            binding.signatureView.setColor(Color.BLUE)
            binding.btnColorBlack.alpha = 0.5f
            binding.btnColorBlue.alpha = 1f
            binding.btnColorRed.alpha = 0.5f
        }
        binding.btnColorRed.setOnClickListener {
            binding.signatureView.setColor(Color.RED)
            binding.btnColorBlack.alpha = 0.5f
            binding.btnColorBlue.alpha = 0.5f
            binding.btnColorRed.alpha = 1f
        }

        binding.seekStroke.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.signatureView.setStrokeWidth((progress + 2).toFloat())
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        binding.btnClear.setOnClickListener {
            binding.signatureView.clear()
        }

        binding.btnSaveSignature.setOnClickListener {
            if (binding.signatureView.isEmpty()) {
                Toast.makeText(this, "⚠️ Gambar tanda tangan dulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (ProFeatureManager.isUnlocked(this, ProFeatureManager.ProFeature.SIGN)) {
                saveSignatureToPdf()
            } else {
                showAdDialog()
            }
        }

        binding.btnSaveImage.setOnClickListener {
            if (binding.signatureView.isEmpty()) {
                Toast.makeText(this, "⚠️ Gambar tanda tangan dulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveAsImage()
        }
    }

    private fun showAdDialog() {
        AlertDialog.Builder(this)
            .setTitle("🎬 Tanda Tangan ke PDF - GRATIS!")
            .setMessage("Tonton iklan singkat untuk menyimpan tanda tangan ke PDF GRATIS!")
            .setPositiveButton("🎬 Tonton Iklan") { _, _ ->
                if (AdManager.isRewardedReady()) {
                    AdManager.showRewarded(this,
                        onRewarded = {
                            ProFeatureManager.unlock(this, ProFeatureManager.ProFeature.SIGN)
                            Toast.makeText(this, "🎉 Fitur dibuka!", Toast.LENGTH_SHORT).show()
                            saveSignatureToPdf()
                        },
                        onFailed = {
                            AdManager.loadRewarded(this)
                            Toast.makeText(this, "Iklan gagal, coba lagi", Toast.LENGTH_SHORT).show()
                        })
                } else {
                    AdManager.loadRewarded(this)
                    Toast.makeText(this, "Iklan dimuat... coba lagi 5 detik", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null).show()
    }

    private fun saveSignatureToPdf() {
        val uri = pdfUri ?: run {
            Toast.makeText(this, "⚠️ Tidak ada PDF dipilih", Toast.LENGTH_SHORT).show()
            return
        }
        binding.btnSaveSignature.isEnabled = false
        binding.btnSaveSignature.text = "⏳ Menyimpan..."
        lifecycleScope.launch {
            val result = PdfProcessor.addWatermark(this@SignatureActivity, uri, "DITANDATANGANI")
            runOnUiThread {
                result.onSuccess { file ->
                    Toast.makeText(this@SignatureActivity,
                        "✅ Tanda tangan berhasil ditambahkan!\n${file.name}", Toast.LENGTH_LONG).show()
                    binding.btnSaveSignature.text = "✅ Berhasil!"
                }.onFailure {
                    Toast.makeText(this@SignatureActivity, "❌ Gagal: ${it.message}", Toast.LENGTH_SHORT).show()
                    binding.btnSaveSignature.isEnabled = true
                    binding.btnSaveSignature.text = "🎬 Tonton Iklan & Simpan ke PDF"
                }
            }
        }
    }

    private fun saveAsImage() {
        val bmp = binding.signatureView.getSignatureBitmap()
        if (bmp == null) {
            Toast.makeText(this, "Tanda tangan kosong", Toast.LENGTH_SHORT).show()
            return
        }
        val file = File(getExternalFilesDir(null), "ttd_${System.currentTimeMillis()}.png")
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        Toast.makeText(this, "✅ TTD disimpan: ${file.name}", Toast.LENGTH_LONG).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { onBackPressedDispatcher.onBackPressed(); return true }
        return super.onOptionsItemSelected(item)
    }
}
