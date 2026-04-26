package com.foxitrider.ui.fragments
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.foxitrider.databinding.FragmentToolsBinding
import com.foxitrider.ui.activities.ProFeaturesActivity
class ToolsFragment : Fragment() {
    private var _b: FragmentToolsBinding? = null
    private val b get() = _b!!
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View { _b = FragmentToolsBinding.inflate(i,c,false); return b.root }
    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)
        val launch = { key: String -> startActivity(Intent(requireContext(), ProFeaturesActivity::class.java).putExtra(ProFeaturesActivity.EXTRA_FEATURE, key)) }
        b.btnMerge.setOnClickListener { launch("merge") }
        b.btnSplit.setOnClickListener { launch("split") }
        b.btnCompress.setOnClickListener { launch("compress") }
        b.btnWatermark.setOnClickListener { launch("watermark") }
        b.btnEncrypt.setOnClickListener { launch("encrypt") }
        b.btnConvert.setOnClickListener { launch("convert") }
        b.btnSign.setOnClickListener { launch("sign") }
    }
    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
