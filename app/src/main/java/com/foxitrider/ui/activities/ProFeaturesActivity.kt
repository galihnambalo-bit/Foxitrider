package com.foxitrider.ui.activities

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.foxitrider.ads.AdManager
import com.foxitrider.databinding.ActivityProFeaturesBinding
import com.foxitrider.utils.PdfProcessor
import com.foxitrider.utils.ProFeatureManager
import kotlinx.coroutines.launch

class ProFeaturesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProFeaturesBinding
    private val selectedUris = mutableListOf<Uri>()
    private var currentFeature = ProFeatureManager.ProFeature.MERGE

    companion object { const val EXTRA_FEATURE = "feature" }

    private val pickFiles = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            selectedUris.clear()
            selectedUris.addAll(uris)
            binding.btnSelectFile.text = "✅ ${uris.size} file dipilih"
            Toast.makeText(this, "✅ ${uris.size} file PDF dipilih", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickSingleFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedUris.clear()
            selectedUris.add(it)
            val name = it.lastPathSegment ?: "file dipilih"
            binding.btnSelectFile.text = "✅ $name"
            Toast.makeText(this, "✅ File dipilih", Toast.LENGTH_SHORT).show()
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
        currentFeature = ProFeatureManager.ProFeature.values().find { it.key == featureKey }
            ?: ProFeatureManager.ProFeature.MERGE

        supportActionBar?.title = currentFeature.displayName
        binding.tvFeatureIcon.text = getIcon(currentFeature)
        binding.tvFeatureTitle.text = currentFeature.displayName
        binding.tvFeatureDesc.text = getDesc(currentFeature)

        binding.btnSelectFile.setOnClickListener {
            if (currentFeature == ProFeatureManager.ProFeature.MERGE) {
                pickFiles.launch("application/pdf")
            } else {
                pickSingleFile.launch("application/pdf")
            }
        }

        binding.btnExecute.setOnClickListener {
            if (selectedUris.isEmpty()) {
                Toast.makeText(this, "⚠️ Pilih file PDF terlebih dahulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (ProFeatureManager.isUnlocked(this, currentFeature)) {
                runFeature()
            } else {
                showAdDialog()
            }
        }
    }

    private fun showAdDialog() {
        AlertDialog.Builder(this)
            .setTitle("🎬 Buka Fitur Pro GRATIS!")
            .setMessage("Tonton iklan ~30 detik untuk membuka '${currentFeature.displayName}' GRATIS!\n\n✅ Tidak perlu bayar\n✅ Akses 24 jam penuh")
            .setPositiveButton("🎬 Tonton Iklan Sekarang") { _, _ ->
                if (AdManager.isRewardedReady()) {
                    AdManager.showRewarded(this,
                        onRewarded = {
                            ProFeatureManager.unlock(this, currentFeature)
                            Toast.makeText(this, "🎉 Fitur '${currentFeature.displayName}' berhasil dibuka!", Toast.LENGTH_LONG).show()
                            runFeature()
                        },
                        onFailed = {
                            Toast.makeText(this, "❌ Iklan gagal, coba lagi", Toast.LENGTH_SHORT).show()
                            AdManager.loadRewarded(this)
                        }
                    )
                } else {
                    AdManager.loadRewarded(this)
                    Toast.makeText(this, "⏳ Iklan sedang dimuat... Coba lagi 5 detik", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Batal", null).show()
    }

    private fun runFeature() {
        binding.btnExecute.isEnabled = false
        binding.btnExecute.text = "⏳ Memproses..."

        lifecycleScope.launch {
            val result: Result<java.io.File> = when (currentFeature) {
                ProFeatureManager.ProFeature.MERGE -> {
                    if (selectedUris.size < 2) {
                        runOnUiThread { Toast.makeText(this@ProFeaturesActivity, "Pilih minimal 2 file untuk digabung", Toast.LENGTH_SHORT).show() }
                        resetButton(); return@launch
                    }
                    PdfProcessor.merge(this@ProFeaturesActivity, selectedUris)
                }
                ProFeatureManager.ProFeature.SPLIT -> {
                    showSplitDialog(); return@launch
                }
                ProFeatureManager.ProFeature.COMPRESS -> {
                    PdfProcessor.compress(this@ProFeaturesActivity, selectedUris[0])
                }
                ProFeatureManager.ProFeature.WATERMARK -> {
                    showWatermarkDialog(); return@launch
                }
                ProFeatureManager.ProFeature.ENCRYPT -> {
                    showEncryptDialog(); return@launch
                }
                ProFeatureManager.ProFeature.CONVERT -> {
                    PdfProcessor.compress(this@ProFeaturesActivity, selectedUris[0])
                }
                ProFeatureManager.ProFeature.SIGN -> {
                    PdfProcessor.addWatermark(this@ProFeaturesActivity, selectedUris[0], "SIGNED")
                }
            }

            runOnUiThread {
                result.onSuccess { file ->
                    Toast.makeText(this@ProFeaturesActivity,
                        "✅ Berhasil!\nDisimpan: ${file.name}\nUkuran: ${file.length()/1024}KB",
                        Toast.LENGTH_LONG).show()
                    binding.btnExecute.text = "✅ Selesai! Buka File"
                    binding.btnExecute.isEnabled = true
                    binding.btnExecute.setOnClickListener { shareFile(file) }
                }.onFailure { e ->
                    Toast.makeText(this@ProFeaturesActivity, "❌ Gagal: ${e.message}", Toast.LENGTH_LONG).show()
                    resetButton()
                }
            }
        }
    }

    private fun showSplitDialog() {
        resetButton()
        val layout = android.widget.LinearLayout(this).apply { orientation = android.widget.LinearLayout.VERTICAL; setPadding(50,20,50,20) }
        val etStart = EditText(this).apply { hint = "Halaman mulai (misal: 1)"; inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        val etEnd = EditText(this).apply { hint = "Halaman akhir (misal: 3)"; inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        layout.addView(etStart); layout.addView(etEnd)
        AlertDialog.Builder(this).setTitle("✂️ Pisahkan Halaman").setView(layout)
            .setPositiveButton("Pisahkan") { _, _ ->
                val s = etStart.text.toString().toIntOrNull() ?: 1
                val e = etEnd.text.toString().toIntOrNull() ?: 1
                binding.btnExecute.isEnabled = false
                binding.btnExecute.text = "⏳ Memproses..."
                lifecycleScope.launch {
                    val result = PdfProcessor.split(this@ProFeaturesActivity, selectedUris[0], s, e)
                    runOnUiThread {
                        result.onSuccess { file ->
                            Toast.makeText(this@ProFeaturesActivity, "✅ Halaman $s-$e berhasil dipisahkan!\n${file.name}", Toast.LENGTH_LONG).show()
                            binding.btnExecute.text = "✅ Buka File"
                            binding.btnExecute.isEnabled = true
                            binding.btnExecute.setOnClickListener { shareFile(file) }
                        }.onFailure { e -> Toast.makeText(this@ProFeaturesActivity, "❌ Gagal: ${e.message}", Toast.LENGTH_SHORT).show(); resetButton() }
                    }
                }
            }.setNegativeButton("Batal") { _, _ -> resetButton() }.show()
    }

    private fun showWatermarkDialog() {
        resetButton()
        val et = EditText(this).apply { hint = "Teks watermark (misal: RAHASIA)"; setText("CONFIDENTIAL") }
        AlertDialog.Builder(this).setTitle("💧 Teks Watermark").setView(et)
            .setPositiveButton("Tambahkan") { _, _ ->
                val text = et.text.toString().ifEmpty { "WATERMARK" }
                binding.btnExecute.isEnabled = false; binding.btnExecute.text = "⏳ Memproses..."
                lifecycleScope.launch {
                    val result = PdfProcessor.addWatermark(this@ProFeaturesActivity, selectedUris[0], text)
                    runOnUiThread {
                        result.onSuccess { file ->
                            Toast.makeText(this@ProFeaturesActivity, "✅ Watermark '$text' ditambahkan!\n${file.name}", Toast.LENGTH_LONG).show()
                            binding.btnExecute.text = "✅ Buka File"; binding.btnExecute.isEnabled = true
                            binding.btnExecute.setOnClickListener { shareFile(file) }
                        }.onFailure { e -> Toast.makeText(this@ProFeaturesActivity, "❌ Gagal: ${e.message}", Toast.LENGTH_SHORT).show(); resetButton() }
                    }
                }
            }.setNegativeButton("Batal") { _, _ -> resetButton() }.show()
    }

    private fun showEncryptDialog() {
        resetButton()
        val et = EditText(this).apply { hint = "Kata sandi PDF"; inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD }
        AlertDialog.Builder(this).setTitle("🔒 Enkripsi PDF").setView(et)
            .setPositiveButton("Enkripsi") { _, _ ->
                val pass = et.text.toString()
                if (pass.isEmpty()) { Toast.makeText(this, "Masukkan kata sandi!", Toast.LENGTH_SHORT).show(); resetButton(); return@setPositiveButton }
                binding.btnExecute.isEnabled = false; binding.btnExecute.text = "⏳ Mengenkripsi..."
                lifecycleScope.launch {
                    val result = PdfProcessor.encrypt(this@ProFeaturesActivity, selectedUris[0], pass)
                    runOnUiThread {
                        result.onSuccess { file ->
                            Toast.makeText(this@ProFeaturesActivity, "✅ PDF berhasil dienkripsi!\nPassword: $pass\n${file.name}", Toast.LENGTH_LONG).show()
                            binding.btnExecute.text = "✅ Buka File"; binding.btnExecute.isEnabled = true
                            binding.btnExecute.setOnClickListener { shareFile(file) }
                        }.onFailure { e -> Toast.makeText(this@ProFeaturesActivity, "❌ Gagal: ${e.message}", Toast.LENGTH_SHORT).show(); resetButton() }
                    }
                }
            }.setNegativeButton("Batal") { _, _ -> resetButton() }.show()
    }

    private fun shareFile(file: java.io.File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Bagikan / Simpan PDF"))
    }

    private fun resetButton() {
        binding.btnExecute.isEnabled = true
        binding.btnExecute.text = "🎬 Tonton Iklan & Jalankan"
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
        ProFeatureManager.ProFeature.COMPRESS -> "Kurangi ukuran file PDF"
        ProFeatureManager.ProFeature.WATERMARK -> "Tambahkan tanda air teks ke semua halaman"
        ProFeatureManager.ProFeature.ENCRYPT -> "Lindungi PDF dengan kata sandi 256-bit"
        ProFeatureManager.ProFeature.CONVERT -> "Kompres dan optimalkan PDF"
        ProFeatureManager.ProFeature.SIGN -> "Tambahkan tanda tangan ke PDF"
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { onBackPressedDispatcher.onBackPressed(); return true }
        return super.onOptionsItemSelected(item)
    }
}
