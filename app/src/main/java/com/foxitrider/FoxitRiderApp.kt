package com.foxitrider

import androidx.multidex.MultiDexApplication
import com.google.android.gms.ads.MobileAds
import com.foxitrider.utils.PdfProcessor

class FoxitRiderApp : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
        PdfProcessor.init(this)
    }
}
