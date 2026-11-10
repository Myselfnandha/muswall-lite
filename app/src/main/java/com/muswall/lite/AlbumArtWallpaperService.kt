package com.muswall.lite

import android.content.Context
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

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            val context: Context = this@AlbumArtWallpaperService
            WallpaperState.init(context)

            scope.launch {
                WallpaperState.currentArt.collect {
                    triggerApplyTransition()
                }
            }

            scope.launch {
                WallpaperState.isRestoreActive.collect { isRestored ->
                    if (isRestored) {
                        triggerRestoreTransition()
                    } else {
                        triggerApplyTransition()
                    }
                }
            }

            scope.launch {
                WallpaperState.settingsChanged.collect {
                    renderIfVisible()
                }
            }
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
            val config = WallpaperState.getEffectiveConfig(context, isLockScreen = false)
            if (config.applyEffect == ScreenConfig.TRANSITION_NONE) {
                transitionProgress = 1f
                renderIfVisible()
                return
            }

            transitionJob?.cancel()
            transitionJob = scope.launch {
                val frames = 18
                val delayMs = 18L
                transitionMode = config.applyEffect
                for (i in 1..frames) {
                    transitionProgress = i / frames.toFloat()
                    renderIfVisible()
                    delay(delayMs)
                }
                transitionProgress = 1f
                renderIfVisible()
            }
        }

        private fun triggerRestoreTransition() {
            val context: Context = this@AlbumArtWallpaperService
            val config = WallpaperState.getEffectiveConfig(context, isLockScreen = false)
            if (config.restoreEffect == ScreenConfig.TRANSITION_NONE) {
                transitionProgress = 1f
                renderIfVisible()
                return
            }

            transitionJob?.cancel()
            transitionJob = scope.launch {
                val frames = 18
                val delayMs = 18L
                transitionMode = config.restoreEffect
                for (i in 1..frames) {
                    transitionProgress = i / frames.toFloat()
                    renderIfVisible()
                    delay(delayMs)
                }
                transitionProgress = 1f
                renderIfVisible()
            }
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
            val currentArt = WallpaperState.currentArt.value
            val restoreArt = WallpaperState.restoreArt.value
            val isRestoreActive = WallpaperState.isRestoreActive.value
            val config = WallpaperState.getEffectiveConfig(context, isLockScreen = false)

            WallpaperRenderer.render(
                canvas = canvas,
                width = surfaceWidth,
                height = surfaceHeight,
                artBitmap = currentArt,
                restoreBitmap = restoreArt,
                isRestoreActive = isRestoreActive,
                config = config,
                transitionProgress = transitionProgress,
                transitionMode = transitionMode
            )
        }
    }
}
