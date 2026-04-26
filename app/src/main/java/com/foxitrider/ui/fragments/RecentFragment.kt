package com.foxitrider.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.foxitrider.databinding.FragmentRecentBinding
import com.foxitrider.ui.activities.PdfViewerActivity
import java.io.File

class RecentFragment : Fragment() {
    private var _binding: FragmentRecentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRecentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadRecentFiles()
    }

    private fun loadRecentFiles() {
        // Load recently opened files from SharedPreferences
        val prefs = requireContext().getSharedPreferences("recent_files", android.content.Context.MODE_PRIVATE)
        val recentJson = prefs.getString("files", "[]") ?: "[]"
        // Display recent files
        binding.tvEmpty.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
