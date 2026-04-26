package com.foxitrider

import androidx.multidex.MultiDexApplication
import com.google.android.gms.ads.MobileAds

class FoxitRiderApp : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
    }
}
