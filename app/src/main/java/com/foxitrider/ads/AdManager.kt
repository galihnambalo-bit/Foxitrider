package com.foxitrider.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdManager {
    private const val BANNER_ID = "ca-app-pub-4744122948371705/5604675924"
    private const val INTERSTITIAL_ID = "ca-app-pub-4744122948371705/8972330436"
    private const val REWARDED_ID = "ca-app-pub-4744122948371705/1141627244"

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    fun createBannerAd(context: Context): AdView {
        return AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = BANNER_ID
            loadAd(AdRequest.Builder().build())
        }
    }

    fun loadInterstitial(context: Context) {
        if (interstitialAd != null) return
        InterstitialAd.load(context, INTERSTITIAL_ID, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitialAd = ad }
                override fun onAdFailedToLoad(e: LoadAdError) { interstitialAd = null }
            })
    }

    fun showInterstitial(activity: Activity, onDone: () -> Unit) {
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitial(activity)
                    onDone()
                }
                override fun onAdFailedToShowFullScreenContent(e: AdError) {
                    interstitialAd = null; onDone()
                }
            }
            ad.show(activity)
        } else {
            onDone()
            loadInterstitial(activity)
        }
    }

    fun loadRewarded(context: Context) {
        if (rewardedAd != null) return
        RewardedAd.load(context, REWARDED_ID, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { rewardedAd = ad }
                override fun onAdFailedToLoad(e: LoadAdError) { rewardedAd = null }
            })
    }

    fun showRewarded(activity: Activity, onRewarded: () -> Unit, onFailed: () -> Unit) {
        val ad = rewardedAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null; loadRewarded(activity)
                }
                override fun onAdFailedToShowFullScreenContent(e: AdError) {
                    rewardedAd = null; onFailed()
                }
            }
            ad.show(activity) { onRewarded() }
        } else {
            onFailed(); loadRewarded(activity)
        }
    }

    fun isRewardedReady() = rewardedAd != null
}
