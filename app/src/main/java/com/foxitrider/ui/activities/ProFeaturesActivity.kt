package com.foxitrider.ui.activities

import android.app.AlertDialog
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.foxitrider.R
import com.foxitrider.ads.AdManager
import com.foxitrider.databinding.ActivityProFeaturesBinding
import com.foxitrider.utils.PdfUtils
import com.foxitrider.utils.ProFeatureManager
import kotlinx.coroutines.launch
import java.io.File

class ProFeaturesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProFeaturesBinding
    private var selectedFiles = mutableListOf<File>()
    private var currentFeature = ""

    companion object {
        const val EXTRA_FEATURE = "feature"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProFeaturesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        currentFeature = intent.getStringExtra(EXTRA_FEATURE) ?: ""

        setupBannerAd()
        setupFeatureUI()
        setupActionButton()

        // Preload rewarded ad
        AdManager.loadRewardedAd(this)
    }

    private fun setupBannerAd() {
        val bannerAd = AdManager.createBannerAd(this)
        binding.adContainer.addView(bannerAd)
    }

    private fun setupFeatureUI() {
        when (currentFeature) {
            "merge" -> {
                supportActionBar?.title = "🔗 Gabungkan PDF"
                binding.tvFeatureTitle.text = "Gabungkan PDF"
                binding.tvFeatureDesc.text = "Gabungkan beberapa file PDF menjadi satu dokumen"
                binding.tvFeatureIcon.text = "🔗"
                binding.btnSelectFiles.visibility = View.VISIBLE
                binding.btnSelectFiles.text = "Pilih File PDF"
                binding.tvProBadge.text = "✨ FITUR PRO - GRATIS dengan Iklan"
            }
            "split" -> {
                supportActionBar?.title = "✂️ Pisahkan PDF"
                binding.tvFeatureTitle.text = "Pisahkan PDF"
                binding.tvFeatureDesc.text = "Pisahkan PDF menjadi beberapa bagian berdasarkan halaman"
                binding.tvFeatureIcon.text = "✂️"
                binding.btnSelectFiles.visibility = View.VISIBLE
                binding.btnSelectFiles.text = "Pilih File PDF"
                binding.tvProBadge.text = "✨ FITUR PRO - GRATIS dengan Iklan"
            }
            "compress" -> {
                supportActionBar?.title = "📦 Kompres PDF"
                binding.tvFeatureTitle.text = "Kompres PDF"
                binding.tvFeatureDesc.text = "Kurangi ukuran file PDF tanpa mengurangi kualitas secara signifikan"
                binding.tvFeatureIcon.text = "📦"
                binding.btnSelectFiles.visibility = View.VISIBLE
                binding.btnSelectFiles.text = "Pilih File PDF"
                binding.tvProBadge.text = "✨ FITUR PRO - GRATIS dengan Iklan"
            }
            "watermark" -> {
                supportActionBar?.title = "💧 Watermark"
                binding.tvFeatureTitle.text = "Tambah Watermark"
                binding.tvFeatureDesc.text = "Tambahkan teks watermark ke semua halaman PDF"
                binding.tvFeatureIcon.text = "💧"
                binding.btnSelectFiles.visibility = View.VISIBLE
                binding.btnSelectFiles.text = "Pilih File PDF"
                binding.etWatermarkText.visibility = View.VISIBLE
                binding.tvProBadge.text = "✨ FITUR PRO - GRATIS dengan Iklan"
            }
            "encrypt" -> {
                supportActionBar?.title = "🔒 Enkripsi PDF"
                binding.tvFeatureTitle.text = "Enkripsi PDF"
                binding.tvFeatureDesc.text = "Lindungi PDF dengan kata sandi yang kuat"
                binding.tvFeatureIcon.text = "🔒"
                binding.btnSelectFiles.visibility = View.VISIBLE
                binding.btnSelectFiles.text = "Pilih File PDF"
                binding.layoutPassword.visibility = View.VISIBLE
                binding.tvProBadge.text = "✨ FITUR PRO - GRATIS dengan Iklan"
            }
            "convert" -> {
                supportActionBar?.title = "🔄 Konversi"
                binding.tvFeatureTitle.text = "Konversi File"
                binding.tvFeatureDesc.text = "Konversi gambar ke PDF atau ekstrak teks dari PDF"
                binding.tvFeatureIcon.text = "🔄"
                binding.btnSelectFiles.visibility = View.VISIBLE
                binding.btnSelectFiles.text = "Pilih File"
                binding.tvProBadge.text = "✨ FITUR PRO - GRATIS dengan Iklan"
            }
            else -> {
                showAllFeatures()
            }
        }
    }

    private fun showAllFeatures() {
        supportActionBar?.title = "Fitur Pro"
        binding.allFeaturesContainer.visibility = View.VISIBLE
        binding.singleFeatureContainer.visibility = View.GONE
    }

    private fun setupActionButton() {
        binding.btnSelectFiles.setOnClickListener {
            // TODO: Open file picker for multiple files
            Toast.makeText(this, "Pilih file PDF dari penyimpanan", Toast.LENGTH_SHORT).show()
        }

        binding.btnExecuteFeature.setOnClickListener {
            checkProAndExecute()
        }
    }

    private fun checkProAndExecute() {
        val feature = when (currentFeature) {
            "merge" -> ProFeatureManager.ProFeature.MERGE
            "split" -> ProFeatureManager.ProFeature.SPLIT
            "compress" -> ProFeatureManager.ProFeature.COMPRESS
            "watermark" -> ProFeatureManager.ProFeature.WATERMARK
            "encrypt" -> ProFeatureManager.ProFeature.ENCRYPT
            "convert" -> ProFeatureManager.ProFeature.CONVERT
            else -> null
        }

        feature?.let {
            if (ProFeatureManager.isFeatureUnlocked(this, it)) {
                executeFeature()
            } else {
                showWatchAdDialog(it)
            }
        }
    }

    private fun showWatchAdDialog(feature: ProFeatureManager.ProFeature) {
        AlertDialog.Builder(this)
            .setTitle("🎬 Buka Fitur Pro GRATIS!")
            .setMessage(
                "Tonton iklan singkat (sekitar 30 detik) untuk membuka '${feature.displayName}' secara GRATIS!\n\n" +
                "✅ Tidak perlu bayar\n✅ Akses penuh selama 24 jam\n✅ Dukung pengembang aplikasi"
            )
            .setPositiveButton("🎬 Tonton Iklan Sekarang") { _, _ ->
                if (AdManager.isRewardedAdReady()) {
                    AdManager.showRewardedAd(
                        this,
                        onRewarded = {
                            ProFeatureManager.unlockFeature(this, feature)
                            Toast.makeText(this, "🎉 Fitur '${feature.displayName}' berhasil dibuka!", Toast.LENGTH_LONG).show()
                            executeFeature()
                        },
                        onAdFailed = {
                            Toast.makeText(this, "Iklan gagal dimuat. Coba lagi nanti.", Toast.LENGTH_SHORT).show()
                            AdManager.loadRewardedAd(this)
                        }
                    )
                } else {
                    AdManager.loadRewardedAd(this)
                    Toast.makeText(this, "Iklan sedang dimuat... Coba lagi dalam beberapa detik", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Batal", null)
            .setIcon(R.drawable.ic_star)
            .show()
    }

    private fun executeFeature() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnExecuteFeature.isEnabled = false

        when (currentFeature) {
            "merge" -> executeMerge()
            "split" -> executeSplit()
            "compress" -> executeCompress()
            "watermark" -> executeWatermark()
            "encrypt" -> executeEncrypt()
            "convert" -> executeConvert()
        }
    }

    private fun executeMerge() {
        if (selectedFiles.size < 2) {
            showError("Pilih minimal 2 file PDF untuk digabungkan")
            return
        }
        lifecycleScope.launch {
            val result = PdfUtils.mergePdfs(
                this@ProFeaturesActivity,
                selectedFiles,
                "merged_${System.currentTimeMillis()}.pdf"
            )
            handleResult(result, "PDF berhasil digabungkan!")
        }
    }

    private fun executeSplit() {
        if (selectedFiles.isEmpty()) {
            showError("Pilih file PDF terlebih dahulu")
            return
        }
        showSplitDialog(selectedFiles[0])
    }

    private fun showSplitDialog(file: File) {
        val layout = layoutInflater.inflate(R.layout.dialog_split_pdf, null)
        AlertDialog.Builder(this)
            .setTitle("Pisahkan PDF")
            .setView(layout)
            .setPositiveButton("Pisahkan") { dialog, _ ->
                val startPage = layout.findViewById<EditText>(R.id.etStartPage).text.toString().toIntOrNull() ?: 1
                val endPage = layout.findViewById<EditText>(R.id.etEndPage).text.toString().toIntOrNull() ?: PdfUtils.getPageCount(file)

                lifecycleScope.launch {
                    val result = PdfUtils.splitPdf(
                        this@ProFeaturesActivity, file, startPage, endPage
                    )
                    handleResult(result, "PDF berhasil dipisahkan!")
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun executeCompress() {
        if (selectedFiles.isEmpty()) {
            showError("Pilih file PDF terlebih dahulu")
            return
        }
        lifecycleScope.launch {
            val result = PdfUtils.compressPdf(this@ProFeaturesActivity, selectedFiles[0])
            handleResult(result, "PDF berhasil dikompres!")
        }
    }

    private fun executeWatermark() {
        if (selectedFiles.isEmpty()) {
            showError("Pilih file PDF terlebih dahulu")
            return
        }
        val watermarkText = binding.etWatermarkText.text.toString()
        if (watermarkText.isEmpty()) {
            showError("Masukkan teks watermark")
            return
        }
        lifecycleScope.launch {
            val result = PdfUtils.addWatermark(this@ProFeaturesActivity, selectedFiles[0], watermarkText)
            handleResult(result, "Watermark berhasil ditambahkan!")
        }
    }

    private fun executeEncrypt() {
        if (selectedFiles.isEmpty()) {
            showError("Pilih file PDF terlebih dahulu")
            return
        }
        val userPass = binding.etUserPassword.text.toString()
        val ownerPass = binding.etOwnerPassword.text.toString()
        if (userPass.isEmpty()) {
            showError("Masukkan kata sandi pengguna")
            return
        }
        lifecycleScope.launch {
            val result = PdfUtils.encryptPdf(
                this@ProFeaturesActivity, selectedFiles[0], userPass,
                ownerPass.ifEmpty { userPass }
            )
            handleResult(result, "PDF berhasil dienkripsi!")
        }
    }

    private fun executeConvert() {
        Toast.makeText(this, "Konversi sedang diproses...", Toast.LENGTH_SHORT).show()
        binding.progressBar.visibility = View.GONE
        binding.btnExecuteFeature.isEnabled = true
    }

    private fun handleResult(result: Result<File>, successMsg: String) {
        runOnUiThread {
            binding.progressBar.visibility = View.GONE
            binding.btnExecuteFeature.isEnabled = true
            result.onSuccess { file ->
                Toast.makeText(this, "$successMsg\nDisimpan: ${file.name}", Toast.LENGTH_LONG).show()
            }.onFailure { e ->
                Toast.makeText(this, "Gagal: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showError(msg: String) {
        binding.progressBar.visibility = View.GONE
        binding.btnExecuteFeature.isEnabled = true
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
