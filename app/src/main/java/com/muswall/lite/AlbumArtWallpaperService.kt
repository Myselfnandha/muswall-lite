package com.muswall.lite

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.Choreographer
import android.view.SurfaceHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AlbumArtWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return AlbumEngine()
    }

    inner class AlbumEngine : Engine() {
        private val engineJob = Job()
        private val scope = CoroutineScope(Dispatchers.Default + engineJob)
        private val mainHandler = Handler(Looper.getMainLooper())
        private var isVisibleState = false

        private var surfaceWidth = 1080
        private var surfaceHeight = 2400
        private var displayWidth = 1080
        private var currentXOffset = 0.5f

        private var isAnimating = false
        private var animStartTimeNanos = 0L
        private val animDurationNanos = 300_000_000L
        private var transitionProgress = 1f
        private var transitionMode = ScreenConfig.TRANSITION_FADE

        private var previousBitmap: Bitmap? = null
        private var lastRenderedArt: Bitmap? = null
        private var lastRenderedBlur: Bitmap? = null
        private var lastRestoreState = true
        private var cachedIsLocked = false

        private val frameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (!isAnimating || !isVisibleState) return

                if (animStartTimeNanos == 0L) {
                    animStartTimeNanos = frameTimeNanos
                }

                val elapsed = frameTimeNanos - animStartTimeNanos
                val t = (elapsed.toFloat() / animDurationNanos).coerceIn(0f, 1f)
                transitionProgress = 1f - (1f - t) * (1f - t)

                if (t >= 1f) {
                    isAnimating = false
                    transitionProgress = 1f
                    previousBitmap = null
                }

                renderDirect()

                if (t < 1f) {
                    Choreographer.getInstance().postFrameCallback(this)
                }
            }
        }

        private val screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        cachedIsLocked = true
                        stopAnimation()
                    }
                    Intent.ACTION_USER_PRESENT -> {
                        val wasLocked = cachedIsLocked
                        cachedIsLocked = false
                        if (wasLocked && WallpaperState.separateScreens.value && isVisibleState) {
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
            displayWidth = resources.displayMetrics.widthPixels

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
                        if (isVisibleState) {
                            previousBitmap = lastRenderedBlur ?: lastRenderedArt ?: WallpaperState.getRestoreArt(cachedIsLocked)
                            lastRenderedArt = newArt
                            lastRenderedBlur = WallpaperState.getPreparedBlur(cachedIsLocked)
                            triggerApplyTransition()
                        } else {
                            lastRenderedArt = newArt
                            lastRenderedBlur = WallpaperState.getPreparedBlur(cachedIsLocked)
                            transitionProgress = 1f
                            previousBitmap = null
                        }
                    } else {
                        lastRenderedArt = newArt
                        lastRenderedBlur = WallpaperState.getPreparedBlur(cachedIsLocked)
                        if (isVisibleState) {
                            scheduleRender()
                        }
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

                        if (isVisibleState) {
                            if (isRestored) {
                                previousBitmap = currentBlur ?: currentArt ?: restoreArt
                                triggerRestoreTransition()
                            } else {
                                previousBitmap = restoreArt ?: currentBlur ?: currentArt
                                triggerApplyTransition()
                            }
                        } else {
                            transitionProgress = 1f
                            previousBitmap = null
                        }
                    }
                }
            }

            scope.launch {
                WallpaperState.homeRestoreArt.collect {
                    if (WallpaperState.isRestoreActive.value && !cachedIsLocked && isVisibleState) {
                        scheduleRender()
                    }
                }
            }

            scope.launch {
                WallpaperState.lockRestoreArt.collect {
                    if (WallpaperState.isRestoreActive.value && cachedIsLocked && isVisibleState) {
                        scheduleRender()
                    }
                }
            }

            scope.launch {
                WallpaperState.preparedHomeBlur.collect { blur ->
                    if (!WallpaperState.isRestoreActive.value && !cachedIsLocked) {
                        lastRenderedBlur = blur
                        if (isVisibleState) scheduleRender()
                    }
                }
            }

            scope.launch {
                WallpaperState.preparedLockBlur.collect { blur ->
                    if (!WallpaperState.isRestoreActive.value && cachedIsLocked) {
                        lastRenderedBlur = blur
                        if (isVisibleState) scheduleRender()
                    }
                }
            }

            scope.launch {
                WallpaperState.preparedHomeRestoreBlur.collect {
                    if (WallpaperState.isRestoreActive.value && !cachedIsLocked && isVisibleState) {
                        scheduleRender()
                    }
                }
            }

            scope.launch {
                WallpaperState.preparedLockRestoreBlur.collect {
                    if (WallpaperState.isRestoreActive.value && cachedIsLocked && isVisibleState) {
                        scheduleRender()
                    }
                }
            }

            scope.launch {
                WallpaperState.settingsChanged.collect {
                    if (isVisibleState) scheduleRender()
                }
            }
        }

        override fun onDestroy() {
            try {
                unregisterReceiver(screenReceiver)
            } catch (_: Exception) {
            }
            stopAnimation()
            engineJob.cancel()
            super.onDestroy()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            isVisibleState = visible
            if (visible) {
                updateKeyguardState(forceRender = false)
                scheduleRender()
            } else {
                stopAnimation()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceWidth = width
            surfaceHeight = height
            displayWidth = resources.displayMetrics.widthPixels
            scheduleRender()
        }

        override fun onOffsetsChanged(
            xOffset: Float,
            yOffset: Float,
            xOffsetStep: Float,
            yOffsetStep: Float,
            xPixelOffset: Int,
            yPixelOffset: Int
        ) {
            super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset)
            val changed = (currentXOffset != xOffset)
            currentXOffset = xOffset
            if (changed && isVisibleState && surfaceWidth > displayWidth) {
                scheduleRender()
            }
        }

        override fun onSurfaceRedrawNeeded(holder: SurfaceHolder) {
            super.onSurfaceRedrawNeeded(holder)
            renderDirect()
        }

        private fun updateKeyguardState(forceRender: Boolean = true) {
            try {
                val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                val isLocked = km?.isKeyguardLocked ?: false
                val changed = (cachedIsLocked != isLocked)
                cachedIsLocked = isLocked
                if (forceRender && changed && WallpaperState.separateScreens.value && isVisibleState) {
                    scheduleRender()
                }
            } catch (_: Exception) {
            }
        }

        private fun triggerApplyTransition() {
            val context: Context = this@AlbumArtWallpaperService
            val config = WallpaperState.getEffectiveConfig(context, isLockScreen = cachedIsLocked)
            if (config.applyEffect == ScreenConfig.TRANSITION_NONE || !isVisibleState) {
                stopAnimation()
                renderDirect()
                return
            }

            startAnimation(config.applyEffect)
        }

        private fun triggerRestoreTransition() {
            val context: Context = this@AlbumArtWallpaperService
            val config = WallpaperState.getEffectiveConfig(context, isLockScreen = cachedIsLocked)
            if (config.restoreEffect == ScreenConfig.TRANSITION_NONE || !isVisibleState) {
                stopAnimation()
                renderDirect()
                return
            }

            startAnimation(config.restoreEffect)
        }

        private fun startAnimation(mode: Int) {
            transitionMode = mode
            isAnimating = true
            animStartTimeNanos = 0L
            mainHandler.post {
                Choreographer.getInstance().removeFrameCallback(frameCallback)
                Choreographer.getInstance().postFrameCallback(frameCallback)
            }
        }

        private fun stopAnimation() {
            isAnimating = false
            transitionProgress = 1f
            previousBitmap = null
            mainHandler.post {
                Choreographer.getInstance().removeFrameCallback(frameCallback)
            }
        }

        private fun scheduleRender() {
            if (!isVisibleState || isAnimating) return
            mainHandler.post {
                if (!isVisibleState || isAnimating) return@post
                renderDirect()
            }
        }

        private fun renderDirect() {
            if (!isVisibleState) return

            val holder = surfaceHolder ?: return
            var canvas: Canvas? = null

            try {
                canvas = try {
                    holder.lockHardwareCanvas()
                } catch (_: Exception) {
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
                preparedRestoreBlur = preparedRestoreBlur,
                xOffset = currentXOffset,
                displayWidth = displayWidth
            )
        }
    }
}
