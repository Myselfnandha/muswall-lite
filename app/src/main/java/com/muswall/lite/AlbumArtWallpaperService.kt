package com.muswall.lite

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.max

class AlbumArtWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return AlbumEngine()
    }

    inner class AlbumEngine : Engine() {
        private val scope = CoroutineScope(Dispatchers.Main + Job())
        private var isVisibleState = false

        private var surfaceWidth = 1080
        private var surfaceHeight = 2400

        private var lastRenderedBitmap: Bitmap? = null
        private var cachedProcessedBitmap: Bitmap? = null
        private var lastBlur = -1
        private var lastDim = -1
        private var lastMode = -1

        private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        private val dimPaint = Paint().apply {
            style = Paint.Style.FILL
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)

            scope.launch {
                WallpaperState.currentArt.collect {
                    renderIfVisible()
                }
            }

            scope.launch {
                WallpaperState.settingsChanged.collect {
                    cachedProcessedBitmap = null
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
            cachedProcessedBitmap = null
            renderIfVisible()
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
            val blur = WallpaperState.getBlur(context)
            val dim = WallpaperState.getDim(context)
            val mode = WallpaperState.getMode(context)

            if (currentArt == null) {
                canvas.drawColor(Color.parseColor("#121212"))
                return
            }

            if (currentArt != lastRenderedBitmap || blur != lastBlur || dim != lastDim || mode != lastMode || cachedProcessedBitmap == null) {
                lastRenderedBitmap = currentArt
                lastBlur = blur
                lastDim = dim
                lastMode = mode
                cachedProcessedBitmap = processBitmap(currentArt, blur, mode)
            }

            val processed = cachedProcessedBitmap
            if (processed != null) {
                val src = Rect(0, 0, processed.width, processed.height)
                val dst = Rect(0, 0, surfaceWidth, surfaceHeight)
                canvas.drawBitmap(processed, src, dst, bitmapPaint)
            } else {
                canvas.drawColor(Color.BLACK)
            }

            if (dim > 0) {
                val alpha = (dim * 255 / 100).coerceIn(0, 255)
                dimPaint.color = Color.argb(alpha, 0, 0, 0)
                canvas.drawRect(0f, 0f, surfaceWidth.toFloat(), surfaceHeight.toFloat(), dimPaint)
            }
        }

        private fun processBitmap(source: Bitmap, blurPercent: Int, mode: Int): Bitmap {
            if (mode == WallpaperState.MODE_EDGE_TO_EDGE) {
                return scaleCenterCrop(source, surfaceWidth, surfaceHeight)
            }

            val scaled = scaleCenterCrop(source, surfaceWidth, surfaceHeight)
            if (blurPercent <= 0) return scaled

            return fastBoxBlur(scaled, (blurPercent * 25 / 100).coerceAtLeast(1))
        }

        private fun scaleCenterCrop(src: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
            val srcWidth = src.width
            val srcHeight = src.height

            val scale = max(targetWidth.toFloat() / srcWidth, targetHeight.toFloat() / srcHeight)
            val scaledWidth = (scale * srcWidth).toInt()
            val scaledHeight = (scale * srcHeight).toInt()

            val left = (targetWidth - scaledWidth) / 2f
            val top = (targetHeight - scaledHeight) / 2f

            val result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            val destRect = RectF(left, top, left + scaledWidth, top + scaledHeight)
            canvas.drawBitmap(src, null, destRect, bitmapPaint)
            return result
        }

        private fun fastBoxBlur(source: Bitmap, radius: Int): Bitmap {
            val downscale = 4
            val smallWidth = (source.width / downscale).coerceAtLeast(1)
            val smallHeight = (source.height / downscale).coerceAtLeast(1)

            val small = Bitmap.createScaledBitmap(source, smallWidth, smallHeight, true)
            val output = small.copy(Bitmap.Config.ARGB_8888, true)

            val w = output.width
            val h = output.height
            val pixels = IntArray(w * h)
            output.getPixels(pixels, 0, w, 0, 0, w, h)

            val rad = radius.coerceIn(1, 25)
            boxBlurHorizontal(pixels, w, h, rad)
            boxBlurVertical(pixels, w, h, rad)

            output.setPixels(pixels, 0, w, 0, 0, w, h)
            val result = Bitmap.createScaledBitmap(output, source.width, source.height, true)
            small.recycle()
            output.recycle()
            return result
        }

        private fun boxBlurHorizontal(pixels: IntArray, w: Int, h: Int, r: Int) {
            val div = 2 * r + 1
            for (y in 0 until h) {
                var a = 0
                var red = 0
                var g = 0
                var b = 0
                val rowOffset = y * w

                for (i in -r..r) {
                    val p = pixels[rowOffset + i.coerceIn(0, w - 1)]
                    a += (p ushr 24) and 0xff
                    red += (p ushr 16) and 0xff
                    g += (p ushr 8) and 0xff
                    b += p and 0xff
                }

                for (x in 0 until w) {
                    pixels[rowOffset + x] = ((a / div) shl 24) or
                            ((red / div) shl 16) or
                            ((g / div) shl 8) or
                            (b / div)

                    val pOut = pixels[rowOffset + (x - r).coerceIn(0, w - 1)]
                    val pIn = pixels[rowOffset + (x + r + 1).coerceIn(0, w - 1)]

                    a += ((pIn ushr 24) and 0xff) - ((pOut ushr 24) and 0xff)
                    red += ((pIn ushr 16) and 0xff) - ((pOut ushr 16) and 0xff)
                    g += ((pIn ushr 8) and 0xff) - ((pOut ushr 8) and 0xff)
                    b += (pIn and 0xff) - (pOut and 0xff)
                }
            }
        }

        private fun boxBlurVertical(pixels: IntArray, w: Int, h: Int, r: Int) {
            val div = 2 * r + 1
            for (x in 0 until w) {
                var a = 0
                var red = 0
                var g = 0
                var b = 0

                for (i in -r..r) {
                    val p = pixels[i.coerceIn(0, h - 1) * w + x]
                    a += (p ushr 24) and 0xff
                    red += (p ushr 16) and 0xff
                    g += (p ushr 8) and 0xff
                    b += p and 0xff
                }

                for (y in 0 until h) {
                    pixels[y * w + x] = ((a / div) shl 24) or
                            ((red / div) shl 16) or
                            ((g / div) shl 8) or
                            (b / div)

                    val pOut = pixels[(y - r).coerceIn(0, h - 1) * w + x]
                    val pIn = pixels[(y + r + 1).coerceIn(0, h - 1) * w + x]

                    a += ((pIn ushr 24) and 0xff) - ((pOut ushr 24) and 0xff)
                    red += ((pIn ushr 16) and 0xff) - ((pOut ushr 16) and 0xff)
                    g += ((pIn ushr 8) and 0xff) - ((pOut ushr 8) and 0xff)
                    b += (pIn and 0xff) - (pOut and 0xff)
                }
            }
        }
    }
}
