package com.foxitrider.utils

import android.content.Context

object ProFeatureManager {
    private const val PREFS = "foxit_pro"
    private const val KEY_TS = "unlock_ts"
    private const val DURATION = 24 * 60 * 60 * 1000L

    enum class ProFeature(val key: String, val displayName: String) {
        MERGE("merge", "Gabungkan PDF"),
        SPLIT("split", "Pisahkan PDF"),
        COMPRESS("compress", "Kompres PDF"),
        WATERMARK("watermark", "Watermark"),
        ENCRYPT("encrypt", "Enkripsi PDF"),
        CONVERT("convert", "Konversi File"),
        SIGN("sign", "Tanda Tangan")
    }

    fun isUnlocked(context: Context, feature: ProFeature): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val ts = prefs.getLong(KEY_TS, 0L)
        if (System.currentTimeMillis() - ts > DURATION) {
            prefs.edit().clear().apply(); return false
        }
        return prefs.getBoolean(feature.key, false)
    }

    fun unlock(context: Context, feature: ProFeature) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(feature.key, true)
            .putLong(KEY_TS, System.currentTimeMillis())
            .apply()
    }

    fun unlockAll(context: Context) {
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        ProFeature.values().forEach { editor.putBoolean(it.key, true) }
        editor.putLong(KEY_TS, System.currentTimeMillis()).apply()
    }

    fun remainingMs(context: Context): Long {
        val ts = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_TS, 0L)
        return maxOf(0L, DURATION - (System.currentTimeMillis() - ts))
    }
}
