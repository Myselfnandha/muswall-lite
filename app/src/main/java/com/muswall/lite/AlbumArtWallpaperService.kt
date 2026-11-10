package com.muswall.lite

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AlbumArtWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return AlbumEngine()
    }

    inner class AlbumEngine : Engine() {
        private val scope = CoroutineScope(Dispatchers.Default + Job())
        private var isVisibleState = false

        private var surfaceWidth = 1080
        private var surfaceHeight = 2400

        private var transitionJob: Job? = null
        private var renderJob: Job? = null
        private var transitionProgress = 1f
        private var transitionMode = ScreenConfig.TRANSITION_FADE

        private var previousBitmap: Bitmap? = null
        private var lastRenderedArt: Bitmap? = null
        private var lastRenderedBlur: Bitmap? = null
        private var lastRestoreState = true
        private var cachedIsLocked = false

        private val screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        cachedIsLocked = true
                        transitionJob?.cancel()
                        transitionProgress = 1f
                    }
                    Intent.ACTION_USER_PRESENT -> {
                        val wasLocked = cachedIsLocked
                        cachedIsLocked = false
                        if (wasLocked && WallpaperState.separateScreens.value) {
                            scheduleRender()
                        }
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        updateKeyguardState(forceRender = true)
                    }
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            val context: Context = this@AlbumArtWallpaperService
            WallpaperState.init(context)

            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            registerReceiver(screenReceiver, filter)

            updateKeyguardState(forceRender = false)
            lastRestoreState = WallpaperState.isRestoreActive.value
            lastRenderedArt = WallpaperState.currentArt.value
            lastRenderedBlur = WallpaperState.getPreparedBlur(cachedIsLocked)

            scope.launch {
                WallpaperState.currentArt.collect { newArt ->
                    val isRestored = WallpaperState.isRestoreActive.value
                    val artChanged = when {
                        newArt == null && lastRenderedArt != null -> true
                        newArt != null && lastRenderedArt == null -> true
                        newArt != null && lastRenderedArt != null -> !newArt.sameAs(lastRenderedArt)
                        else -> false
                    }

                    if (!isRestored && artChanged && newArt != null) {
                        previousBitmap = lastRenderedBlur ?: lastRenderedArt ?: WallpaperState.getRestoreArt(cachedIsLocked)
                        lastRenderedArt = newArt
                        lastRenderedBlur = WallpaperState.getPreparedBlur(cachedIsLocked)
                        triggerApplyTransition()
                    } else {
                        lastRenderedArt = newArt
                        lastRenderedBlur = WallpaperState.getPreparedBlur(cachedIsLocked)
                        scheduleRender()
                    }
                }
            }

            scope.launch {
                WallpaperState.isRestoreActive.collect { isRestored ->
                    if (isRestored != lastRestoreState) {
                        lastRestoreState = isRestored
                        val currentArt = WallpaperState.currentArt.value
                        val restoreArt = WallpaperState.getRestoreArt(cachedIsLocked)
                        val currentBlur = WallpaperState.getPreparedBlur(cachedIsLocked)

                        if (isRestored) {
                            previousBitmap = currentBlur ?: currentArt ?: restoreArt
                            triggerRestoreTransition()
                        } else {
                            previousBitmap = restoreArt ?: currentBlur ?: currentArt
                            triggerApplyTransition()
                        }
                    }
                }
            }

            scope.launch {
                WallpaperState.homeRestoreArt.collect {
                    if (WallpaperState.isRestoreActive.value && !cachedIsLocked) {
                        scheduleRender()
                    }
                }
            }

            scope.launch {
                WallpaperState.lockRestoreArt.collect {
                    if (WallpaperState.isRestoreActive.value && cachedIsLocked) {
                        scheduleRender()
                    }
                }
            }

            scope.launch {
                WallpaperState.preparedHomeBlur.collect { blur ->
                    if (!WallpaperState.isRestoreActive.value && !cachedIsLocked) {
                        lastRenderedBlur = blur
                        scheduleRender()
                    }
                }
            }

            scope.launch {
                WallpaperState.preparedLockBlur.collect { blur ->
                    if (!WallpaperState.isRestoreActive.value && cachedIsLocked) {
                        lastRenderedBlur = blur
                        scheduleRender()
                    }
                }
            }

            scope.launch {
                WallpaperState.preparedHomeRestoreBlur.collect {
                    if (WallpaperState.isRestoreActive.value && !cachedIsLocked) {
                        scheduleRender()
                    }
                }
            }

            scope.launch {
                WallpaperState.preparedLockRestoreBlur.collect {
                    if (WallpaperState.isRestoreActive.value && cachedIsLocked) {
                        scheduleRender()
                    }
                }
            }

            scope.launch {
                WallpaperState.settingsChanged.collect {
                    scheduleRender()
                }
            }
        }

        override fun onDestroy() {
            try {
                unregisterReceiver(screenReceiver)
            } catch (_: Exception) {
            }
            transitionJob?.cancel()
            renderJob?.cancel()
            super.onDestroy()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            isVisibleState = visible
            if (visible) {
                updateKeyguardState(forceRender = false)
                scheduleRender()
            } else {
                transitionJob?.cancel()
                transitionProgress = 1f
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceWidth = width
            surfaceHeight = height
            scheduleRender()
        }

        private fun updateKeyguardState(forceRender: Boolean = true) {
            try {
                val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                val isLocked = km?.isKeyguardLocked ?: false
                val changed = (cachedIsLocked != isLocked)
                cachedIsLocked = isLocked
                if (forceRender && changed && WallpaperState.separateScreens.value) {
                    scheduleRender()
                }
            } catch (_: Exception) {
            }
        }

        private fun triggerApplyTransition() {
            val context: Context = this@AlbumArtWallpaperService
            val config = WallpaperState.getEffectiveConfig(context, isLockScreen = cachedIsLocked)
            if (config.applyEffect == ScreenConfig.TRANSITION_NONE || !isVisibleState) {
                transitionProgress = 1f
                previousBitmap = null
                scheduleRender()
                return
            }

            transitionJob?.cancel()
            transitionJob = scope.launch {
                val frames = 18
                val delayMs = 16L
                transitionMode = config.applyEffect
                for (i in 1..frames) {
                    val t = i / frames.toFloat()
                    transitionProgress = 1f - (1f - t) * (1f - t)
                    renderDirect()
                    delay(delayMs)
                }
                transitionProgress = 1f
                previousBitmap = null
                renderDirect()
            }
        }

        private fun triggerRestoreTransition() {
            val context: Context = this@AlbumArtWallpaperService
            val config = WallpaperState.getEffectiveConfig(context, isLockScreen = cachedIsLocked)
            if (config.restoreEffect == ScreenConfig.TRANSITION_NONE || !isVisibleState) {
                transitionProgress = 1f
                previousBitmap = null
                scheduleRender()
                return
            }

            transitionJob?.cancel()
            transitionJob = scope.launch {
                val frames = 18
                val delayMs = 16L
                transitionMode = config.restoreEffect
                for (i in 1..frames) {
                    val t = i / frames.toFloat()
                    transitionProgress = 1f - (1f - t) * (1f - t)
                    renderDirect()
                    delay(delayMs)
                }
                transitionProgress = 1f
                previousBitmap = null
                renderDirect()
            }
        }

        private fun scheduleRender() {
            if (!isVisibleState) return
            renderJob?.cancel()
            renderJob = scope.launch {
                renderDirect()
            }
        }

        private fun renderDirect() {
            if (!isVisibleState) return

            val holder = surfaceHolder ?: return
            var canvas: Canvas? = null

            try {
                canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        holder.lockHardwareCanvas()
                    } catch (_: Exception) {
                        holder.lockCanvas()
                    }
                } else {
                    holder.lockCanvas()
                }
                if (canvas != null) {
                    drawFrame(canvas)
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }

        private fun drawFrame(canvas: Canvas) {
            val context: Context = this@AlbumArtWallpaperService
            val currentArt = WallpaperState.currentArt.value
            val restoreArt = WallpaperState.getRestoreArt(isLockScreen = cachedIsLocked)
            val isRestoreActive = WallpaperState.isRestoreActive.value
            val config = WallpaperState.getEffectiveConfig(context, isLockScreen = cachedIsLocked)
            val preparedBlur = WallpaperState.getPreparedBlur(isLockScreen = cachedIsLocked)
            val preparedRestoreBlur = WallpaperState.getPreparedRestoreBlur(isLockScreen = cachedIsLocked)

            WallpaperRenderer.render(
                canvas = canvas,
                width = surfaceWidth,
                height = surfaceHeight,
                artBitmap = currentArt,
                restoreBitmap = restoreArt,
                isRestoreActive = isRestoreActive,
                config = config,
                transitionProgress = transitionProgress,
                transitionMode = transitionMode,
                previousBitmap = previousBitmap,
                preparedBlur = preparedBlur,
                preparedRestoreBlur = preparedRestoreBlur
            )
        }
    }
}
