package com.ultraviolette.navigator.model

import android.content.Context

/**
 * Persisted brand theme pack — survives process death so reopen keeps
 * the last chosen UV / KTM / BMW / Aprilia accent set.
 */
class BrandThemeStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun readBrandTheme(default: String = BrandTheme.DEFAULT): String =
        BrandTheme.canonicalize(prefs.getString(KEY_BRAND_THEME, null)) ?: default

    fun writeBrandTheme(theme: String) {
        val canonical = BrandTheme.canonicalize(theme) ?: return
        prefs.edit().putString(KEY_BRAND_THEME, canonical).apply()
    }

    private companion object {
        const val PREFS_NAME = "cluster_brand_theme"
        const val KEY_BRAND_THEME = "brand_theme"
    }
}
