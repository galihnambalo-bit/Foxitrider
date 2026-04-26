package com.foxitrider.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.foxitrider.R
import com.foxitrider.ads.AdManager
import com.foxitrider.databinding.ActivityMainBinding
import com.foxitrider.ui.fragments.*
import com.google.android.material.navigation.NavigationBarView

class MainActivity : AppCompatActivity(), NavigationBarView.OnItemSelectedListener {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavigation.setOnItemSelectedListener(this)
        binding.adContainer.addView(AdManager.createBannerAd(this))

        if (savedInstanceState == null) loadFragment(HomeFragment())
        intent?.data?.let { openPdf(it) }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val fragment: Fragment = when (item.itemId) {
            R.id.nav_home -> HomeFragment()
            R.id.nav_recent -> RecentFragment()
            R.id.nav_tools -> ToolsFragment()
            R.id.nav_settings -> SettingsFragment()
            else -> return false
        }
        loadFragment(fragment)
        return true
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment).commit()
    }

    fun openPdf(uri: Uri) {
        startActivity(Intent(this, PdfViewerActivity::class.java).apply {
            putExtra(PdfViewerActivity.EXTRA_URI, uri.toString())
        })
    }
}
