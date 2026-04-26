package com.foxitrider.ui.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.foxitrider.databinding.FragmentHomeBinding
import com.foxitrider.ui.activities.FileManagerActivity
import com.foxitrider.ui.activities.PdfViewerActivity
import com.foxitrider.ui.activities.ProFeaturesActivity

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val openFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { openPdfFromUri(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        // Open PDF button
        binding.btnOpenPdf.setOnClickListener {
            openFileLauncher.launch("application/pdf")
        }

        // Browse files
        binding.btnBrowseFiles.setOnClickListener {
            startActivity(Intent(requireContext(), FileManagerActivity::class.java))
        }

        // Quick access cards
        binding.cardMergePdf.setOnClickListener {
            startActivity(Intent(requireContext(), ProFeaturesActivity::class.java).apply {
                putExtra(ProFeaturesActivity.EXTRA_FEATURE, "merge")
            })
        }

        binding.cardSplitPdf.setOnClickListener {
            startActivity(Intent(requireContext(), ProFeaturesActivity::class.java).apply {
                putExtra(ProFeaturesActivity.EXTRA_FEATURE, "split")
            })
        }

        binding.cardCompressPdf.setOnClickListener {
            startActivity(Intent(requireContext(), ProFeaturesActivity::class.java).apply {
                putExtra(ProFeaturesActivity.EXTRA_FEATURE, "compress")
            })
        }

        binding.cardWatermark.setOnClickListener {
            startActivity(Intent(requireContext(), ProFeaturesActivity::class.java).apply {
                putExtra(ProFeaturesActivity.EXTRA_FEATURE, "watermark")
            })
        }

        binding.cardEncrypt.setOnClickListener {
            startActivity(Intent(requireContext(), ProFeaturesActivity::class.java).apply {
                putExtra(ProFeaturesActivity.EXTRA_FEATURE, "encrypt")
            })
        }

        binding.cardConvert.setOnClickListener {
            startActivity(Intent(requireContext(), ProFeaturesActivity::class.java).apply {
                putExtra(ProFeaturesActivity.EXTRA_FEATURE, "convert")
            })
        }
    }

    private fun openPdfFromUri(uri: Uri) {
        val intent = Intent(requireContext(), PdfViewerActivity::class.java)
        intent.putExtra(PdfViewerActivity.EXTRA_PDF_URI, uri.toString())
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
