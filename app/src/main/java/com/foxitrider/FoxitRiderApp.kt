package com.foxitrider

import android.app.Application
import androidx.multidex.MultiDexApplication
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration

class FoxitRiderApp : MultiDexApplication() {

    companion object {
        lateinit var instance: FoxitRiderApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        initAdMob()
    }

    private fun initAdMob() {
        // Initialize AdMob
        MobileAds.initialize(this) { initializationStatus ->
            val statusMap = initializationStatus.adapterStatusMap
            for ((adapterClass, status) in statusMap) {
                android.util.Log.d("AdMob", "Adapter: $adapterClass, Status: ${status.initializationState}")
            }
        }

        // Configure test devices if needed (remove for production)
        val testDeviceIds = listOf("EMULATOR")
        val config = RequestConfiguration.Builder()
            .setTestDeviceIds(testDeviceIds)
            .build()
        MobileAds.setRequestConfiguration(config)
    }
}
