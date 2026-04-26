package com.foxitrider.ui.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.foxitrider.databinding.FragmentHomeBinding
import com.foxitrider.ui.activities.FileManagerActivity
import com.foxitrider.ui.activities.ProFeaturesActivity
import com.foxitrider.ui.activities.PdfViewerActivity

class HomeFragment : Fragment() {
    private var _b: FragmentHomeBinding? = null
    private val b get() = _b!!

    private val pickPdf = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            startActivity(Intent(requireContext(), PdfViewerActivity::class.java).apply {
                putExtra(PdfViewerActivity.EXTRA_URI, it.toString())
            })
        }
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentHomeBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)
        b.btnOpenPdf.setOnClickListener { pickPdf.launch("application/pdf") }
        b.btnBrowseFiles.setOnClickListener { startActivity(Intent(requireContext(), FileManagerActivity::class.java)) }
        val launch = { key: String -> startActivity(Intent(requireContext(), ProFeaturesActivity::class.java).putExtra(ProFeaturesActivity.EXTRA_FEATURE, key)) }
        b.cardMerge.setOnClickListener { launch("merge") }
        b.cardSplit.setOnClickListener { launch("split") }
        b.cardCompress.setOnClickListener { launch("compress") }
        b.cardWatermark.setOnClickListener { launch("watermark") }
        b.cardEncrypt.setOnClickListener { launch("encrypt") }
        b.cardConvert.setOnClickListener { launch("convert") }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
