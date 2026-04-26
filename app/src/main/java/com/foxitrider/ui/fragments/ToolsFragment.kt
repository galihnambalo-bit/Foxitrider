package com.foxitrider.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.foxitrider.databinding.FragmentToolsBinding
import com.foxitrider.ui.activities.ProFeaturesActivity

class ToolsFragment : Fragment() {
    private var _binding: FragmentToolsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentToolsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTools()
    }

    private fun setupTools() {
        val launchFeature = { feature: String ->
            startActivity(Intent(requireContext(), ProFeaturesActivity::class.java).apply {
                putExtra(ProFeaturesActivity.EXTRA_FEATURE, feature)
            })
        }

        binding.btnMerge.setOnClickListener { launchFeature("merge") }
        binding.btnSplit.setOnClickListener { launchFeature("split") }
        binding.btnCompress.setOnClickListener { launchFeature("compress") }
        binding.btnWatermark.setOnClickListener { launchFeature("watermark") }
        binding.btnEncrypt.setOnClickListener { launchFeature("encrypt") }
        binding.btnDecrypt.setOnClickListener { launchFeature("encrypt") }
        binding.btnConvert.setOnClickListener { launchFeature("convert") }
        binding.btnSign.setOnClickListener { launchFeature("sign") }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
