package com.foxitrider.ui.activities

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.foxitrider.R
import com.foxitrider.ads.AdManager
import com.foxitrider.databinding.ActivitySplashBinding
import com.foxitrider.utils.PdfUtils

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize PdfBox
        PdfUtils.initialize(this)

        // Preload ads
        AdManager.loadInterstitialAd(this)
        AdManager.loadRewardedAd(this)

        startAnimations()

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 2500)
    }

    private fun startAnimations() {
        // Logo scale animation
        val scaleX = ObjectAnimator.ofFloat(binding.ivLogo, View.SCALE_X, 0.3f, 1f)
        val scaleY = ObjectAnimator.ofFloat(binding.ivLogo, View.SCALE_Y, 0.3f, 1f)
        val alpha = ObjectAnimator.ofFloat(binding.ivLogo, View.ALPHA, 0f, 1f)

        val logoAnim = AnimatorSet()
        logoAnim.playTogether(scaleX, scaleY, alpha)
        logoAnim.duration = 800
        logoAnim.interpolator = AccelerateDecelerateInterpolator()
        logoAnim.start()

        // Text fade in
        binding.tvAppName.alpha = 0f
        binding.tvTagline.alpha = 0f

        Handler(Looper.getMainLooper()).postDelayed({
            binding.tvAppName.animate().alpha(1f).setDuration(600).start()
        }, 700)

        Handler(Looper.getMainLooper()).postDelayed({
            binding.tvTagline.animate().alpha(1f).setDuration(600).start()
        }, 1000)

        // Progress bar animation
        Handler(Looper.getMainLooper()).postDelayed({
            binding.progressBar.visibility = View.VISIBLE
            binding.progressBar.animate().alpha(1f).setDuration(400).start()
        }, 1500)
    }
}
