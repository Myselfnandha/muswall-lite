package com.muswall.lite.graphics

import android.graphics.Bitmap
import android.graphics.PointF
import android.media.FaceDetector
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Intelligent horizontal framing detector.
 * Identifies the focal point (faces or visual energy) of an artwork
 * to center the most important subjects when displayed on tall portrait phone displays.
 */
object SmartFramingDetector {

    private var cachedFocalKey: Int = 0
    private var cachedFocalXPercent: Int = 0

    @Synchronized
    fun getFocalXPercent(src: Bitmap): Int {
        if (src.isRecycled || src.width <= 0 || src.height <= 0) return 0
        if (src.width <= src.height) return 0
        val key = System.identityHashCode(src)
        if (cachedFocalKey == key) {
            return cachedFocalXPercent
        }
        val calculated = calculateFocalXPercent(src)
        cachedFocalKey = key
        cachedFocalXPercent = calculated
        return calculated
    }

    private fun calculateFocalXPercent(src: Bitmap): Int {
        if (src.isRecycled || src.width <= 0 || src.height <= 0) return 0
        if (src.width <= src.height) return 0

        // Step 1: Detect Faces
        try {
            val sampleW = 320
            val sampleH = (sampleW * src.height / src.width).coerceAtLeast(16)
            val evenW = if (sampleW % 2 == 0) sampleW else sampleW - 1
            val scaled = Bitmap.createScaledBitmap(src, evenW, sampleH, false)
            val rgb565 = scaled.copy(Bitmap.Config.RGB_565, false)
            if (scaled != src && !scaled.isRecycled) scaled.recycle()

            if (rgb565 != null) {
                val detector = FaceDetector(evenW, sampleH, 3)
                val faces = Array<FaceDetector.Face?>(3) { null }
                val count = detector.findFaces(rgb565, faces)
                if (count > 0) {
                    var totalWeight = 0f
                    var weightedX = 0f
                    for (i in 0 until count) {
                        val face = faces[i] ?: continue
                        val mid = PointF()
                        face.getMidPoint(mid)
                        val eyeDist = face.eyesDistance()
                        val weight = eyeDist.coerceAtLeast(10f)
                        weightedX += (mid.x / evenW.toFloat()) * weight
                        totalWeight += weight
                    }
                    rgb565.recycle()
                    if (totalWeight > 0f) {
                        val focalNormX = (weightedX / totalWeight).coerceIn(0f, 1f)
                        return ((focalNormX - 0.5f) * 200f).roundToInt().coerceIn(-100, 100)
                    }
                }
                if (!rgb565.isRecycled) rgb565.recycle()
            }
        } catch (_: Throwable) {
        }

        // Step 2: Edge & Contrast Energy Detection Fallback
        try {
            val sampleW = 64
            val sampleH = (sampleW * src.height / src.width).coerceAtLeast(8)
            val scaled = Bitmap.createScaledBitmap(src, sampleW, sampleH, false)
            val pixels = IntArray(sampleW * sampleH)
            scaled.getPixels(pixels, 0, sampleW, 0, 0, sampleW, sampleH)
            if (scaled != src && !scaled.isRecycled) scaled.recycle()

            val columnEnergy = FloatArray(sampleW)
            for (x in 1 until sampleW - 1) {
                var colEnergy = 0f
                for (y in 0 until sampleH) {
                    val pLeft = pixels[y * sampleW + (x - 1)]
                    val pRight = pixels[y * sampleW + (x + 1)]
                    val rDiff = abs(((pRight shr 16) and 0xFF) - ((pLeft shr 16) and 0xFF))
                    val gDiff = abs(((pRight shr 8) and 0xFF) - ((pLeft shr 8) and 0xFF))
                    val bDiff = abs((pRight and 0xFF) - (pLeft and 0xFF))
                    colEnergy += (rDiff + gDiff + bDiff).toFloat()
                }
                columnEnergy[x] = colEnergy
            }

            var totalEnergy = 0f
            var weightedX = 0f
            for (x in 0 until sampleW) {
                val e = columnEnergy[x]
                weightedX += (x.toFloat() / (sampleW - 1).toFloat()) * e
                totalEnergy += e
            }

            if (totalEnergy > 0f) {
                val normX = (weightedX / totalEnergy).coerceIn(0f, 1f)
                return ((normX - 0.5f) * 200f).roundToInt().coerceIn(-100, 100)
            }
        } catch (_: Throwable) {
        }

        return 0
    }
}
