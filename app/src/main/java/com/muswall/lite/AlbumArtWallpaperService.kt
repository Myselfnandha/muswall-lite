package com.muswall.lite

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
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
        private val scope = CoroutineScope(Dispatchers.Main + Job())
        private var isVisibleState = false

        private var surfaceWidth = 1080
        private var surfaceHeight = 2400

        private var transitionJob: Job? = null
        private var transitionProgress = 1f
        private var transitionMode = ScreenConfig.TRANSITION_FADE

        private var previousBitmap: Bitmap? = null
        private var lastRenderedArt: Bitmap? = null
        private var lastRestoreState = true

        private val screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_USER_PRESENT,
                    Intent.ACTION_SCREEN_ON,
                    Intent.ACTION_SCREEN_OFF -> {
                        renderIfVisible()
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

            lastRestoreState = WallpaperState.isRestoreActive.value
            lastRenderedArt = WallpaperState.currentArt.value

            scope.launch {
                WallpaperState.currentArt.collect { newArt ->
                    val isRestored = WallpaperState.isRestoreActive.value
                    if (!isRestored && newArt != null && newArt !== lastRenderedArt) {
                        previousBitmap = lastRenderedArt ?: WallpaperState.getRestoreArt(isPhoneLocked())
                        lastRenderedArt = newArt
                        triggerApplyTransition()
                    } else {
                        lastRenderedArt = newArt
                        renderIfVisible()
                    }
                }
            }

            scope.launch {
                WallpaperState.isRestoreActive.collect { isRestored ->
                    if (isRestored != lastRestoreState) {
                        lastRestoreState = isRestored
                        val currentArt = WallpaperState.currentArt.value
                        val restoreArt = WallpaperState.getRestoreArt(isPhoneLocked())

                        if (isRestored) {
                            previousBitmap = currentArt ?: restoreArt
                            triggerRestoreTransition()
                        } else {
                            previousBitmap = restoreArt ?: currentArt
                            triggerApplyTransition()
                        }
                    }
                }
            }

            scope.launch {
                WallpaperState.settingsChanged.collect {
                    renderIfVisible()
                }
            }
        }

        override fun onDestroy() {
            try {
                unregisterReceiver(screenReceiver)
            } catch (_: Exception) {
            }
            transitionJob?.cancel()
            super.onDestroy()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            isVisibleState = visible
            if (visible) {
                renderIfVisible()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceWidth = width
            surfaceHeight = height
            renderIfVisible()
        }

        private fun triggerApplyTransition() {
            val context: Context = this@AlbumArtWallpaperService
            val isLocked = isPhoneLocked()
            val config = WallpaperState.getEffectiveConfig(context, isLockScreen = isLocked)
            if (config.applyEffect == ScreenConfig.TRANSITION_NONE) {
                transitionProgress = 1f
                renderIfVisible()
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
                    renderIfVisible()
                    delay(delayMs)
                }
                transitionProgress = 1f
                renderIfVisible()
            }
        }

        private fun triggerRestoreTransition() {
            val context: Context = this@AlbumArtWallpaperService
            val isLocked = isPhoneLocked()
            val config = WallpaperState.getEffectiveConfig(context, isLockScreen = isLocked)
            if (config.restoreEffect == ScreenConfig.TRANSITION_NONE) {
                transitionProgress = 1f
                renderIfVisible()
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
                    renderIfVisible()
                    delay(delayMs)
                }
                transitionProgress = 1f
                renderIfVisible()
            }
        }

        private fun isPhoneLocked(): Boolean {
            val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            return km?.isKeyguardLocked ?: false
        }

        private fun renderIfVisible() {
            if (!isVisibleState) return

            val holder = surfaceHolder ?: return
            var canvas: Canvas? = null

            try {
                canvas = holder.lockCanvas()
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
            val isLocked = isPhoneLocked()
            val currentArt = WallpaperState.currentArt.value
            val restoreArt = WallpaperState.getRestoreArt(isLockScreen = isLocked)
            val isRestoreActive = WallpaperState.isRestoreActive.value
            val config = WallpaperState.getEffectiveConfig(context, isLockScreen = isLocked)

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
                previousBitmap = previousBitmap
            )
        }
    }
}
