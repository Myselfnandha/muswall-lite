package com.muswall.lite.ui.viewmodel

import android.app.WallpaperManager
import android.content.Context
import android.net.Uri
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muswall.lite.WallpaperState
import com.muswall.lite.domain.model.ScreenConfig
import com.muswall.lite.ui.util.PreviewUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _isNotificationGranted = MutableStateFlow(true)
    val isNotificationGranted = _isNotificationGranted.asStateFlow()

    private val _isWallpaperActive = MutableStateFlow(false)
    val isWallpaperActive = _isWallpaperActive.asStateFlow()

    private val _localHomeConfig = MutableStateFlow(ScreenConfig())
    val localHomeConfig = _localHomeConfig.asStateFlow()

    private val _localLockConfig = MutableStateFlow(ScreenConfig())
    val localLockConfig = _localLockConfig.asStateFlow()

    private val _localSeparateScreens = MutableStateFlow(false)
    val localSeparateScreens = _localSeparateScreens.asStateFlow()

    private val _activeTab = MutableStateFlow(0)
    val activeTab = _activeTab.asStateFlow()

    private val _recoverTab = MutableStateFlow(0)
    val recoverTab = _recoverTab.asStateFlow()

    private val _previewShowingRestore = MutableStateFlow(false)
    val previewShowingRestore = _previewShowingRestore.asStateFlow()

    private val _previewAnimProgress = MutableStateFlow(1f)
    val previewAnimProgress = _previewAnimProgress.asStateFlow()

    init {
        viewModelScope.launch {
            WallpaperState.homeConfig.collect {
                _localHomeConfig.value = it
            }
        }
        viewModelScope.launch {
            WallpaperState.lockConfig.collect {
                _localLockConfig.value = it
            }
        }
        viewModelScope.launch {
            WallpaperState.separateScreens.collect {
                _localSeparateScreens.value = it
            }
        }
    }

    fun refreshStatus(context: Context) {
        _isNotificationGranted.value = checkNotificationAccess(context)
        _isWallpaperActive.value = checkWallpaperActive(context)
    }

    fun setActiveTab(tab: Int) {
        _activeTab.value = tab
    }

    fun setRecoverTab(tab: Int) {
        _recoverTab.value = tab
    }

    fun updateHomeConfig(context: Context, newConfig: ScreenConfig) {
        _localHomeConfig.value = newConfig
        WallpaperState.updateHomeConfig(context, newConfig)
        if (!_localSeparateScreens.value) {
            _localLockConfig.value = newConfig
            WallpaperState.updateLockConfig(context, newConfig)
        }
    }

    fun updateLockConfig(context: Context, newConfig: ScreenConfig) {
        _localLockConfig.value = newConfig
        WallpaperState.updateLockConfig(context, newConfig)
    }

    fun updateCurrentConfig(context: Context, newConfig: ScreenConfig) {
        if (_localSeparateScreens.value && _activeTab.value == 1) {
            updateLockConfig(context, newConfig)
        } else {
            updateHomeConfig(context, newConfig)
        }
    }

    fun setSeparateScreens(context: Context, enabled: Boolean) {
        _localSeparateScreens.value = enabled
        if (!enabled && _activeTab.value == 1) {
            _activeTab.value = 0
        }
        if (!enabled && _recoverTab.value == 1) {
            _recoverTab.value = 0
        }
        WallpaperState.setSeparateScreens(context, enabled)
    }

    fun applyAndSaveChanges(context: Context) {
        WallpaperState.updateHomeConfig(context, _localHomeConfig.value)
        WallpaperState.updateLockConfig(context, _localLockConfig.value)
        WallpaperState.setSeparateScreens(context, _localSeparateScreens.value)
    }

    fun discardChanges() {
        _localHomeConfig.value = WallpaperState.homeConfig.value
        _localLockConfig.value = WallpaperState.lockConfig.value
        _localSeparateScreens.value = WallpaperState.separateScreens.value
    }

    fun playPreviewTransition(targetRestoreState: Boolean) {
        viewModelScope.launch {
            _previewShowingRestore.value = targetRestoreState
            val frames = 18
            val delayMs = 16L
            for (i in 1..frames) {
                val t = i / frames.toFloat()
                _previewAnimProgress.value = 1f - (1f - t) * (1f - t)
                delay(delayMs)
            }
            _previewAnimProgress.value = 1f
        }
    }

    fun handleImagePicked(context: Context, uri: Uri, isLockScreen: Boolean) {
        val bmp = PreviewUtils.loadSoftwareBitmapFromUri(context, uri) ?: return
        if (_localSeparateScreens.value) {
            if (isLockScreen) {
                WallpaperState.setLockRestoreBitmap(context, bmp)
            } else {
                WallpaperState.setHomeRestoreBitmap(context, bmp)
            }
        } else {
            WallpaperState.setUnifiedRestoreBitmap(context, bmp)
        }
    }

    private fun checkNotificationAccess(context: Context): Boolean {
        return try {
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        } catch (_: Throwable) {
            false
        }
    }

    private fun checkWallpaperActive(context: Context): Boolean {
        return try {
            val wm = WallpaperManager.getInstance(context)
            wm.wallpaperInfo?.packageName == context.packageName
        } catch (_: Throwable) {
            false
        }
    }
}
