package com.muswall.lite

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object WallpaperState {
    private const val PREFS_NAME = "muswall_lite_prefs"
    private const val KEY_BLUR = "blur_amount"
    private const val KEY_DIM = "dim_amount"
    private const val KEY_MODE = "display_mode"

    const val MODE_EDGE_TO_EDGE = 0
    const val MODE_BLURRED_STRETCH = 1

    private val _currentArt = MutableStateFlow<Bitmap?>(null)
    val currentArt = _currentArt.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _trackTitle = MutableStateFlow<String?>(null)
    val trackTitle = _trackTitle.asStateFlow()

    private val _settingsChanged = MutableStateFlow(0L)
    val settingsChanged = _settingsChanged.asStateFlow()

    fun updateMedia(art: Bitmap?, playing: Boolean, title: String?) {
        _currentArt.value = art
        _isPlaying.value = playing
        _trackTitle.value = title
    }

    fun notifySettingsChanged() {
        _settingsChanged.value = System.currentTimeMillis()
    }

    fun getBlur(context: Context): Int {
        return getPrefs(context).getInt(KEY_BLUR, 40)
    }

    fun setBlur(context: Context, value: Int) {
        getPrefs(context).edit().putInt(KEY_BLUR, value).apply()
        notifySettingsChanged()
    }

    fun getDim(context: Context): Int {
        return getPrefs(context).getInt(KEY_DIM, 25)
    }

    fun setDim(context: Context, value: Int) {
        getPrefs(context).edit().putInt(KEY_DIM, value).apply()
        notifySettingsChanged()
    }

    fun getMode(context: Context): Int {
        return getPrefs(context).getInt(KEY_MODE, MODE_BLURRED_STRETCH)
    }

    fun setMode(context: Context, mode: Int) {
        getPrefs(context).edit().putInt(KEY_MODE, mode).apply()
        notifySettingsChanged()
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
