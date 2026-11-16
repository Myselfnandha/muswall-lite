package com.muswall.lite.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenConfigTest {

    @Test
    fun testDefaultValues() {
        val config = ScreenConfig()

        assertEquals(ScreenConfig.EFFECT_BLUR_STILL, config.effectType)
        assertEquals(45, config.blurPercent)
        assertEquals(20, config.dimPercent)
        assertEquals(80, config.stillScalePercent)
        assertEquals(-35, config.stillOffsetYPercent)
        assertEquals(12, config.stillCornerRadiusPercent)
        assertEquals(ScreenConfig.TRANSITION_FADE, config.applyEffect)
        assertEquals(ScreenConfig.TRANSITION_FADE, config.restoreEffect)
        assertEquals(30, config.restoreDelaySeconds)
        assertFalse(config.applyEffectsToRestore)
        assertEquals(45, config.restoreBlurPercent)
        assertEquals(20, config.restoreDimPercent)
        assertEquals(-35, config.wallpaperOffsetYPercent)
        assertEquals(0, config.wallpaperOffsetXPercent)
        assertTrue(config.autoHorizontalFraming)
        assertEquals(ScreenConfig.ASPECT_FIT_WIDTH, config.aspectFitMode)
    }

    @Test
    fun testCustomValuesAndCopy() {
        val original = ScreenConfig(
            effectType = ScreenConfig.EFFECT_BLUR,
            blurPercent = 75,
            dimPercent = 40,
            restoreDelaySeconds = 60
        )

        val modified = original.copy(
            blurPercent = 50,
            applyEffectsToRestore = true
        )

        assertEquals(ScreenConfig.EFFECT_BLUR, modified.effectType)
        assertEquals(50, modified.blurPercent)
        assertEquals(40, modified.dimPercent)
        assertEquals(60, modified.restoreDelaySeconds)
        assertTrue(modified.applyEffectsToRestore)
    }

    @Test
    fun testTransitionConstants() {
        assertEquals(0, ScreenConfig.TRANSITION_FADE)
        assertEquals(1, ScreenConfig.TRANSITION_ZOOM)
        assertEquals(2, ScreenConfig.TRANSITION_SLIDE_UP)
        assertEquals(3, ScreenConfig.TRANSITION_SLIDE_DOWN)
        assertEquals(4, ScreenConfig.TRANSITION_RADIAL)
        assertEquals(5, ScreenConfig.TRANSITION_BLUR_DISSOLVE)
        assertEquals(6, ScreenConfig.TRANSITION_NONE)
    }

    @Test
    fun testAspectFitConstants() {
        assertEquals(0, ScreenConfig.ASPECT_FILL)
        assertEquals(1, ScreenConfig.ASPECT_FIT_WIDTH)
    }
}
