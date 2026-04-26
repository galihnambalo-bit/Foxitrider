package com.foxitrider.ui.activities

import android.app.AlertDialog
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.foxitrider.ads.AdManager
import com.foxitrider.databinding.ActivityProFeaturesBinding
import com.foxitrider.utils.ProFeatureManager

class ProFeaturesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProFeaturesBinding

    companion object { const val EXTRA_FEATURE = "feature" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProFeaturesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.adContainer.addView(AdManager.createBannerAd(this))
        AdManager.loadRewarded(this)

        val featureKey = intent.getStringExtra(EXTRA_FEATURE) ?: ""
        val feature = ProFeatureManager.ProFeature.values().find { it.key == featureKey }
            ?: ProFeatureManager.ProFeature.MERGE

        supportActionBar?.title = feature.displayName
        binding.tvFeatureIcon.text = getIcon(feature)
        binding.tvFeatureTitle.text = feature.displayName
        binding.tvFeatureDesc.text = getDesc(feature)

        binding.btnExecute.setOnClickListener {
            if (ProFeatureManager.isUnlocked(this, feature)) {
                execute(feature)
            } else {
                showWatchAdDialog(feature)
            }
        }
        binding.btnSelectFile.setOnClickListener {
            Toast.makeText(this, "Pilih file PDF dari penyimpanan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showWatchAdDialog(feature: ProFeatureManager.ProFeature) {
        AlertDialog.Builder(this)
            .setTitle("🎬 Buka Fitur Pro GRATIS!")
            .setMessage("Tonton iklan singkat untuk membuka '${feature.displayName}' secara GRATIS!\n\n✅ Tidak perlu bayar\n✅ Akses 24 jam")
            .setPositiveButton("Tonton Iklan") { _, _ ->
                if (AdManager.isRewardedReady()) {
                    AdManager.showRewarded(this,
                        onRewarded = {
                            ProFeatureManager.unlock(this, feature)
                            Toast.makeText(this, "🎉 Fitur dibuka!", Toast.LENGTH_SHORT).show()
                            execute(feature)
                        },
                        onFailed = { Toast.makeText(this, "Iklan gagal", Toast.LENGTH_SHORT).show() })
                } else {
                    AdManager.loadRewarded(this)
                    Toast.makeText(this, "Iklan dimuat, coba lagi", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null).show()
    }

    private fun execute(feature: ProFeatureManager.ProFeature) {
        Toast.makeText(this, "${feature.displayName} dijalankan!", Toast.LENGTH_SHORT).show()
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
        ProFeatureManager.ProFeature.MERGE -> "Gabungkan beberapa PDF menjadi satu"
        ProFeatureManager.ProFeature.SPLIT -> "Pisahkan halaman PDF tertentu"
        ProFeatureManager.ProFeature.COMPRESS -> "Kurangi ukuran file PDF"
        ProFeatureManager.ProFeature.WATERMARK -> "Tambahkan tanda air ke PDF"
        ProFeatureManager.ProFeature.ENCRYPT -> "Lindungi PDF dengan kata sandi"
        ProFeatureManager.ProFeature.CONVERT -> "Konversi gambar ke PDF"
        ProFeatureManager.ProFeature.SIGN -> "Tanda tangani dokumen PDF"
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { onBackPressedDispatcher.onBackPressed(); return true }
        return super.onOptionsItemSelected(item)
    }
}
