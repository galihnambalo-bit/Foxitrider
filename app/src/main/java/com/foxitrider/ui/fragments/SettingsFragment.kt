package com.foxitrider.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.foxitrider.BuildConfig
import com.foxitrider.ads.AdManager
import com.foxitrider.databinding.FragmentSettingsBinding
import com.foxitrider.utils.ProFeatureManager

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSettings()
        showProStatus()
    }

    private fun setupSettings() {
        binding.tvVersion.text = "Versi ${BuildConfig.VERSION_NAME}"

        binding.btnUnlockAll.setOnClickListener {
            if (AdManager.isRewardedAdReady()) {
                AdManager.showRewardedAd(
                    requireActivity(),
                    onRewarded = {
                        ProFeatureManager.unlockAllFeatures(requireContext())
                        Toast.makeText(context, "🎉 Semua fitur Pro dibuka selama 24 jam!", Toast.LENGTH_LONG).show()
                        showProStatus()
                    },
                    onAdFailed = {
                        Toast.makeText(context, "Iklan tidak tersedia saat ini", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                AdManager.loadRewardedAd(requireContext())
                Toast.makeText(context, "Iklan sedang dimuat...", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnPrivacyPolicy.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://foxitrider.com/privacy"))
            startActivity(intent)
        }

        binding.btnRateApp.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${requireContext().packageName}"))
            try {
                startActivity(intent)
            } catch (e: Exception) {
                val webIntent = Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=${requireContext().packageName}"))
                startActivity(webIntent)
            }
        }

        binding.btnShareApp.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT,
                    "Coba Foxit Rider - PDF Editor terbaik! Gratis di Play Store: https://play.google.com/store/apps/details?id=${requireContext().packageName}")
            }
            startActivity(Intent.createChooser(shareIntent, "Bagikan Foxit Rider"))
        }
    }

    private fun showProStatus() {
        val remainingTime = ProFeatureManager.getRemainingTime(requireContext())
        if (remainingTime > 0) {
            val hours = remainingTime / (1000 * 60 * 60)
            val minutes = (remainingTime % (1000 * 60 * 60)) / (1000 * 60)
            binding.tvProStatus.text = "✅ Pro Aktif: $hours jam $minutes menit tersisa"
            binding.tvProStatus.visibility = View.VISIBLE
        } else {
            binding.tvProStatus.text = "⭐ Tonton iklan untuk membuka fitur Pro"
            binding.tvProStatus.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
