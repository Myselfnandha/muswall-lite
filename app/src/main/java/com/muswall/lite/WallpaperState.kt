package com.muswall.lite

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

data class ScreenConfig(
    val effectType: Int = EFFECT_COVER_CARD,
    val blurPercent: Int = 45,
    val dimPercent: Int = 20,
    val coverPosition: Int = POSITION_CENTER,
    val coverSizePercent: Int = 68,
    val coverYOffsetPercent: Int = 0,
    val coverCornerRadiusDp: Int = 24,
    val applyEffect: Int = TRANSITION_FADE,
    val restoreEffect: Int = TRANSITION_FADE,
    val restoreDelaySeconds: Int = 30
) {
    companion object {
        const val EFFECT_COVER_CARD = 0
        const val EFFECT_FULL_BLEED = 1
        const val EFFECT_STRETCHED_BLUR = 2

        const val POSITION_TOP = 0
        const val POSITION_CENTER = 1
        const val POSITION_BOTTOM = 2
        const val POSITION_CUSTOM = 3

        const val TRANSITION_FADE = 0
        const val TRANSITION_ZOOM = 1
        const val TRANSITION_NONE = 2
    }
}

object WallpaperState {
    private const val PREFS_NAME = "muswall_lite_prefs"
    private const val RESTORE_IMAGE_NAME = "restore_wallpaper.png"

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var restoreJob: Job? = null

    private val _currentArt = MutableStateFlow<Bitmap?>(null)
    val currentArt = _currentArt.asStateFlow()

    private val _restoreArt = MutableStateFlow<Bitmap?>(null)
    val restoreArt = _restoreArt.asStateFlow()

    private val _isRestoreActive = MutableStateFlow(false)
    val isRestoreActive = _isRestoreActive.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _trackTitle = MutableStateFlow<String?>(null)
    val trackTitle = _trackTitle.asStateFlow()

    private val _settingsChanged = MutableStateFlow(0L)
    val settingsChanged = _settingsChanged.asStateFlow()

    private val _homeConfig = MutableStateFlow(ScreenConfig())
    val homeConfig = _homeConfig.asStateFlow()

    private val _lockConfig = MutableStateFlow(ScreenConfig())
    val lockConfig = _lockConfig.asStateFlow()

    private val _separateScreens = MutableStateFlow(false)
    val separateScreens = _separateScreens.asStateFlow()

    fun init(context: Context) {
        loadConfigs(context)
        loadRestoreImage(context)
    }

    fun updateMedia(context: Context, art: Bitmap?, playing: Boolean, title: String?) {
        _currentArt.value = art
        _isPlaying.value = playing
        _trackTitle.value = title

        val config = getEffectiveConfig(context, isLockScreen = false)

        if (playing) {
            restoreJob?.cancel()
            _isRestoreActive.value = false
        } else {
            if (config.restoreDelaySeconds == -1) {
                _isRestoreActive.value = false
            } else if (config.restoreDelaySeconds == 0) {
                _isRestoreActive.value = true
            } else {
                restoreJob?.cancel()
                restoreJob = scope.launch {
                    delay(config.restoreDelaySeconds * 1000L)
                    _isRestoreActive.value = true
                }
            }
        }
    }

    fun setTestPlayback(context: Context, playing: Boolean) {
        updateMedia(context, _currentArt.value, playing, if (playing) "Playing Test Audio" else "Playback Paused")
    }

    fun notifySettingsChanged() {
        _settingsChanged.value = System.currentTimeMillis()
    }

    fun getEffectiveConfig(context: Context, isLockScreen: Boolean): ScreenConfig {
        return if (_separateScreens.value && isLockScreen) {
            _lockConfig.value
        } else {
            _homeConfig.value
        }
    }

    fun updateHomeConfig(context: Context, config: ScreenConfig) {
        _homeConfig.value = config
        saveConfig(context, "home", config)
        notifySettingsChanged()
    }

    fun updateLockConfig(context: Context, config: ScreenConfig) {
        _lockConfig.value = config
        saveConfig(context, "lock", config)
        notifySettingsChanged()
    }

    fun setSeparateScreens(context: Context, enabled: Boolean) {
        _separateScreens.value = enabled
        getPrefs(context).edit().putBoolean("separate_screens", enabled).apply()
        notifySettingsChanged()
    }

    fun setRestoreBitmap(context: Context, bitmap: Bitmap?) {
        _restoreArt.value = bitmap
        if (bitmap != null) {
            saveRestoreImage(context, bitmap)
        } else {
            val file = File(context.filesDir, RESTORE_IMAGE_NAME)
            if (file.exists()) file.delete()
        }
        notifySettingsChanged()
    }

    private fun loadConfigs(context: Context) {
        val prefs = getPrefs(context)
        _separateScreens.value = prefs.getBoolean("separate_screens", false)
        _homeConfig.value = readConfig(prefs, "home")
        _lockConfig.value = readConfig(prefs, "lock")
    }

    private fun readConfig(prefs: SharedPreferences, prefix: String): ScreenConfig {
        return ScreenConfig(
            effectType = prefs.getInt("${prefix}_effect_type", ScreenConfig.EFFECT_COVER_CARD),
            blurPercent = prefs.getInt("${prefix}_blur_percent", 45),
            dimPercent = prefs.getInt("${prefix}_dim_percent", 20),
            coverPosition = prefs.getInt("${prefix}_cover_position", ScreenConfig.POSITION_CENTER),
            coverSizePercent = prefs.getInt("${prefix}_cover_size", 68),
            coverYOffsetPercent = prefs.getInt("${prefix}_cover_y_offset", 0),
            coverCornerRadiusDp = prefs.getInt("${prefix}_cover_radius", 24),
            applyEffect = prefs.getInt("${prefix}_apply_effect", ScreenConfig.TRANSITION_FADE),
            restoreEffect = prefs.getInt("${prefix}_restore_effect", ScreenConfig.TRANSITION_FADE),
            restoreDelaySeconds = prefs.getInt("${prefix}_restore_delay", 30)
        )
    }

    private fun saveConfig(context: Context, prefix: String, config: ScreenConfig) {
        getPrefs(context).edit().apply {
            putInt("${prefix}_effect_type", config.effectType)
            putInt("${prefix}_blur_percent", config.blurPercent)
            putInt("${prefix}_dim_percent", config.dimPercent)
            putInt("${prefix}_cover_position", config.coverPosition)
            putInt("${prefix}_cover_size", config.coverSizePercent)
            putInt("${prefix}_cover_y_offset", config.coverYOffsetPercent)
            putInt("${prefix}_cover_radius", config.coverCornerRadiusDp)
            putInt("${prefix}_apply_effect", config.applyEffect)
            putInt("${prefix}_restore_effect", config.restoreEffect)
            putInt("${prefix}_restore_delay", config.restoreDelaySeconds)
            apply()
        }
    }

    private fun saveRestoreImage(context: Context, bitmap: Bitmap) {
        scope.launch(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, RESTORE_IMAGE_NAME)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun loadRestoreImage(context: Context) {
        scope.launch(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, RESTORE_IMAGE_NAME)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    _restoreArt.value = bitmap
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
