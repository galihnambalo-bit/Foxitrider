package com.foxitrider.ui.activities

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.foxitrider.ads.AdManager
import com.foxitrider.databinding.ActivityProFeaturesBinding
import com.foxitrider.utils.ProFeatureManager

class ProFeaturesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProFeaturesBinding
    private var selectedFileUri: Uri? = null
    private var currentFeature = ProFeatureManager.ProFeature.MERGE

    companion object {
        const val EXTRA_FEATURE = "feature"
    }

    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            val name = uri.lastPathSegment ?: "file dipilih"
            Toast.makeText(this, "✅ File dipilih: $name", Toast.LENGTH_SHORT).show()
            binding.btnSelectFile.text = "✅ $name"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProFeaturesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.adContainer.addView(AdManager.createBannerAd(this))
        AdManager.loadRewarded(this)

        val featureKey = intent.getStringExtra(EXTRA_FEATURE) ?: "merge"
        currentFeature = ProFeatureManager.ProFeature.values()
            .find { it.key == featureKey } ?: ProFeatureManager.ProFeature.MERGE

        supportActionBar?.title = currentFeature.displayName
        binding.tvFeatureIcon.text = getIcon(currentFeature)
        binding.tvFeatureTitle.text = currentFeature.displayName
        binding.tvFeatureDesc.text = getDesc(currentFeature)

        binding.btnSelectFile.setOnClickListener {
            pickFileLauncher.launch("application/pdf")
        }

        binding.btnExecute.setOnClickListener {
            if (selectedFileUri == null) {
                Toast.makeText(this, "Pilih file PDF dulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (ProFeatureManager.isUnlocked(this, currentFeature)) {
                executeFeature()
            } else {
                showWatchAdDialog()
            }
        }
    }

    private fun showWatchAdDialog() {
        AlertDialog.Builder(this)
            .setTitle("🎬 Buka Fitur Pro GRATIS!")
            .setMessage(
                "Tonton iklan singkat (~30 detik) untuk membuka '${currentFeature.displayName}' secara GRATIS!\n\n" +
                "✅ Tidak perlu bayar\n✅ Akses penuh 24 jam\n✅ Dukung pengembang"
            )
            .setPositiveButton("🎬 Tonton Iklan") { _, _ ->
                if (AdManager.isRewardedReady()) {
                    AdManager.showRewarded(
                        this,
                        onRewarded = {
                            ProFeatureManager.unlock(this, currentFeature)
                            Toast.makeText(this, "🎉 Fitur '${currentFeature.displayName}' berhasil dibuka!", Toast.LENGTH_LONG).show()
                            executeFeature()
                        },
                        onFailed = {
                            Toast.makeText(this, "Iklan gagal tampil, coba lagi", Toast.LENGTH_SHORT).show()
                            AdManager.loadRewarded(this)
                        }
                    )
                } else {
                    AdManager.loadRewarded(this)
                    Toast.makeText(this, "Iklan sedang dimuat... Coba lagi dalam 5 detik", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun executeFeature() {
        Toast.makeText(this, "✅ ${currentFeature.displayName} sedang dijalankan!\nFile: ${selectedFileUri?.lastPathSegment}", Toast.LENGTH_LONG).show()
        binding.btnExecute.text = "✅ Selesai!"
        binding.btnExecute.isEnabled = false
    }

    private fun getIcon(f: ProFeatureManager.ProFeature) = when (f) {
        ProFeatureManager.ProFeature.MERGE -> "🔗"
        ProFeatureManager.ProFeature.SPLIT -> "✂️"
        ProFeatureManager.ProFeature.COMPRESS -> "📦"
        ProFeatureManager.ProFeature.WATERMARK -> "💧"
        ProFeatureManager.ProFeature.ENCRYPT -> "🔒"
        ProFeatureManager.ProFeature.CONVERT -> "🔄"
        ProFeatureManager.ProFeature.SIGN -> "✍️"
    }

    private fun getDesc(f: ProFeatureManager.ProFeature) = when (f) {
        ProFeatureManager.ProFeature.MERGE -> "Gabungkan beberapa PDF menjadi satu dokumen"
        ProFeatureManager.ProFeature.SPLIT -> "Pisahkan halaman PDF tertentu menjadi file baru"
        ProFeatureManager.ProFeature.COMPRESS -> "Kurangi ukuran file PDF tanpa mengurangi kualitas"
        ProFeatureManager.ProFeature.WATERMARK -> "Tambahkan tanda air teks ke semua halaman PDF"
        ProFeatureManager.ProFeature.ENCRYPT -> "Lindungi PDF dengan kata sandi yang kuat"
        ProFeatureManager.ProFeature.CONVERT -> "Konversi gambar (JPG/PNG) ke format PDF"
        ProFeatureManager.ProFeature.SIGN -> "Tambahkan tanda tangan digital ke dokumen PDF"
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
