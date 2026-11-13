package com.muswall.lite

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Build
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
    val effectType: Int = EFFECT_BLUR,
    val blurPercent: Int = 45,
    val dimPercent: Int = 20,
    val stillScalePercent: Int = 80,
    val stillOffsetYPercent: Int = -25,
    val stillCornerRadiusPercent: Int = 12,
    val applyEffect: Int = TRANSITION_FADE,
    val restoreEffect: Int = TRANSITION_FADE,
    val restoreDelaySeconds: Int = 30,
    val applyEffectsToRestore: Boolean = false,
    val restoreBlurPercent: Int = 45,
    val restoreDimPercent: Int = 20
) {
    companion object {
        const val EFFECT_BLUR = 0
        const val EFFECT_STILL = 1
        const val EFFECT_BLUR_STILL = 2

        const val TRANSITION_FADE = 0
        const val TRANSITION_ZOOM = 1
        const val TRANSITION_SLIDE_UP = 2
        const val TRANSITION_SLIDE_DOWN = 3
        const val TRANSITION_RADIAL = 4
        const val TRANSITION_BLUR_DISSOLVE = 5
        const val TRANSITION_NONE = 6
    }
}

object WallpaperState {
    private const val PREFS_NAME = "muswall_lite_prefs"
    private const val HOME_RESTORE_IMAGE = "home_restore_wallpaper.png"
    private const val LOCK_RESTORE_IMAGE = "lock_restore_wallpaper.png"

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var restoreJob: Job? = null
    private var precomputeJob: Job? = null

    private val _currentArt = MutableStateFlow<Bitmap?>(null)
    val currentArt = _currentArt.asStateFlow()

    private val _homeRestoreArt = MutableStateFlow<Bitmap?>(null)
    val homeRestoreArt = _homeRestoreArt.asStateFlow()

    private val _lockRestoreArt = MutableStateFlow<Bitmap?>(null)
    val lockRestoreArt = _lockRestoreArt.asStateFlow()

    private val _preparedHomeBlur = MutableStateFlow<Bitmap?>(null)
    val preparedHomeBlur = _preparedHomeBlur.asStateFlow()

    private val _preparedLockBlur = MutableStateFlow<Bitmap?>(null)
    val preparedLockBlur = _preparedLockBlur.asStateFlow()

    private val _preparedHomeRestoreBlur = MutableStateFlow<Bitmap?>(null)
    val preparedHomeRestoreBlur = _preparedHomeRestoreBlur.asStateFlow()

    private val _preparedLockRestoreBlur = MutableStateFlow<Bitmap?>(null)
    val preparedLockRestoreBlur = _preparedLockRestoreBlur.asStateFlow()

    private val _isRestoreActive = MutableStateFlow(true)
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
        loadRestoreImages(context)
        if (!_isPlaying.value) {
            _isRestoreActive.value = true
        }
    }

    fun updateMedia(context: Context, art: Bitmap?, playing: Boolean, title: String?) {
        val current = _currentArt.value
        val artChanged = when {
            art == null && current != null -> true
            art != null && current == null -> true
            art != null && current != null -> !art.sameAs(current)
            else -> false
        }

        if (artChanged) {
            _currentArt.value = art
            schedulePrecomputeBlur()
        }

        _isPlaying.value = playing
        _trackTitle.value = title

        val config = getEffectiveConfig(context, isLockScreen = false)

        if (playing) {
            restoreJob?.cancel()
            restoreJob = null
            _isRestoreActive.value = false
        } else {
            if (config.restoreDelaySeconds == -1) {
                _isRestoreActive.value = false
            } else if (config.restoreDelaySeconds == 0) {
                _isRestoreActive.value = true
            } else if (!_isRestoreActive.value && restoreJob == null) {
                restoreJob = scope.launch {
                    delay(config.restoreDelaySeconds * 1000L)
                    _isRestoreActive.value = true
                    restoreJob = null
                }
            }
        }
    }

    fun setTestPlayback(context: Context, playing: Boolean) {
        updateMedia(context, _currentArt.value, playing, if (playing) "Playing Test Audio" else "Playback Paused")
    }

    fun notifySettingsChanged() {
        schedulePrecomputeBlur()
        _settingsChanged.value = System.currentTimeMillis()
    }

    fun getEffectiveConfig(context: Context, isLockScreen: Boolean): ScreenConfig {
        return if (_separateScreens.value && isLockScreen) {
            _lockConfig.value
        } else {
            _homeConfig.value
        }
    }

    fun getRestoreArt(isLockScreen: Boolean): Bitmap? {
        return if (_separateScreens.value && isLockScreen) {
            _lockRestoreArt.value ?: _homeRestoreArt.value
        } else {
            _homeRestoreArt.value
        }
    }

    fun getPreparedBlur(isLockScreen: Boolean): Bitmap? {
        return if (_separateScreens.value && isLockScreen) {
            _preparedLockBlur.value ?: _preparedHomeBlur.value
        } else {
            _preparedHomeBlur.value
        }
    }

    fun getPreparedRestoreBlur(isLockScreen: Boolean): Bitmap? {
        return if (_separateScreens.value && isLockScreen) {
            _preparedLockRestoreBlur.value ?: _preparedHomeRestoreBlur.value
        } else {
            _preparedHomeRestoreBlur.value
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

    fun setHomeRestoreBitmap(context: Context, bitmap: Bitmap?) {
        _homeRestoreArt.value = bitmap
        saveRestoreImageFile(context, HOME_RESTORE_IMAGE, bitmap)
        notifySettingsChanged()
    }

    fun setLockRestoreBitmap(context: Context, bitmap: Bitmap?) {
        _lockRestoreArt.value = bitmap
        saveRestoreImageFile(context, LOCK_RESTORE_IMAGE, bitmap)
        notifySettingsChanged()
    }

    fun setUnifiedRestoreBitmap(context: Context, bitmap: Bitmap?) {
        _homeRestoreArt.value = bitmap
        _lockRestoreArt.value = bitmap
        saveRestoreImageFile(context, HOME_RESTORE_IMAGE, bitmap)
        saveRestoreImageFile(context, LOCK_RESTORE_IMAGE, bitmap)
        notifySettingsChanged()
    }

    private var systemCaptureAttempted = false

    @SuppressLint("MissingPermission")
    fun captureSystemDefaultWallpaper(context: Context) {
        if (systemCaptureAttempted) return
        systemCaptureAttempted = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return
        }
        scope.launch(Dispatchers.IO) {
            try {
                val wm = WallpaperManager.getInstance(context)
                val drawable = wm.drawable ?: wm.builtInDrawable
                if (drawable != null) {
                    val w = drawable.intrinsicWidth.coerceIn(1080, 1440)
                    val h = drawable.intrinsicHeight.coerceIn(1920, 3200)
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    drawable.setBounds(0, 0, w, h)
                    drawable.draw(canvas)
                    setUnifiedRestoreBitmap(context, bmp)
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun schedulePrecomputeBlur() {
        precomputeJob?.cancel()
        precomputeJob = scope.launch(Dispatchers.Default) {
            val art = _currentArt.value
            val home = _homeConfig.value
            val lock = _lockConfig.value

            if (art != null) {
                _preparedHomeBlur.value = if (home.effectType != ScreenConfig.EFFECT_STILL && home.blurPercent > 0) {
                    WallpaperRenderer.getNormalBlur(art, home.blurPercent)
                } else null

                if (_separateScreens.value && (lock.effectType != home.effectType || lock.blurPercent != home.blurPercent)) {
                    _preparedLockBlur.value = if (lock.effectType != ScreenConfig.EFFECT_STILL && lock.blurPercent > 0) {
                        WallpaperRenderer.getNormalBlur(art, lock.blurPercent)
                    } else null
                } else {
                    _preparedLockBlur.value = _preparedHomeBlur.value
                }
            }

            val homeRestore = _homeRestoreArt.value
            if (homeRestore != null && home.applyEffectsToRestore && home.restoreBlurPercent > 0) {
                _preparedHomeRestoreBlur.value = WallpaperRenderer.getNormalBlur(homeRestore, home.restoreBlurPercent)
            } else {
                _preparedHomeRestoreBlur.value = null
            }

            val lockRestore = if (_separateScreens.value) (_lockRestoreArt.value ?: homeRestore) else homeRestore
            if (lockRestore != null && lock.applyEffectsToRestore && lock.restoreBlurPercent > 0) {
                _preparedLockRestoreBlur.value = WallpaperRenderer.getNormalBlur(lockRestore, lock.restoreBlurPercent)
            } else {
                _preparedLockRestoreBlur.value = null
            }
        }
    }

    private fun loadConfigs(context: Context) {
        val prefs = getPrefs(context)
        _separateScreens.value = prefs.getBoolean("separate_screens", false)
        _homeConfig.value = readConfig(prefs, "home")
        _lockConfig.value = readConfig(prefs, "lock")
    }

    private fun readConfig(prefs: SharedPreferences, prefix: String): ScreenConfig {
        return ScreenConfig(
            effectType = prefs.getInt("${prefix}_effect_type", ScreenConfig.EFFECT_BLUR),
            blurPercent = prefs.getInt("${prefix}_blur_percent", 45),
            dimPercent = prefs.getInt("${prefix}_dim_percent", 20),
            stillScalePercent = prefs.getInt("${prefix}_still_scale_percent", 80),
            stillOffsetYPercent = prefs.getInt("${prefix}_still_offset_y_percent", -25),
            stillCornerRadiusPercent = prefs.getInt("${prefix}_still_corner_radius_percent", 12),
            applyEffect = prefs.getInt("${prefix}_apply_effect", ScreenConfig.TRANSITION_FADE),
            restoreEffect = prefs.getInt("${prefix}_restore_effect", ScreenConfig.TRANSITION_FADE),
            restoreDelaySeconds = prefs.getInt("${prefix}_restore_delay", 30),
            applyEffectsToRestore = prefs.getBoolean("${prefix}_apply_effects_to_restore", false),
            restoreBlurPercent = prefs.getInt("${prefix}_restore_blur_percent", 45),
            restoreDimPercent = prefs.getInt("${prefix}_restore_dim_percent", 20)
        )
    }

    private fun saveConfig(context: Context, prefix: String, config: ScreenConfig) {
        getPrefs(context).edit().apply {
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
            apply()
        }
    }

    private fun saveRestoreImageFile(context: Context, filename: String, bitmap: Bitmap?) {
        scope.launch(Dispatchers.IO) {
            val file = File(context.filesDir, filename)
            if (bitmap != null) {
                try {
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
                    }
                } catch (_: Exception) {
                }
            } else {
                if (file.exists()) file.delete()
            }
        }
    }

    private fun loadRestoreImages(context: Context) {
        scope.launch(Dispatchers.IO) {
            val homeFile = File(context.filesDir, HOME_RESTORE_IMAGE)
            if (homeFile.exists()) {
                try {
                    _homeRestoreArt.value = BitmapFactory.decodeFile(homeFile.absolutePath)
                } catch (_: Exception) {
                }
            }

            val lockFile = File(context.filesDir, LOCK_RESTORE_IMAGE)
            if (lockFile.exists()) {
                try {
                    _lockRestoreArt.value = BitmapFactory.decodeFile(lockFile.absolutePath)
                } catch (_: Exception) {
                }
            }

            if (_homeRestoreArt.value == null && _lockRestoreArt.value == null) {
                captureSystemDefaultWallpaper(context)
            } else {
                schedulePrecomputeBlur()
            }
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
