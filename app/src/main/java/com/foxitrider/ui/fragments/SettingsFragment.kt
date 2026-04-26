package com.foxitrider.ui.fragments
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.foxitrider.BuildConfig
import com.foxitrider.ads.AdManager
import com.foxitrider.databinding.FragmentSettingsBinding
import com.foxitrider.utils.ProFeatureManager
class SettingsFragment : Fragment() {
    private var _b: FragmentSettingsBinding? = null
    private val b get() = _b!!
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View { _b = FragmentSettingsBinding.inflate(i,c,false); return b.root }
    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)
        b.tvVersion.text = "Versi ${BuildConfig.VERSION_NAME}"
        updateProStatus()
        b.btnUnlockAll.setOnClickListener {
            if (AdManager.isRewardedReady()) {
                AdManager.showRewarded(requireActivity(),
                    onRewarded = { ProFeatureManager.unlockAll(requireContext()); updateProStatus(); Toast.makeText(context,"🎉 Semua fitur Pro dibuka!",Toast.LENGTH_LONG).show() },
                    onFailed = { Toast.makeText(context,"Iklan tidak tersedia",Toast.LENGTH_SHORT).show() })
            } else { AdManager.loadRewarded(requireContext()); Toast.makeText(context,"Iklan dimuat...",Toast.LENGTH_SHORT).show() }
        }
        b.btnRate.setOnClickListener {
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${requireContext().packageName}"))) }
            catch (e: Exception) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${requireContext().packageName}"))) }
        }
        b.btnShare.setOnClickListener {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type="text/plain"; putExtra(Intent.EXTRA_TEXT,"Coba Foxit Rider - PDF Editor gratis!") },"Bagikan"))
        }
    }
    private fun updateProStatus() {
        val ms = ProFeatureManager.remainingMs(requireContext())
        b.tvProStatus.text = if (ms > 0) "✅ Pro aktif: ${ms/3600000}j ${(ms%3600000)/60000}m" else "⭐ Tonton iklan untuk buka fitur Pro"
    }
    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
