package com.muswall.lite

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import kotlin.math.max

object WallpaperRenderer {

    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val renderMatrix = Matrix()

    private var cachedSourceKey: Int = 0
    private var cachedBlurPercent: Int = -1
    private var cachedBlurredBitmap: Bitmap? = null
    private var scratchPixels: IntArray? = null
    private var scratchTemp: IntArray? = null

    @Synchronized
    fun render(
        canvas: Canvas,
        width: Int,
        height: Int,
        artBitmap: Bitmap?,
        restoreBitmap: Bitmap?,
        isRestoreActive: Boolean,
        config: ScreenConfig,
        transitionProgress: Float = 1f,
        transitionMode: Int = 0,
        previousBitmap: Bitmap? = null,
        preparedBlur: Bitmap? = null,
        preparedRestoreBlur: Bitmap? = null,
        xOffset: Float = 0.5f,
        displayWidth: Int = 0
    ) {
        if (width <= 0 || height <= 0) return

        val targetArt = if (isRestoreActive) restoreBitmap else artBitmap

        if (targetArt == null && previousBitmap == null) {
            drawFallbackBackground(canvas, width, height)
            return
        }

        val progress = transitionProgress.coerceIn(0f, 1f)
        val screenCenterX = if (displayWidth in 1 until width) {
            ((width - displayWidth) * xOffset.coerceIn(0f, 1f)) + (displayWidth / 2f)
        } else {
            width / 2f
        }

        if (progress < 1f && previousBitmap != null && !previousBitmap.isRecycled) {
            drawFullBleed(canvas, previousBitmap, width, height, 1f, screenCenterX)
        } else if (targetArt == null) {
            drawFallbackBackground(canvas, width, height)
            return
        }

        if (targetArt != null && !targetArt.isRecycled) {
            canvas.save()

            when (transitionMode) {
                ScreenConfig.TRANSITION_ZOOM -> {
                    val scale = 1.15f - (0.15f * progress)
                    canvas.scale(scale, scale, screenCenterX, height / 2f)
                }
                ScreenConfig.TRANSITION_SLIDE_UP -> {
                    val offsetY = (1f - progress) * (height * 0.16f)
                    canvas.translate(0f, offsetY)
                }
                ScreenConfig.TRANSITION_SLIDE_DOWN -> {
                    val offsetY = -(1f - progress) * (height * 0.16f)
                    canvas.translate(0f, offsetY)
                }
                ScreenConfig.TRANSITION_RADIAL -> {
                    val scale = 0.82f + (0.18f * progress)
                    canvas.scale(scale, scale, screenCenterX, height / 2f)
                }
                else -> {
                }
            }

            if (isRestoreActive) {
                if (config.applyEffectsToRestore) {
                    renderBlurredArt(canvas, targetArt, width, height, config.blurPercent, progress, preparedRestoreBlur, screenCenterX)
                } else {
                    drawFullBleed(canvas, targetArt, width, height, progress, screenCenterX)
                }
            } else {
                if (transitionMode == ScreenConfig.TRANSITION_BLUR_DISSOLVE) {
                    if (progress < 1f) {
                        drawFullBleed(canvas, targetArt, width, height, 1f - progress, screenCenterX)
                    }
                    renderBlurredArt(canvas, targetArt, width, height, config.blurPercent, progress, preparedBlur, screenCenterX)
                } else {
                    renderBlurredArt(canvas, targetArt, width, height, config.blurPercent, progress, preparedBlur, screenCenterX)
                }
            }

            canvas.restore()
        }

        applyDimming(canvas, width, height, config.dimPercent)
    }

    private fun renderBlurredArt(
        canvas: Canvas,
        art: Bitmap,
        width: Int,
        height: Int,
        blurPercent: Int,
        progress: Float,
        preparedFallback: Bitmap?,
        centerX: Float
    ) {
        if (preparedFallback != null && !preparedFallback.isRecycled) {
            drawFullBleed(canvas, preparedFallback, width, height, progress, centerX)
        } else {
            drawBlur(canvas, art, width, height, blurPercent, progress, centerX)
        }
    }

    private fun drawFallbackBackground(canvas: Canvas, width: Int, height: Int) {
        overlayPaint.color = Color.parseColor("#0C0C12")
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
    }

    private fun drawFullBleed(
        canvas: Canvas,
        src: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        progress: Float,
        centerX: Float = targetWidth / 2f
    ) {
        if (src.isRecycled) return
        val alpha = (progress.coerceIn(0f, 1f) * 255).toInt()
        if (alpha <= 0) return
        bitmapPaint.alpha = alpha

        val scale = max(targetWidth.toFloat() / src.width, targetHeight.toFloat() / src.height)
        val scaledW = scale * src.width
        val scaledH = scale * src.height

        val left = centerX - (scaledW / 2f)
        val top = (targetHeight - scaledH) / 2f

        renderMatrix.reset()
        renderMatrix.postScale(scale, scale)
        renderMatrix.postTranslate(left, top)
        canvas.drawBitmap(src, renderMatrix, bitmapPaint)
        bitmapPaint.alpha = 255
    }

    private fun drawBlur(
        canvas: Canvas,
        src: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        blurPercent: Int,
        progress: Float,
        centerX: Float
    ) {
        if (src.isRecycled) return
        val effectiveBlur = if (blurPercent > 0) blurPercent else 50
        val blurred = getNormalBlur(src, effectiveBlur)
        if (blurred.isRecycled) return

        drawFullBleed(canvas, blurred, targetWidth, targetHeight, progress, centerX)
    }

    private fun applyDimming(canvas: Canvas, width: Int, height: Int, dimPercent: Int) {
        if (dimPercent <= 0) return
        val alpha = (dimPercent * 255 / 100).coerceIn(0, 255)
        overlayPaint.color = Color.argb(alpha, 0, 0, 0)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
    }

    @Synchronized
    fun getNormalBlur(source: Bitmap, blurPercent: Int): Bitmap {
        if (blurPercent <= 0 || source.isRecycled) return source
        val key = System.identityHashCode(source)
        val cached = cachedBlurredBitmap
        if (cached != null && cachedSourceKey == key && cachedBlurPercent == blurPercent && !cached.isRecycled) {
            return cached
        }

        val downscale = 4
        val sw = (source.width / downscale).coerceAtLeast(16)
        val sh = (source.height / downscale).coerceAtLeast(16)
        val small = Bitmap.createScaledBitmap(source, sw, sh, true)
        val output = small.copy(Bitmap.Config.RGB_565, true)
        if (small !== output) {
            small.recycle()
        }

        val w = output.width
        val h = output.height
        val total = w * h

        val pixels = if (scratchPixels != null && scratchPixels!!.size >= total) {
            scratchPixels!!
        } else {
            val newArr = IntArray(total)
            scratchPixels = newArr
            newArr
        }

        output.getPixels(pixels, 0, w, 0, 0, w, h)

        val radius = (blurPercent * 16 / 100).coerceIn(1, 16)
        twoPassBoxBlur(pixels, w, h, radius)
        output.setPixels(pixels, 0, w, 0, 0, w, h)

        cachedBlurredBitmap = output
        cachedSourceKey = key
        cachedBlurPercent = blurPercent
        return output
    }

    private fun twoPassBoxBlur(pixels: IntArray, w: Int, h: Int, r: Int) {
        val total = w * h
        val temp = if (scratchTemp != null && scratchTemp!!.size >= total) {
            scratchTemp!!
        } else {
            val newArr = IntArray(total)
            scratchTemp = newArr
            newArr
        }

        boxBlurPass(pixels, temp, w, h, r)
        boxBlurPass(pixels, temp, w, h, r)
    }

    private fun boxBlurPass(pixels: IntArray, temp: IntArray, w: Int, h: Int, r: Int) {
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
                temp[rowOffset + x] = ((a / div) shl 24) or
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

        for (x in 0 until w) {
            var a = 0
            var red = 0
            var g = 0
            var b = 0

            for (i in -r..r) {
                val p = temp[i.coerceIn(0, h - 1) * w + x]
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

                val pOut = temp[(y - r).coerceIn(0, h - 1) * w + x]
                val pIn = temp[(y + r + 1).coerceIn(0, h - 1) * w + x]

                a += ((pIn ushr 24) and 0xff) - ((pOut ushr 24) and 0xff)
                red += ((pIn ushr 16) and 0xff) - ((pOut ushr 16) and 0xff)
                g += ((pIn ushr 8) and 0xff) - ((pOut ushr 8) and 0xff)
                b += (pIn and 0xff) - (pOut and 0xff)
            }
        }
    }
}
