package com.muswall.lite.domain.model

data class ScreenConfig(
    val effectType: Int = EFFECT_BLUR_STILL,
    val blurPercent: Int = 45,
    val dimPercent: Int = 20,
    val stillScalePercent: Int = 80,
    val stillOffsetYPercent: Int = -35,
    val stillCornerRadiusPercent: Int = 12,
    val applyEffect: Int = TRANSITION_FADE,
    val restoreEffect: Int = TRANSITION_FADE,
    val restoreDelaySeconds: Int = 30,
    val applyEffectsToRestore: Boolean = false,
    val restoreBlurPercent: Int = 45,
    val restoreDimPercent: Int = 20,
    val wallpaperOffsetYPercent: Int = -35,
    val wallpaperOffsetXPercent: Int = 0,
    val autoHorizontalFraming: Boolean = true,
    val aspectFitMode: Int = ASPECT_FIT_WIDTH,
    val amoledMode: Boolean = false,
    val scrimEnabled: Boolean = false,
    val showTrackInfo: Boolean = false,
    val trackInfoOffsetYPercent: Int = 25
) {
    companion object {
        const val EFFECT_BLUR = 0
        const val EFFECT_STILL = 1
        const val EFFECT_BLUR_STILL = 2

        const val ASPECT_FILL = 0
        const val ASPECT_FIT_WIDTH = 1

        const val TRANSITION_FADE = 0
        const val TRANSITION_ZOOM = 1
        const val TRANSITION_SLIDE_UP = 2
        const val TRANSITION_SLIDE_DOWN = 3
        const val TRANSITION_RADIAL = 4
        const val TRANSITION_BLUR_DISSOLVE = 5
        const val TRANSITION_NONE = 6

        const val PRESET_BALANCED = 0
        const val PRESET_AMOLED = 1
        const val PRESET_IMMERSIVE = 2
        const val PRESET_MINIMAL = 3
        const val PRESET_POSTER = 4

        fun createPreset(presetId: Int, isLockScreen: Boolean): ScreenConfig {
            val safeZone = if (isLockScreen) 35 else -35
            return when (presetId) {
                PRESET_AMOLED -> ScreenConfig(
                    effectType = EFFECT_STILL,
                    amoledMode = true,
                    blurPercent = 0,
                    dimPercent = 0,
                    stillScalePercent = 76,
                    stillOffsetYPercent = safeZone,
                    stillCornerRadiusPercent = 18,
                    scrimEnabled = false,
                    showTrackInfo = false
                )
                PRESET_IMMERSIVE -> ScreenConfig(
                    effectType = EFFECT_BLUR,
                    blurPercent = 65,
                    dimPercent = 25,
                    aspectFitMode = ASPECT_FIT_WIDTH,
                    scrimEnabled = true,
                    showTrackInfo = false
                )
                PRESET_MINIMAL -> ScreenConfig(
                    effectType = EFFECT_STILL,
                    amoledMode = false,
                    blurPercent = 0,
                    dimPercent = 15,
                    stillScalePercent = 72,
                    stillOffsetYPercent = safeZone,
                    stillCornerRadiusPercent = 14,
                    scrimEnabled = false,
                    showTrackInfo = false
                )
                PRESET_POSTER -> ScreenConfig(
                    effectType = EFFECT_BLUR_STILL,
                    blurPercent = 55,
                    dimPercent = 30,
                    stillScalePercent = 94,
                    stillOffsetYPercent = safeZone,
                    stillCornerRadiusPercent = 8,
                    scrimEnabled = true,
                    showTrackInfo = false
                )
                else -> ScreenConfig(
                    effectType = EFFECT_BLUR_STILL,
                    blurPercent = 45,
                    dimPercent = 20,
                    stillScalePercent = 80,
                    stillOffsetYPercent = safeZone,
                    stillCornerRadiusPercent = 12,
                    scrimEnabled = false,
                    showTrackInfo = false
                )
            }
        }
    }
}
