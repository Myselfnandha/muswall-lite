package com.muswall.lite.data.repository

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Repository responsible for managing restored/default wallpapers:
 * - Local file persistence (home and lock restore wallpapers)
 * - Safe system default wallpaper capture via WallpaperManager
 * - Ambient gradient fallback generation (guaranteeing never a black screen)
 */
class RestoreImageRepository(private val context: Context) {

    private var defaultHomeRestoreArt: Bitmap? = null
    private var defaultLockRestoreArt: Bitmap? = null

    suspend fun loadHomeRestoreImage(): Bitmap? = withContext(Dispatchers.IO) {
        loadImageFile(HOME_RESTORE_IMAGE)
    }

    suspend fun loadLockRestoreImage(): Bitmap? = withContext(Dispatchers.IO) {
        loadImageFile(LOCK_RESTORE_IMAGE)
    }

    suspend fun saveHomeRestoreImage(bitmap: Bitmap?) = withContext(Dispatchers.IO) {
        saveImageFile(HOME_RESTORE_IMAGE, bitmap)
    }

    suspend fun saveLockRestoreImage(bitmap: Bitmap?) = withContext(Dispatchers.IO) {
        saveImageFile(LOCK_RESTORE_IMAGE, bitmap)
    }

    @SuppressLint("MissingPermission")
    suspend fun captureSystemWallpaper(): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val wm = WallpaperManager.getInstance(context)
            val drawable = try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    wm.drawable ?: wm.builtInDrawable
                } else {
                    wm.builtInDrawable
                }
            } catch (_: SecurityException) {
                null
            }

            if (drawable != null) {
                val w = drawable.intrinsicWidth.coerceIn(1080, 1440)
                val h = drawable.intrinsicHeight.coerceIn(1920, 3200)
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                drawable.setBounds(0, 0, w, h)
                drawable.draw(canvas)
                bmp
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun getDefaultArtwork(isLock: Boolean): Bitmap {
        if (isLock) {
            val cached = defaultLockRestoreArt
            if (cached != null && !cached.isRecycled) return cached
            val bmp = createAmbientGradientArtwork(isLock = true)
            defaultLockRestoreArt = bmp
            return bmp
        } else {
            val cached = defaultHomeRestoreArt
            if (cached != null && !cached.isRecycled) return cached
            val bmp = createAmbientGradientArtwork(isLock = false)
            defaultHomeRestoreArt = bmp
            return bmp
        }
    }

    private fun loadImageFile(filename: String): Bitmap? {
        val file = File(context.filesDir, filename)
        if (!file.exists()) return null

        return try {
            val bmp = BitmapFactory.decodeFile(file.absolutePath)
            if (bmp == null) {
                file.delete()
            }
            bmp
        } catch (_: Throwable) {
            try { file.delete() } catch (_: Throwable) {}
            null
        }
    }

    private fun saveImageFile(filename: String, bitmap: Bitmap?) {
        val file = File(context.filesDir, filename)
        if (bitmap != null && !bitmap.isRecycled) {
            try {
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
                }
            } catch (_: Throwable) {
            }
        } else {
            if (file.exists()) file.delete()
        }
    }

    companion object {
        const val HOME_RESTORE_IMAGE = "home_restore_wallpaper.png"
        const val LOCK_RESTORE_IMAGE = "lock_restore_wallpaper.png"

        fun createAmbientGradientArtwork(isLock: Boolean): Bitmap {
            val w = 1080
            val h = 2400
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)

            val colors = if (isLock) {
                intArrayOf(
                    Color.parseColor("#0F2027"),
                    Color.parseColor("#203A43"),
                    Color.parseColor("#2C5364")
                )
            } else {
                intArrayOf(
                    Color.parseColor("#2E1065"),
                    Color.parseColor("#701A75"),
                    Color.parseColor("#9D174D"),
                    Color.parseColor("#EA580C")
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
                color = Color.argb(40, 255, 255, 255)
                style = Paint.Style.FILL
            }
            canvas.drawCircle(w * 0.5f, h * 0.4f, w * 0.35f, shapePaint)

            return bmp
        }
    }
}
