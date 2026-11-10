package com.muswall.lite

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.max
import kotlin.math.min

object WallpaperRenderer {

    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val overlayPaint = Paint().apply {
        style = Paint.Style.FILL
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(140, 0, 0, 0)
    }

    fun render(
        canvas: Canvas,
        width: Int,
        height: Int,
        artBitmap: Bitmap?,
        restoreBitmap: Bitmap?,
        isRestoreActive: Boolean,
        config: ScreenConfig,
        transitionProgress: Float = 1f,
        transitionMode: Int = 0
    ) {
        if (width <= 0 || height <= 0) return

        val activeArt = if (isRestoreActive) restoreBitmap else artBitmap

        if (activeArt == null) {
            drawFallbackBackground(canvas, width, height)
            return
        }

        when (config.effectType) {
            ScreenConfig.EFFECT_FULL_BLEED -> {
                drawFullBleed(canvas, activeArt, width, height, config, transitionProgress, transitionMode)
            }
            ScreenConfig.EFFECT_STRETCHED_BLUR -> {
                drawStretchedBlur(canvas, activeArt, width, height, config, transitionProgress, transitionMode)
            }
            ScreenConfig.EFFECT_COVER_CARD -> {
                drawCoverCardWithBlurredBackground(canvas, activeArt, width, height, config, transitionProgress, transitionMode)
            }
        }

        applyDimming(canvas, width, height, config.dimPercent)
    }

    private fun drawFallbackBackground(canvas: Canvas, width: Int, height: Int) {
        val gradient = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            intArrayOf(Color.parseColor("#1A1A24"), Color.parseColor("#0C0C12"), Color.BLACK),
            null,
            Shader.TileMode.CLAMP
        )
        overlayPaint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
        overlayPaint.shader = null
    }

    private fun drawFullBleed(
        canvas: Canvas,
        src: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        config: ScreenConfig,
        progress: Float,
        mode: Int
    ) {
        val alpha = (progress.coerceIn(0f, 1f) * 255).toInt()
        bitmapPaint.alpha = alpha

        val scale = max(targetWidth.toFloat() / src.width, targetHeight.toFloat() / src.height)
        val scaledW = (scale * src.width)
        val scaledH = (scale * src.height)

        val left = (targetWidth - scaledW) / 2f
        val top = (targetHeight - scaledH) / 2f

        val destRect = RectF(left, top, left + scaledW, top + scaledH)
        canvas.drawBitmap(src, null, destRect, bitmapPaint)
        bitmapPaint.alpha = 255
    }

    private fun drawStretchedBlur(
        canvas: Canvas,
        src: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        config: ScreenConfig,
        progress: Float,
        mode: Int
    ) {
        val blurred = fastBoxBlur(src, config.blurPercent)
        drawFullBleed(canvas, blurred, targetWidth, targetHeight, config, progress, mode)
    }

    private fun drawCoverCardWithBlurredBackground(
        canvas: Canvas,
        src: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        config: ScreenConfig,
        progress: Float,
        mode: Int
    ) {
        val blurredBg = fastBoxBlur(src, config.blurPercent)
        drawFullBleed(canvas, blurredBg, targetWidth, targetHeight, config, 1f, 0)

        overlayPaint.color = Color.argb(60, 0, 0, 0)
        canvas.drawRect(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat(), overlayPaint)

        val cardSize = (min(targetWidth, targetHeight) * (config.coverSizePercent / 100f)).toInt()
            .coerceAtLeast(100)

        val baseCenterX = targetWidth / 2f
        val baseCenterY = when (config.coverPosition) {
            ScreenConfig.POSITION_TOP -> targetHeight * 0.28f
            ScreenConfig.POSITION_BOTTOM -> targetHeight * 0.72f
            else -> targetHeight * 0.50f
        }

        val offsetY = (config.coverYOffsetPercent / 100f) * (targetHeight * 0.4f)
        val centerY = (baseCenterY + offsetY).coerceIn(cardSize / 2f, targetHeight - (cardSize / 2f))

        val left = baseCenterX - (cardSize / 2f)
        val top = centerY - (cardSize / 2f)
        val right = left + cardSize
        val bottom = top + cardSize

        val cardRect = RectF(left, top, right, bottom)
        val radius = (config.coverCornerRadiusDp * (targetWidth / 360f)).coerceAtLeast(0f)

        val shadowOffset = 14f
        val shadowRect = RectF(left, top + shadowOffset, right, bottom + shadowOffset)
        canvas.drawRoundRect(shadowRect, radius, radius, shadowPaint)

        val path = Path().apply {
            addRoundRect(cardRect, radius, radius, Path.Direction.CW)
        }

        canvas.save()
        canvas.clipPath(path)

        val alpha = (progress.coerceIn(0f, 1f) * 255).toInt()
        bitmapPaint.alpha = alpha
        val srcRect = Rect(0, 0, src.width, src.height)
        canvas.drawBitmap(src, srcRect, cardRect, bitmapPaint)
        bitmapPaint.alpha = 255
        canvas.restore()
    }

    private fun applyDimming(canvas: Canvas, width: Int, height: Int, dimPercent: Int) {
        if (dimPercent <= 0) return
        val alpha = (dimPercent * 255 / 100).coerceIn(0, 255)
        overlayPaint.color = Color.argb(alpha, 0, 0, 0)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
    }

    fun fastBoxBlur(source: Bitmap, blurPercent: Int): Bitmap {
        if (blurPercent <= 0) return source

        val downscale = 4
        val smallWidth = (source.width / downscale).coerceAtLeast(1)
        val smallHeight = (source.height / downscale).coerceAtLeast(1)

        val small = Bitmap.createScaledBitmap(source, smallWidth, smallHeight, true)
        val output = small.copy(Bitmap.Config.ARGB_8888, true)

        val w = output.width
        val h = output.height
        val pixels = IntArray(w * h)
        output.getPixels(pixels, 0, w, 0, 0, w, h)

        val radius = (blurPercent * 25 / 100).coerceIn(1, 25)
        boxBlurHorizontal(pixels, w, h, radius)
        boxBlurVertical(pixels, w, h, radius)

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
