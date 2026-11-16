package com.muswall.lite

import android.content.Context
import android.graphics.Bitmap
import com.muswall.lite.data.repository.RestoreImageRepository
import com.muswall.lite.data.repository.WallpaperConfigRepository
import com.muswall.lite.domain.model.ScreenConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

typealias ScreenConfig = com.muswall.lite.domain.model.ScreenConfig

/**
 * Global reactive state facade for MusWall Lite.
 * Orchestrates playback state, blur precomputations, restore delay timers,
 * and delegates persistence to [WallpaperConfigRepository] and [RestoreImageRepository].
 */
object WallpaperState {

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var homeRestoreJob: Job? = null
    private var lockRestoreJob: Job? = null
    private var precomputeJob: Job? = null

    private var appContext: Context? = null
    private var configRepository: WallpaperConfigRepository? = null
    private var restoreRepository: RestoreImageRepository? = null

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

    private val _isHomeRestoreActive = MutableStateFlow(true)
    val isHomeRestoreActive = _isHomeRestoreActive.asStateFlow()

    private val _isLockRestoreActive = MutableStateFlow(true)
    val isLockRestoreActive = _isLockRestoreActive.asStateFlow()

    val isRestoreActive = _isHomeRestoreActive.asStateFlow()

    fun isRestoreActive(isLockScreen: Boolean): Boolean {
        if (_currentArt.value == null) {
            return true
        }
        return if (_separateScreens.value && isLockScreen) {
            _isLockRestoreActive.value
        } else {
            _isHomeRestoreActive.value
        }
    }

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
        val appCtx = context.applicationContext
        appContext = appCtx
        val configRepo = WallpaperConfigRepository(appCtx).also { configRepository = it }
        val restoreRepo = RestoreImageRepository(appCtx).also { restoreRepository = it }

        _separateScreens.value = configRepo.isSeparateScreens()
        _homeConfig.value = configRepo.getHomeConfig()
        _lockConfig.value = configRepo.getLockConfig()

        if (!_isPlaying.value) {
            _isHomeRestoreActive.value = true
            _isLockRestoreActive.value = true
        }

        loadRestoreImages(appCtx, restoreRepo)
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

        if (playing && art != null) {
            homeRestoreJob?.cancel()
            homeRestoreJob = null
            lockRestoreJob?.cancel()
            lockRestoreJob = null
            _isHomeRestoreActive.value = false
            _isLockRestoreActive.value = false
        } else if (art == null) {
            // If no still from playing media, leave it default wallpaper
            homeRestoreJob?.cancel()
            homeRestoreJob = null
            lockRestoreJob?.cancel()
            lockRestoreJob = null
            _isHomeRestoreActive.value = true
            _isLockRestoreActive.value = true
        } else {
            armRestoreJob(false)
            if (_separateScreens.value) {
                armRestoreJob(true)
            } else {
                _isLockRestoreActive.value = _isHomeRestoreActive.value
            }
        }
    }

    fun onMediaAppClosed(context: Context) {
        homeRestoreJob?.cancel()
        homeRestoreJob = null
        lockRestoreJob?.cancel()
        lockRestoreJob = null
        _isPlaying.value = false
        _currentArt.value = null
        _trackTitle.value = null
        _isHomeRestoreActive.value = true
        _isLockRestoreActive.value = true
        notifySettingsChanged()
    }

    private fun armRestoreJob(isLockScreen: Boolean) {
        val config = if (_separateScreens.value && isLockScreen) _lockConfig.value else _homeConfig.value
        val flow = if (_separateScreens.value && isLockScreen) _isLockRestoreActive else _isHomeRestoreActive

        if (isLockScreen) {
            lockRestoreJob?.cancel()
            lockRestoreJob = null
        } else {
            homeRestoreJob?.cancel()
            homeRestoreJob = null
        }

        when (config.restoreDelaySeconds) {
            -1 -> {
                flow.value = false
            }
            0 -> {
                flow.value = true
            }
            else -> {
                val delayMs = config.restoreDelaySeconds * 1000L
                val job = scope.launch {
                    delay(delayMs)
                    flow.value = true
                    if (isLockScreen) lockRestoreJob = null else homeRestoreJob = null
                }
                if (isLockScreen) lockRestoreJob = job else homeRestoreJob = job
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

    fun getDefaultRestoreArt(isLockScreen: Boolean): Bitmap {
        val repo = restoreRepository ?: appContext?.let { RestoreImageRepository(it) }
        return repo?.getDefaultArtwork(isLockScreen) ?: RestoreImageRepository.createAmbientGradientArtwork(isLockScreen)
    }

    fun getRestoreArt(isLockScreen: Boolean): Bitmap {
        val custom = if (_separateScreens.value && isLockScreen) {
            _lockRestoreArt.value ?: _homeRestoreArt.value
        } else {
            _homeRestoreArt.value
        }
        return (custom?.takeUnless { it.isRecycled }) ?: getDefaultRestoreArt(isLockScreen)
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
        val delayChanged = _homeConfig.value.restoreDelaySeconds != config.restoreDelaySeconds
        _homeConfig.value = config
        val repo = configRepository ?: WallpaperConfigRepository(context.applicationContext)
        repo.saveHomeConfig(config)
        if (!_isPlaying.value && delayChanged) {
            armRestoreJob(false)
        }
        notifySettingsChanged()
    }

    fun updateLockConfig(context: Context, config: ScreenConfig) {
        val delayChanged = _lockConfig.value.restoreDelaySeconds != config.restoreDelaySeconds
        _lockConfig.value = config
        val repo = configRepository ?: WallpaperConfigRepository(context.applicationContext)
        repo.saveLockConfig(config)
        if (!_isPlaying.value && delayChanged && _separateScreens.value) {
            armRestoreJob(true)
        }
        notifySettingsChanged()
    }

    fun setSeparateScreens(context: Context, enabled: Boolean) {
        _separateScreens.value = enabled
        val repo = configRepository ?: WallpaperConfigRepository(context.applicationContext)
        repo.setSeparateScreens(enabled)
        notifySettingsChanged()
    }

    fun setHomeRestoreBitmap(context: Context, bitmap: Bitmap?) {
        _homeRestoreArt.value = bitmap
        val repo = restoreRepository ?: RestoreImageRepository(context.applicationContext)
        scope.launch {
            repo.saveHomeRestoreImage(bitmap)
        }
        notifySettingsChanged()
    }

    fun setLockRestoreBitmap(context: Context, bitmap: Bitmap?) {
        _lockRestoreArt.value = bitmap
        val repo = restoreRepository ?: RestoreImageRepository(context.applicationContext)
        scope.launch {
            repo.saveLockRestoreImage(bitmap)
        }
        notifySettingsChanged()
    }

    fun setUnifiedRestoreBitmap(context: Context, bitmap: Bitmap?) {
        _homeRestoreArt.value = bitmap
        _lockRestoreArt.value = bitmap
        val repo = restoreRepository ?: RestoreImageRepository(context.applicationContext)
        scope.launch {
            repo.saveHomeRestoreImage(bitmap)
            repo.saveLockRestoreImage(bitmap)
        }
        notifySettingsChanged()
    }

    private var systemCaptureAttempted = false

    fun captureSystemDefaultWallpaper(context: Context) {
        if (systemCaptureAttempted) return
        systemCaptureAttempted = true
        val repo = restoreRepository ?: RestoreImageRepository(context.applicationContext)
        scope.launch {
            val bmp = repo.captureSystemWallpaper()
            if (bmp != null) {
                setUnifiedRestoreBitmap(context, bmp)
            }
        }
    }

    private fun schedulePrecomputeBlur() {
        precomputeJob?.cancel()
        precomputeJob = scope.launch(Dispatchers.Default) {
            try {
                val art = _currentArt.value
                val home = _homeConfig.value
                val lock = _lockConfig.value

                if (art != null && !art.isRecycled) {
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
                } else {
                    _preparedHomeBlur.value = null
                    _preparedLockBlur.value = null
                }

                val homeRestore = _homeRestoreArt.value
                if (homeRestore != null && !homeRestore.isRecycled && home.applyEffectsToRestore && home.restoreBlurPercent > 0) {
                    _preparedHomeRestoreBlur.value = WallpaperRenderer.getNormalBlur(homeRestore, home.restoreBlurPercent)
                } else {
                    _preparedHomeRestoreBlur.value = null
                }

                val lockRestore = if (_separateScreens.value) (_lockRestoreArt.value ?: homeRestore) else homeRestore
                if (lockRestore != null && !lockRestore.isRecycled && lock.applyEffectsToRestore && lock.restoreBlurPercent > 0) {
                    _preparedLockRestoreBlur.value = WallpaperRenderer.getNormalBlur(lockRestore, lock.restoreBlurPercent)
                } else {
                    _preparedLockRestoreBlur.value = null
                }
            } catch (_: Throwable) {
            }
        }
    }

    private fun loadRestoreImages(context: Context, repo: RestoreImageRepository) {
        scope.launch(Dispatchers.IO) {
            val homeBmp = repo.loadHomeRestoreImage()
            if (homeBmp != null) {
                _homeRestoreArt.value = homeBmp
            }

            val lockBmp = repo.loadLockRestoreImage()
            if (lockBmp != null) {
                _lockRestoreArt.value = lockBmp
            }

            if (_homeRestoreArt.value == null && _lockRestoreArt.value == null) {
                captureSystemDefaultWallpaper(context)
            } else {
                schedulePrecomputeBlur()
            }
        }
    }
}
