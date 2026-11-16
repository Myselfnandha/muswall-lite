package com.muswall.lite.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.muswall.lite.domain.model.ScreenConfig

/**
 * Repository responsible for persisting and retrieving user wallpaper configurations
 * from SharedPreferences.
 */
class WallpaperConfigRepository(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isSeparateScreens(): Boolean {
        return prefs.getBoolean(KEY_SEPARATE_SCREENS, false)
    }

    fun setSeparateScreens(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SEPARATE_SCREENS, enabled).apply()
    }

    fun getHomeConfig(): ScreenConfig {
        return readConfig("home")
    }

    fun getLockConfig(): ScreenConfig {
        return readConfig("lock")
    }

    fun saveHomeConfig(config: ScreenConfig) {
        saveConfig("home", config)
    }

    fun saveLockConfig(config: ScreenConfig) {
        saveConfig("lock", config)
    }

    private fun readConfig(prefix: String): ScreenConfig {
        val defaultOffset = if (prefix == "lock") 35 else -35
        return ScreenConfig(
            effectType = prefs.getInt("${prefix}_effect_type", ScreenConfig.EFFECT_BLUR_STILL),
            blurPercent = prefs.getInt("${prefix}_blur_percent", 45),
            dimPercent = prefs.getInt("${prefix}_dim_percent", 20),
            stillScalePercent = prefs.getInt("${prefix}_still_scale_percent", 80),
            stillOffsetYPercent = prefs.getInt("${prefix}_still_offset_y_percent", defaultOffset),
            stillCornerRadiusPercent = prefs.getInt("${prefix}_still_corner_radius_percent", 12),
            applyEffect = prefs.getInt("${prefix}_apply_effect", ScreenConfig.TRANSITION_FADE),
            restoreEffect = prefs.getInt("${prefix}_restore_effect", ScreenConfig.TRANSITION_FADE),
            restoreDelaySeconds = prefs.getInt("${prefix}_restore_delay", 30),
            applyEffectsToRestore = prefs.getBoolean("${prefix}_apply_effects_to_restore", false),
            restoreBlurPercent = prefs.getInt("${prefix}_restore_blur_percent", 45),
            restoreDimPercent = prefs.getInt("${prefix}_restore_dim_percent", 20),
            wallpaperOffsetYPercent = prefs.getInt("${prefix}_wallpaper_offset_y_percent", defaultOffset),
            wallpaperOffsetXPercent = prefs.getInt("${prefix}_wallpaper_offset_x_percent", 0),
            autoHorizontalFraming = prefs.getBoolean("${prefix}_auto_horizontal_framing", true),
            aspectFitMode = prefs.getInt("${prefix}_aspect_fit_mode", ScreenConfig.ASPECT_FIT_WIDTH)
        )
    }

    private fun saveConfig(prefix: String, config: ScreenConfig) {
        prefs.edit().apply {
            putInt("${prefix}_effect_type", config.effectType)
            putInt("${prefix}_blur_percent", config.blurPercent)
            putInt("${prefix}_dim_percent", config.dimPercent)
            putInt("${prefix}_still_scale_percent", config.stillScalePercent)
            putInt("${prefix}_still_offset_y_percent", config.stillOffsetYPercent)
            putInt("${prefix}_still_corner_radius_percent", config.stillCornerRadiusPercent)
            putInt("${prefix}_apply_effect", config.applyEffect)
            putInt("${prefix}_restore_effect", config.restoreEffect)
            putInt("${prefix}_restore_delay", config.restoreDelaySeconds)
            putBoolean("${prefix}_apply_effects_to_restore", config.applyEffectsToRestore)
            putInt("${prefix}_restore_blur_percent", config.restoreBlurPercent)
            putInt("${prefix}_restore_dim_percent", config.restoreDimPercent)
            putInt("${prefix}_wallpaper_offset_y_percent", config.wallpaperOffsetYPercent)
            putInt("${prefix}_wallpaper_offset_x_percent", config.wallpaperOffsetXPercent)
            putBoolean("${prefix}_auto_horizontal_framing", config.autoHorizontalFraming)
            putInt("${prefix}_aspect_fit_mode", config.aspectFitMode)
            apply()
        }
    }

    companion object {
        const val PREFS_NAME = "muswall_lite_prefs"
        const val KEY_SEPARATE_SCREENS = "separate_screens"
    }
}
