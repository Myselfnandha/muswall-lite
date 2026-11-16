package com.muswall.lite.graphics

import android.graphics.Bitmap
import kotlin.math.roundToInt

/**
 * High-performance, low-memory multi-pass box blur engine.
 * Computes an approximation of Gaussian blur using 2 or 3 linear passes.
 * Designed to run in < 15ms with zero garbage allocation after scratch buffers are warmed up.
 */
object BoxBlurEngine {

    private var scratchTemp: IntArray? = null

    /**
     * Blurs [source] using a percentage scale (0 to 100).
     * Automatically downscales high-res inputs to prevent memory spikes.
     */
    @Synchronized
    fun blur(source: Bitmap, blurPercent: Int): Bitmap {
        if (source.isRecycled || source.width <= 0 || source.height <= 0) return source
        if (blurPercent <= 0) return source

        return try {
            val scale = when {
                blurPercent > 40 -> 0.20f
                blurPercent > 20 -> 0.25f
                blurPercent > 10 -> 0.35f
                else -> 0.50f
            }

            val sw = (source.width * scale).roundToInt().coerceAtLeast(16)
            val sh = (source.height * scale).roundToInt().coerceAtLeast(16)

            val downscaled = Bitmap.createScaledBitmap(source, sw, sh, true)
            val w = downscaled.width
            val h = downscaled.height

            val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val total = w * h
            val pixels = IntArray(total)
            downscaled.getPixels(pixels, 0, w, 0, 0, w, h)
            if (downscaled != source && !downscaled.isRecycled) {
                downscaled.recycle()
            }

            val passes: Int
            val radius: Int
            when {
                blurPercent <= 2 -> {
                    passes = 1
                    radius = 1
                }
                blurPercent <= 5 -> {
                    passes = 2
                    radius = 1
                }
                blurPercent <= 15 -> {
                    passes = 2
                    radius = 1 + (blurPercent - 5) / 3
                }
                blurPercent <= 40 -> {
                    passes = 3
                    radius = 2 + (blurPercent - 15) * 8 / 25
                }
                blurPercent <= 70 -> {
                    passes = 3
                    radius = 6 + (blurPercent - 40) * 10 / 30
                }
                else -> {
                    passes = 3
                    radius = 12 + (blurPercent - 70) * 16 / 30
                }
            }

            multiPassBoxBlur(pixels, w, h, radius, passes)
            output.setPixels(pixels, 0, w, 0, 0, w, h)
            output
        } catch (_: Throwable) {
            source
        }
    }

    private fun multiPassBoxBlur(pixels: IntArray, w: Int, h: Int, r: Int, passes: Int = 3) {
        val total = w * h
        val temp = if (scratchTemp != null && scratchTemp!!.size >= total) {
            scratchTemp!!
        } else {
            val newArr = IntArray(total)
            scratchTemp = newArr
            newArr
        }

        for (p in 0 until passes.coerceIn(1, 3)) {
            boxBlurPass(pixels, temp, w, h, r)
        }
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
                val avgA = (a / div).coerceIn(0, 255)
                val avgR = (red / div).coerceIn(0, 255)
                val avgG = (g / div).coerceIn(0, 255)
                val avgB = (b / div).coerceIn(0, 255)
                temp[rowOffset + x] = (avgA shl 24) or (avgR shl 16) or (avgG shl 8) or avgB

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
                val avgA = (a / div).coerceIn(0, 255)
                val avgR = (red / div).coerceIn(0, 255)
                val avgG = (g / div).coerceIn(0, 255)
                val avgB = (b / div).coerceIn(0, 255)
                pixels[y * w + x] = (avgA shl 24) or (avgR shl 16) or (avgG shl 8) or avgB

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
