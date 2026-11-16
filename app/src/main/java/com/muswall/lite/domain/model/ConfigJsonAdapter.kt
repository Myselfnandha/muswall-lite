package com.muswall.lite.domain.model

/**
 * Pure Kotlin JSON serializer and parser for ScreenConfig and application settings.
 * Operates without external libraries or Android SDK stubs, ensuring 100% JVM testability.
 */
object ConfigJsonAdapter {

    fun configToJson(config: ScreenConfig): String {
        return buildString {
            append("{\n")
            append("  \"effectType\": ").append(config.effectType).append(",\n")
            append("  \"blurPercent\": ").append(config.blurPercent).append(",\n")
            append("  \"dimPercent\": ").append(config.dimPercent).append(",\n")
            append("  \"stillScalePercent\": ").append(config.stillScalePercent).append(",\n")
            append("  \"stillOffsetYPercent\": ").append(config.stillOffsetYPercent).append(",\n")
            append("  \"stillCornerRadiusPercent\": ").append(config.stillCornerRadiusPercent).append(",\n")
            append("  \"applyEffect\": ").append(config.applyEffect).append(",\n")
            append("  \"restoreEffect\": ").append(config.restoreEffect).append(",\n")
            append("  \"restoreDelaySeconds\": ").append(config.restoreDelaySeconds).append(",\n")
            append("  \"applyEffectsToRestore\": ").append(config.applyEffectsToRestore).append(",\n")
            append("  \"restoreBlurPercent\": ").append(config.restoreBlurPercent).append(",\n")
            append("  \"restoreDimPercent\": ").append(config.restoreDimPercent).append(",\n")
            append("  \"wallpaperOffsetYPercent\": ").append(config.wallpaperOffsetYPercent).append(",\n")
            append("  \"wallpaperOffsetXPercent\": ").append(config.wallpaperOffsetXPercent).append(",\n")
            append("  \"autoHorizontalFraming\": ").append(config.autoHorizontalFraming).append(",\n")
            append("  \"aspectFitMode\": ").append(config.aspectFitMode).append(",\n")
            append("  \"amoledMode\": ").append(config.amoledMode).append(",\n")
            append("  \"scrimEnabled\": ").append(config.scrimEnabled).append(",\n")
            append("  \"showTrackInfo\": ").append(config.showTrackInfo).append(",\n")
            append("  \"trackInfoOffsetYPercent\": ").append(config.trackInfoOffsetYPercent).append("\n")
            append("}")
        }
    }

    fun exportBackupJson(separateScreens: Boolean, home: ScreenConfig, lock: ScreenConfig): String {
        return buildString {
            append("{\n")
            append("  \"version\": 22,\n")
            append("  \"separate_screens\": ").append(separateScreens).append(",\n")
            append("  \"home\": ").append(configToJson(home).prependIndent("  ").trimStart()).append(",\n")
            append("  \"lock\": ").append(configToJson(lock).prependIndent("  ").trimStart()).append("\n")
            append("}")
        }
    }

    fun parseConfig(json: String, isLock: Boolean): ScreenConfig {
        val defaultOffset = if (isLock) 35 else -35
        val map = parseSimpleJsonMap(json)
        return ScreenConfig(
            effectType = map["effectType"]?.toIntOrNull() ?: ScreenConfig.EFFECT_BLUR_STILL,
            blurPercent = map["blurPercent"]?.toIntOrNull() ?: 45,
            dimPercent = map["dimPercent"]?.toIntOrNull() ?: 20,
            stillScalePercent = map["stillScalePercent"]?.toIntOrNull() ?: 80,
            stillOffsetYPercent = map["stillOffsetYPercent"]?.toIntOrNull() ?: defaultOffset,
            stillCornerRadiusPercent = map["stillCornerRadiusPercent"]?.toIntOrNull() ?: 12,
            applyEffect = map["applyEffect"]?.toIntOrNull() ?: ScreenConfig.TRANSITION_FADE,
            restoreEffect = map["restoreEffect"]?.toIntOrNull() ?: ScreenConfig.TRANSITION_FADE,
            restoreDelaySeconds = map["restoreDelaySeconds"]?.toIntOrNull() ?: 30,
            applyEffectsToRestore = map["applyEffectsToRestore"]?.toBooleanStrictOrNull() ?: false,
            restoreBlurPercent = map["restoreBlurPercent"]?.toIntOrNull() ?: 45,
            restoreDimPercent = map["restoreDimPercent"]?.toIntOrNull() ?: 20,
            wallpaperOffsetYPercent = map["wallpaperOffsetYPercent"]?.toIntOrNull() ?: defaultOffset,
            wallpaperOffsetXPercent = map["wallpaperOffsetXPercent"]?.toIntOrNull() ?: 0,
            autoHorizontalFraming = map["autoHorizontalFraming"]?.toBooleanStrictOrNull() ?: true,
            aspectFitMode = map["aspectFitMode"]?.toIntOrNull() ?: ScreenConfig.ASPECT_FIT_WIDTH,
            amoledMode = map["amoledMode"]?.toBooleanStrictOrNull() ?: false,
            scrimEnabled = map["scrimEnabled"]?.toBooleanStrictOrNull() ?: false,
            showTrackInfo = map["showTrackInfo"]?.toBooleanStrictOrNull() ?: false,
            trackInfoOffsetYPercent = map["trackInfoOffsetYPercent"]?.toIntOrNull() ?: 25
        )
    }

    fun parseBackup(json: String): Triple<Boolean, ScreenConfig, ScreenConfig>? {
        return try {
            val separateScreens = Regex("\"separate_screens\"\\s*:\\s*(true|false)").find(json)?.groupValues?.get(1)?.toBoolean() ?: false

            val homeRegex = Regex("\"home\"\\s*:\\s*\\{([^}]+)\\}")
            val homeMatch = homeRegex.find(json)?.groupValues?.get(1) ?: return null
            val homeConfig = parseConfig("{$homeMatch}", isLock = false)

            val lockRegex = Regex("\"lock\"\\s*:\\s*\\{([^}]+)\\}")
            val lockMatch = lockRegex.find(json)?.groupValues?.get(1) ?: return null
            val lockConfig = parseConfig("{$lockMatch}", isLock = true)

            Triple(separateScreens, homeConfig, lockConfig)
        } catch (_: Throwable) {
            null
        }
    }

    private fun parseSimpleJsonMap(json: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val regex = Regex("\"([a-zA-Z0-9_]+)\"\\s*:\\s*([^,\n}]+)")
        for (match in regex.findAll(json)) {
            val key = match.groupValues[1].trim()
            val value = match.groupValues[2].trim().trim('"', '\'')
            result[key] = value
        }
        return result
    }
}
