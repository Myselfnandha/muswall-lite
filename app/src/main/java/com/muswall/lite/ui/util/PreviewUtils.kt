package com.muswall.lite.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.ImageDecoder
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import com.muswall.lite.WallpaperRenderer
import com.muswall.lite.domain.model.ScreenConfig

object PreviewUtils {

    fun renderScreenPreview(
        art: Bitmap?,
        restore: Bitmap?,
        isRestore: Boolean,
        config: ScreenConfig,
        progress: Float = 1f,
        mode: Int = 0,
        previous: Bitmap? = null,
        preparedBlur: Bitmap? = null,
        preparedRestoreBlur: Bitmap? = null
    ): Bitmap {
        val pw = 280
        val ph = 600
        val bmp = Bitmap.createBitmap(pw, ph, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        WallpaperRenderer.render(
            canvas = canvas,
            width = pw,
            height = ph,
            artBitmap = art,
            restoreBitmap = restore,
            isRestoreActive = isRestore,
            config = config,
            transitionProgress = progress,
            transitionMode = mode,
            previousBitmap = previous,
            preparedBlur = preparedBlur,
            preparedRestoreBlur = preparedRestoreBlur,
            xOffset = 0.5f,
            displayWidth = pw
        )
        return bmp
    }

    fun loadSoftwareBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = true
            }
        } catch (_: Exception) {
            null
        }
    }

    fun generateSampleArtwork(): Bitmap {
        val size = 512
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val gradient = LinearGradient(
            0f, 0f, size.toFloat(), size.toFloat(),
            intArrayOf(AndroidColor.parseColor("#7B1FA2"), AndroidColor.parseColor("#E91E63"), AndroidColor.parseColor("#FF9800")),
            null,
            Shader.TileMode.CLAMP
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = gradient
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.argb(120, 0, 0, 0)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size * 0.32f, circlePaint)

        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size * 0.08f, innerPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = 36f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText("MUSIC ART", size / 2f, size * 0.88f, textPaint)

        return bmp
    }

    fun generateSampleRestoreArtwork(isLock: Boolean): Bitmap {
        val w = 540
        val h = 1200
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val colors = if (isLock) {
            intArrayOf(
                AndroidColor.parseColor("#0F2027"),
                AndroidColor.parseColor("#203A43"),
                AndroidColor.parseColor("#2C5364")
            )
        } else {
            intArrayOf(
                AndroidColor.parseColor("#2E1065"),
                AndroidColor.parseColor("#701A75"),
                AndroidColor.parseColor("#9D174D"),
                AndroidColor.parseColor("#EA580C")
            )
        }

        val gradient = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            colors,
            null,
            Shader.TileMode.CLAMP
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = gradient
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

        val shapePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.argb(40, 255, 255, 255)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(w * 0.5f, h * 0.4f, w * 0.35f, shapePaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.argb(200, 255, 255, 255)
            textSize = 34f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText(if (isLock) "LOCK DEFAULT" else "HOME DEFAULT", w * 0.5f, h * 0.41f, labelPaint)

        return bmp
    }
}
