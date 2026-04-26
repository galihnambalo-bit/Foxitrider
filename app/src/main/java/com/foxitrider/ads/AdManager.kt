package com.foxitrider.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions

object AdManager {

    private const val TAG = "AdManager"

    // Real AdMob IDs from screenshot
    const val APP_ID = "ca-app-pub-4744122948371705~2731100577"
    const val AD_UNIT_ID = "ca-app-pub-4744122948371705/5604675924"

    // Ad Unit IDs - use test IDs during development
    // For production, replace with real IDs
    private const val BANNER_AD_UNIT_ID = "ca-app-pub-4744122948371705/5604675924"
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712" // test
    private const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917" // test

    private var mInterstitialAd: InterstitialAd? = null
    private var mRewardedAd: RewardedAd? = null
    private var isInterstitialLoading = false
    private var isRewardedLoading = false

    // ─────────────── BANNER AD ───────────────

    fun createBannerAd(context: Context): AdView {
        val adView = AdView(context)
        adView.setAdSize(AdSize.BANNER)
        adView.adUnitId = BANNER_AD_UNIT_ID
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
        return adView
    }

    fun createSmartBannerAd(context: Context): AdView {
        val adView = AdView(context)
        adView.setAdSize(AdSize.SMART_BANNER)
        adView.adUnitId = BANNER_AD_UNIT_ID
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
        return adView
    }

    // ─────────────── INTERSTITIAL AD ───────────────

    fun loadInterstitialAd(context: Context) {
        if (isInterstitialLoading || mInterstitialAd != null) return
        isInterstitialLoading = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                    isInterstitialLoading = false
                    Log.d(TAG, "Interstitial loaded")

                    mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            mInterstitialAd = null
                            loadInterstitialAd(context)
                        }
                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            mInterstitialAd = null
                        }
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isInterstitialLoading = false
                    mInterstitialAd = null
                    Log.e(TAG, "Interstitial failed: ${loadAdError.message}")
                }
            }
        )
    }

    fun showInterstitialAd(activity: Activity, onAdFinished: () -> Unit) {
        if (mInterstitialAd != null) {
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    mInterstitialAd = null
                    loadInterstitialAd(activity)
                    onAdFinished()
                }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    mInterstitialAd = null
                    onAdFinished()
                }
            }
            mInterstitialAd?.show(activity)
        } else {
            onAdFinished()
            loadInterstitialAd(activity)
        }
    }

    // ─────────────── REWARDED AD (for Pro Features) ───────────────

    fun loadRewardedAd(context: Context) {
        if (isRewardedLoading || mRewardedAd != null) return
        isRewardedLoading = true

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    mRewardedAd = rewardedAd
                    isRewardedLoading = false
                    Log.d(TAG, "Rewarded ad loaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isRewardedLoading = false
                    mRewardedAd = null
                    Log.e(TAG, "Rewarded ad failed: ${loadAdError.message}")
                }
            }
        )
    }

    fun showRewardedAd(
        activity: Activity,
        onRewarded: () -> Unit,
        onAdFailed: () -> Unit
    ) {
        val rewardedAd = mRewardedAd
        if (rewardedAd != null) {
            rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    mRewardedAd = null
                    loadRewardedAd(activity)
                }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    mRewardedAd = null
                    onAdFailed()
                }
            }
            rewardedAd.show(activity) { rewardItem: RewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                onRewarded()
            }
        } else {
            onAdFailed()
            loadRewardedAd(activity)
        }
    }

    fun isRewardedAdReady(): Boolean = mRewardedAd != null
    fun isInterstitialAdReady(): Boolean = mInterstitialAd != null
}
