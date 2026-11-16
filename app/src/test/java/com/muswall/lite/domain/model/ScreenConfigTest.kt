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
        assertFalse(config.amoledMode)
        assertFalse(config.scrimEnabled)
        assertFalse(config.showTrackInfo)
        assertEquals(25, config.trackInfoOffsetYPercent)
    }

    @Test
    fun testCustomValuesAndCopy() {
        val original = ScreenConfig(
            effectType = ScreenConfig.EFFECT_BLUR,
            blurPercent = 75,
            dimPercent = 40,
            restoreDelaySeconds = 60,
            amoledMode = true,
            scrimEnabled = true,
            showTrackInfo = true
        )

        val modified = original.copy(
            blurPercent = 50,
            applyEffectsToRestore = true,
            amoledMode = false
        )

        assertEquals(ScreenConfig.EFFECT_BLUR, modified.effectType)
        assertEquals(50, modified.blurPercent)
        assertEquals(40, modified.dimPercent)
        assertEquals(60, modified.restoreDelaySeconds)
        assertTrue(modified.applyEffectsToRestore)
        assertFalse(modified.amoledMode)
        assertTrue(modified.scrimEnabled)
        assertTrue(modified.showTrackInfo)
    }

    @Test
    fun testPresetsCreation() {
        val amoledHome = ScreenConfig.createPreset(ScreenConfig.PRESET_AMOLED, isLockScreen = false)
        assertTrue(amoledHome.amoledMode)
        assertEquals(ScreenConfig.EFFECT_STILL, amoledHome.effectType)
        assertEquals(-35, amoledHome.stillOffsetYPercent)

        val amoledLock = ScreenConfig.createPreset(ScreenConfig.PRESET_AMOLED, isLockScreen = true)
        assertTrue(amoledLock.amoledMode)
        assertEquals(35, amoledLock.stillOffsetYPercent)

        val immersive = ScreenConfig.createPreset(ScreenConfig.PRESET_IMMERSIVE, isLockScreen = false)
        assertEquals(ScreenConfig.EFFECT_BLUR, immersive.effectType)
        assertEquals(65, immersive.blurPercent)
        assertTrue(immersive.scrimEnabled)

        val poster = ScreenConfig.createPreset(ScreenConfig.PRESET_POSTER, isLockScreen = false)
        assertEquals(ScreenConfig.EFFECT_BLUR_STILL, poster.effectType)
        assertEquals(94, poster.stillScalePercent)
        assertTrue(poster.scrimEnabled)
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
