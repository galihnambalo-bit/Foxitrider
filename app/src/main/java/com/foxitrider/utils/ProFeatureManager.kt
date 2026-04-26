package com.foxitrider.utils

import android.content.Context
import android.content.SharedPreferences

object ProFeatureManager {

    private const val PREFS_NAME = "foxit_pro_prefs"
    private const val KEY_MERGE_UNLOCKED = "merge_unlocked"
    private const val KEY_COMPRESS_UNLOCKED = "compress_unlocked"
    private const val KEY_SPLIT_UNLOCKED = "split_unlocked"
    private const val KEY_WATERMARK_UNLOCKED = "watermark_unlocked"
    private const val KEY_ENCRYPT_UNLOCKED = "encrypt_unlocked"
    private const val KEY_SIGN_UNLOCKED = "sign_unlocked"
    private const val KEY_OCR_UNLOCKED = "ocr_unlocked"
    private const val KEY_CONVERT_UNLOCKED = "convert_unlocked"
    private const val KEY_UNLOCK_TIMESTAMP = "unlock_timestamp"
    private const val UNLOCK_DURATION_MS = 24 * 60 * 60 * 1000L // 24 hours

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isFeatureUnlocked(context: Context, feature: ProFeature): Boolean {
        val prefs = getPrefs(context)
        val timestamp = prefs.getLong(KEY_UNLOCK_TIMESTAMP, 0L)
        val isExpired = System.currentTimeMillis() - timestamp > UNLOCK_DURATION_MS

        if (isExpired) {
            // Reset all unlocks
            prefs.edit().clear().apply()
            return false
        }

        return prefs.getBoolean(feature.key, false)
    }

    fun unlockFeature(context: Context, feature: ProFeature) {
        val prefs = getPrefs(context)
        prefs.edit()
            .putBoolean(feature.key, true)
            .putLong(KEY_UNLOCK_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    fun unlockAllFeatures(context: Context) {
        val prefs = getPrefs(context)
        val editor = prefs.edit()
        ProFeature.values().forEach { feature ->
            editor.putBoolean(feature.key, true)
        }
        editor.putLong(KEY_UNLOCK_TIMESTAMP, System.currentTimeMillis())
        editor.apply()
    }

    fun getRemainingTime(context: Context): Long {
        val prefs = getPrefs(context)
        val timestamp = prefs.getLong(KEY_UNLOCK_TIMESTAMP, 0L)
        if (timestamp == 0L) return 0L
        val elapsed = System.currentTimeMillis() - timestamp
        return maxOf(0L, UNLOCK_DURATION_MS - elapsed)
    }

    enum class ProFeature(val key: String, val displayName: String, val icon: String) {
        MERGE(KEY_MERGE_UNLOCKED, "Gabungkan PDF", "🔗"),
        COMPRESS(KEY_COMPRESS_UNLOCKED, "Kompres PDF", "📦"),
        SPLIT(KEY_SPLIT_UNLOCKED, "Pisahkan PDF", "✂️"),
        WATERMARK(KEY_WATERMARK_UNLOCKED, "Watermark", "💧"),
        ENCRYPT(KEY_ENCRYPT_UNLOCKED, "Enkripsi PDF", "🔒"),
        SIGN(KEY_SIGN_UNLOCKED, "Tanda Tangan Digital", "✍️"),
        OCR(KEY_OCR_UNLOCKED, "OCR (Text Recognition)", "🔍"),
        CONVERT(KEY_CONVERT_UNLOCKED, "Konversi File", "🔄")
    }
}
