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
    val aspectFitMode: Int = ASPECT_FIT_WIDTH
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
    }
}
