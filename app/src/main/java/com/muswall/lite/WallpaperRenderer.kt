package com.muswall.lite

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.muswall.lite.graphics.BoxBlurEngine
import com.muswall.lite.graphics.SmartFramingDetector
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object WallpaperRenderer {

    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val renderMatrix = Matrix()
    private val clipPath = Path()

    private var cachedSourceKey: Int = 0
    private var cachedBlurPercent: Int = -1
    private var cachedBlurredBitmap: Bitmap? = null

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

        val effectiveRestoreActive = isRestoreActive || artBitmap == null
        val targetArt = if (effectiveRestoreActive) (restoreBitmap ?: WallpaperState.getDefaultRestoreArt(false)) else artBitmap

        val visibleW = if (displayWidth in 1 until width) displayWidth.toFloat() else width.toFloat()
        val screenLeft = if (displayWidth in 1 until width) {
            ((width - displayWidth) * xOffset.coerceIn(0f, 1f))
        } else {
            (width - visibleW) / 2f
        }
        val screenCenterX = screenLeft + (visibleW / 2f)

        val progress = transitionProgress.coerceIn(0f, 1f)

        if (progress < 1f && previousBitmap != null && !previousBitmap.isRecycled) {
            drawFullBleed(canvas, previousBitmap, width, height, visibleW, 1f, screenCenterX, config.wallpaperOffsetYPercent)
        }

        if (!targetArt.isRecycled) {
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

            val effectiveOffsetX = getEffectiveOffsetX(config, targetArt)

            if (effectiveRestoreActive) {
                if (config.applyEffectsToRestore) {
                    if (config.restoreBlurPercent > 0) {
                        renderBlurredArt(canvas, targetArt, width, height, visibleW, config.restoreBlurPercent, progress, preparedRestoreBlur, screenCenterX, config.wallpaperOffsetYPercent, effectiveOffsetX)
                    } else {
                        drawFullBleed(canvas, targetArt, width, height, visibleW, progress, screenCenterX, config.wallpaperOffsetYPercent, effectiveOffsetX)
                    }
                    applyDimming(canvas, width, height, config.restoreDimPercent)
                } else {
                    drawFullBleed(canvas, targetArt, width, height, visibleW, progress, screenCenterX, config.wallpaperOffsetYPercent, effectiveOffsetX)
                }
            } else {
                when (config.effectType) {
                    ScreenConfig.EFFECT_BLUR -> {
                        if (config.aspectFitMode == ScreenConfig.ASPECT_FIT_WIDTH) {
                            renderBlurredArt(canvas, targetArt, width, height, visibleW, config.blurPercent.coerceAtLeast(35), progress, preparedBlur, screenCenterX, 0, effectiveOffsetX)
                            applyDimming(canvas, width, height, config.dimPercent)
                            drawFitWidth(canvas, targetArt, width, height, visibleW, config, progress, screenCenterX)
                        } else {
                            if (transitionMode == ScreenConfig.TRANSITION_BLUR_DISSOLVE && progress < 1f) {
                                drawFullBleed(canvas, targetArt, width, height, visibleW, 1f - progress, screenCenterX, config.wallpaperOffsetYPercent, effectiveOffsetX)
                            }
                            renderBlurredArt(canvas, targetArt, width, height, visibleW, config.blurPercent, progress, preparedBlur, screenCenterX, config.wallpaperOffsetYPercent, effectiveOffsetX)
                            applyDimming(canvas, width, height, config.dimPercent)
                        }
                    }
                    ScreenConfig.EFFECT_STILL -> {
                        drawFallbackBackground(canvas, width, height)
                        applyDimming(canvas, width, height, config.dimPercent)
                        drawStill(canvas, targetArt, width, height, visibleW, config, progress, screenCenterX)
                    }
                    ScreenConfig.EFFECT_BLUR_STILL -> {
                        renderBlurredArt(canvas, targetArt, width, height, visibleW, config.blurPercent.coerceAtLeast(35), progress, preparedBlur, screenCenterX, 0, effectiveOffsetX)
                        applyDimming(canvas, width, height, config.dimPercent)
                        drawStill(canvas, targetArt, width, height, visibleW, config, progress, screenCenterX)
                    }
                    else -> {
                        renderBlurredArt(canvas, targetArt, width, height, visibleW, config.blurPercent, progress, preparedBlur, screenCenterX, config.wallpaperOffsetYPercent, effectiveOffsetX)
                        applyDimming(canvas, width, height, config.dimPercent)
                    }
                }
            }

            canvas.restore()
        }
    }

    private fun renderBlurredArt(
        canvas: Canvas,
        art: Bitmap,
        width: Int,
        height: Int,
        visibleW: Float,
        blurPercent: Int,
        progress: Float,
        preparedFallback: Bitmap?,
        centerX: Float,
        offsetYPercent: Int = 0,
        offsetXPercent: Int = 0
    ) {
        if (preparedFallback != null && !preparedFallback.isRecycled) {
            drawFullBleed(canvas, preparedFallback, width, height, visibleW, progress, centerX, offsetYPercent, offsetXPercent)
        } else {
            drawBlur(canvas, art, width, height, visibleW, blurPercent, progress, centerX, offsetYPercent, offsetXPercent)
        }
    }

    private fun drawFallbackBackground(canvas: Canvas, width: Int, height: Int) {
        overlayPaint.color = Color.parseColor("#0C0C12")
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
    }

    private fun drawFitWidth(
        canvas: Canvas,
        src: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        visibleWidth: Float,
        config: ScreenConfig,
        progress: Float,
        centerX: Float
    ) {
        if (src.isRecycled) return
        val alpha = (progress.coerceIn(0f, 1f) * 255).toInt()
        if (alpha <= 0) return
        bitmapPaint.alpha = alpha

        val scale = (visibleWidth * (config.stillScalePercent / 100f).coerceIn(0.6f, 1f)) / src.width.toFloat()
        val scaledW = scale * src.width.toFloat()
        val scaledH = scale * src.height.toFloat()

        val effectiveOffsetX = getEffectiveOffsetX(config, src)
        val normX = ((effectiveOffsetX + 100f) / 200f).coerceIn(0f, 1f)
        val availableX = (visibleWidth - scaledW).coerceAtLeast(0f)
        val left = (centerX - (visibleWidth / 2f)) + (availableX * normX)

        val availableY = (targetHeight - scaledH).coerceAtLeast(0f)
        val normY = ((config.wallpaperOffsetYPercent + 100f) / 200f).coerceIn(0f, 1f)
        val top = availableY * normY

        val rect = RectF(left, top, left + scaledW, top + scaledH)
        val cornerRadius = min(scaledW, scaledH) * (config.stillCornerRadiusPercent / 100f) * 0.5f

        shadowPaint.color = Color.argb((alpha * 0.45f).toInt(), 0, 0, 0)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, shadowPaint)

        clipPath.reset()
        clipPath.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(clipPath)

        renderMatrix.reset()
        renderMatrix.postScale(scale, scale)
        renderMatrix.postTranslate(left, top)
        canvas.drawBitmap(src, renderMatrix, bitmapPaint)

        canvas.restore()
        bitmapPaint.alpha = 255
    }

    private fun drawFullBleed(
        canvas: Canvas,
        src: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        visibleWidth: Float,
        progress: Float,
        centerX: Float = targetWidth / 2f,
        offsetYPercent: Int = 0,
        offsetXPercent: Int = 0
    ) {
        if (src.isRecycled) return
        val alpha = (progress.coerceIn(0f, 1f) * 255).toInt()
        if (alpha <= 0) return
        bitmapPaint.alpha = alpha

        val scale = max(visibleWidth / src.width, targetHeight.toFloat() / src.height)
        val scaledW = scale * src.width
        val scaledH = scale * src.height

        val normX = ((offsetXPercent + 100f) / 200f).coerceIn(0f, 1f)
        val normY = ((offsetYPercent + 100f) / 200f).coerceIn(0f, 1f)

        val screenLeft = centerX - (visibleWidth / 2f)
        val availableX = visibleWidth - scaledW
        val availableY = targetHeight - scaledH

        val left = screenLeft + (availableX * normX)
        val top = availableY * normY

        renderMatrix.reset()
        renderMatrix.postScale(scale, scale)
        renderMatrix.postTranslate(left, top)
        canvas.drawBitmap(src, renderMatrix, bitmapPaint)
        bitmapPaint.alpha = 255
    }

    private fun drawStill(
        canvas: Canvas,
        src: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        visibleWidth: Float,
        config: ScreenConfig,
        progress: Float,
        centerX: Float
    ) {
        if (src.isRecycled) return
        val alpha = (progress.coerceIn(0f, 1f) * 255).toInt()
        if (alpha <= 0) return

        val scaleFactor = (config.stillScalePercent / 100f).coerceIn(0.2f, 1.5f)
        val maxDim = min(visibleWidth, targetHeight.toFloat())
        val baseSize = maxDim * scaleFactor

        val srcW = src.width.toFloat()
        val srcH = src.height.toFloat()
        val aspect = if (srcH > 0f) srcW / srcH else 1f

        val stillW: Float
        val stillH: Float
        if (aspect >= 1f) {
            stillW = baseSize
            stillH = baseSize / aspect
        } else {
            stillH = baseSize
            stillW = baseSize * aspect
        }

        val effectiveOffsetX = getEffectiveOffsetX(config, src)
        val normX = ((effectiveOffsetX + 100f) / 200f).coerceIn(0f, 1f)
        val availableX = (visibleWidth - stillW).coerceAtLeast(0f)
        val left = (centerX - (visibleWidth / 2f)) + (availableX * normX)
        val availableY = (targetHeight - stillH).coerceAtLeast(0f)
        val normalizedY = ((config.stillOffsetYPercent + 100f) / 200f).coerceIn(0f, 1f)
        val top = availableY * normalizedY

        val rect = RectF(left, top, left + stillW, top + stillH)
        val cornerRadius = min(stillW, stillH) * (config.stillCornerRadiusPercent / 100f) * 0.5f

        shadowPaint.color = Color.argb((alpha * 0.45f).toInt(), 0, 0, 0)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, shadowPaint)

        clipPath.reset()
        clipPath.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)

        canvas.save()
        canvas.clipPath(clipPath)

        val srcScale = max(stillW / srcW, stillH / srcH)
        val sw = srcScale * srcW
        val sh = srcScale * srcH
        val sLeft = left + (stillW - sw) / 2f
        val sTop = top + (stillH - sh) / 2f

        renderMatrix.reset()
        renderMatrix.postScale(srcScale, srcScale)
        renderMatrix.postTranslate(sLeft, sTop)

        bitmapPaint.alpha = alpha
        canvas.drawBitmap(src, renderMatrix, bitmapPaint)
        bitmapPaint.alpha = 255

        canvas.restore()
    }

    private fun drawBlur(
        canvas: Canvas,
        src: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        visibleWidth: Float,
        blurPercent: Int,
        progress: Float,
        centerX: Float,
        offsetYPercent: Int = 0,
        offsetXPercent: Int = 0
    ) {
        if (src.isRecycled) return
        if (blurPercent <= 0) {
            drawFullBleed(canvas, src, targetWidth, targetHeight, visibleWidth, progress, centerX, offsetYPercent, offsetXPercent)
            return
        }
        val blurred = getNormalBlur(src, blurPercent)
        if (blurred.isRecycled) return

        drawFullBleed(canvas, blurred, targetWidth, targetHeight, visibleWidth, progress, centerX, offsetYPercent, offsetXPercent)
    }

    private fun applyDimming(canvas: Canvas, width: Int, height: Int, dimPercent: Int) {
        if (dimPercent <= 0) return
        val alpha = (dimPercent * 255 / 100).coerceIn(0, 255)
        overlayPaint.color = Color.argb(alpha, 0, 0, 0)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
    }

    @Synchronized
    fun getNormalBlur(source: Bitmap, blurPercent: Int): Bitmap {
        if (blurPercent <= 0 || source.isRecycled || source.width <= 0 || source.height <= 0) return source
        val key = System.identityHashCode(source)
        val cached = cachedBlurredBitmap
        if (cached != null && cachedSourceKey == key && cachedBlurPercent == blurPercent && !cached.isRecycled) {
            return cached
        }

        val output = BoxBlurEngine.blur(source, blurPercent)
        if (output !== source && !output.isRecycled) {
            cachedBlurredBitmap = output
            cachedSourceKey = key
            cachedBlurPercent = blurPercent
        }
        return output
    }

    private fun getEffectiveOffsetX(config: ScreenConfig, art: Bitmap?): Int {
        return if (config.autoHorizontalFraming && art != null && !art.isRecycled) {
            getFocalXPercent(art)
        } else {
            config.wallpaperOffsetXPercent
        }
    }

    fun getFocalXPercent(src: Bitmap): Int {
        return SmartFramingDetector.getFocalXPercent(src)
    }
}
