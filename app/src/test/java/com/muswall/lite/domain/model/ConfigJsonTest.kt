package com.muswall.lite.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigJsonTest {

    @Test
    fun testJsonSerializationAndDeserialization() {
        val original = ScreenConfig(
            effectType = ScreenConfig.EFFECT_BLUR,
            blurPercent = 60,
            dimPercent = 30,
            stillScalePercent = 90,
            stillOffsetYPercent = -20,
            stillCornerRadiusPercent = 16,
            applyEffect = ScreenConfig.TRANSITION_ZOOM,
            restoreEffect = ScreenConfig.TRANSITION_SLIDE_UP,
            restoreDelaySeconds = 15,
            applyEffectsToRestore = true,
            restoreBlurPercent = 50,
            restoreDimPercent = 25,
            wallpaperOffsetYPercent = 10,
            wallpaperOffsetXPercent = -15,
            autoHorizontalFraming = false,
            aspectFitMode = ScreenConfig.ASPECT_FILL,
            amoledMode = true,
            scrimEnabled = true,
            showTrackInfo = true,
            trackInfoOffsetYPercent = 40
        )

        val jsonStr = ConfigJsonAdapter.configToJson(original)
        val parsed = ConfigJsonAdapter.parseConfig(jsonStr, isLock = false)

        assertEquals(original, parsed)
    }

    @Test
    fun testBackupExportAndImport() {
        val home = ScreenConfig(effectType = ScreenConfig.EFFECT_STILL, amoledMode = true)
        val lock = ScreenConfig(effectType = ScreenConfig.EFFECT_BLUR, blurPercent = 70)

        val backupJson = ConfigJsonAdapter.exportBackupJson(separateScreens = true, home = home, lock = lock)
        val parsed = ConfigJsonAdapter.parseBackup(backupJson)

        assertNotNull(parsed)
        assertTrue(parsed!!.first)
        assertEquals(home, parsed.second)
        assertEquals(lock, parsed.third)
    }
}
