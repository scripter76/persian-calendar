package com.byagowi.persiancalendar.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.content.edit
import com.byagowi.persiancalendar.DEFAULT_WIDGET_GLASS_Y_POS
import com.byagowi.persiancalendar.PREF_LIQUID_GLASS_IS_DARK
import com.byagowi.persiancalendar.PREF_LIQUID_GLASS_UPDATED_AT
import com.byagowi.persiancalendar.PREF_WIDGET_GLASS_Y_POS
import com.byagowi.persiancalendar.STORED_LIQUID_GLASS_MASTER_BLUR
import com.byagowi.persiancalendar.STORED_LIQUID_GLASS_PROCESSED
import com.byagowi.persiancalendar.STORED_LIQUID_GLASS_RAW
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Liquid Glass Rendering Engine
 *
 * Implements optical glassmorphism with:
 * - High-radius stack blur
 * - Surface tension / liquid meniscus highlight curve
 * - Ambient light specular sheen (directional 315°)
 * - Adaptive frosted tint (translucent dark for high contrast on bright wallpapers,
 *   milky frosted for dark wallpapers)
 * - Specular refractive rim border
 */
object LiquidGlassEngine {

    fun isWallpaperSet(context: Context): Boolean =
        File(context.filesDir, STORED_LIQUID_GLASS_PROCESSED).exists() ||
                File(context.filesDir, STORED_LIQUID_GLASS_RAW).exists()

    fun calculateLuminance(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        val stepX = max(1, width / 40)
        val stepY = max(1, height / 40)
        var totalLuminance = 0.0
        var count = 0

        for (y in 0 until height step stepY) {
            for (x in 0 until width step stepX) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel) / 255.0
                val g = Color.green(pixel) / 255.0
                val b = Color.blue(pixel) / 255.0
                // ITU-R BT.709 sRGB luminance formula
                val lum = 0.2126 * r + 0.7152 * g + 0.0722 * b
                totalLuminance += lum
                count++
            }
        }
        return if (count > 0) (totalLuminance / count).toFloat() else 0.5f
    }

    /**
     * Optimized Mario Klingemann Stack Blur in pure Kotlin.
     * Operates in O(1) per pixel with no native dependencies.
     */
    fun stackBlur(source: Bitmap, radius: Int): Bitmap {
        if (radius < 1) return source
        val w = source.width
        val h = source.height
        val pix = IntArray(w * h)
        source.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(max(w, h))

        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        for (idx in 0 until 256 * divsum) {
            dv[idx] = idx / divsum
        }

        yw = 0
        yi = 0

        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int

        for (yIdx in 0 until h) {
            y = yIdx
            rinsum = 0
            ginsum = 0
            binsum = 0
            routsum = 0
            goutsum = 0
            boutsum = 0
            rsum = 0
            gsum = 0
            bsum = 0
            for (idx in -radius..radius) {
                p = pix[yi + min(wm, max(idx, 0))]
                sir = stack[idx + radius]
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = p and 0x0000ff
                rbs = r1 - Math.abs(idx)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (idx > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
            }
            stackpointer = radius

            for (xIdx in 0 until w) {
                x = xIdx
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (y == 0) {
                    vmin[x] = min(x + radius + 1, wm)
                }
                p = pix[yw + vmin[x]]

                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = p and 0x0000ff

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi++
            }
            yw += w
        }

        for (xIdx in 0 until w) {
            x = xIdx
            rinsum = 0
            ginsum = 0
            binsum = 0
            routsum = 0
            goutsum = 0
            boutsum = 0
            rsum = 0
            gsum = 0
            bsum = 0
            yp = -radius * w
            for (idx in -radius..radius) {
                yi = max(0, yp) + x
                sir = stack[idx + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                rbs = r1 - Math.abs(idx)
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs
                if (idx > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                if (idx < hm) {
                    yp += w
                }
            }
            yi = x
            stackpointer = radius
            for (yIdx in 0 until h) {
                y = yIdx
                pix[yi] = (-0x1000000 and pix[yi]) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (x == 0) {
                    vmin[y] = min(y + r1, hm) * w
                }
                p = x + vmin[y]

                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi += w
            }
        }

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(pix, 0, w, 0, 0, w, h)
        return result
    }

    /**
     * Composites liquid glass effects (frosted diffusion, meniscus curve, specular sheen,
     * rim highlight) onto the blurred wallpaper.
     */
    fun compositeLiquidGlass(
        blurredBase: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        isDark: Boolean,
    ): Bitmap {
        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val w = targetWidth.toFloat()
        val h = targetHeight.toFloat()

        // 1. Draw blurred base (scaled to fit)
        val srcRect = Rect(0, 0, blurredBase.width, blurredBase.height)
        val dstRect = Rect(0, 0, targetWidth, targetHeight)
        canvas.drawBitmap(blurredBase, srcRect, dstRect, null)

        // 2. Adaptive frosted tint
        val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) {
                // Milky white frosted glass on dark wallpaper
                Color.argb(35, 255, 255, 255)
            } else {
                // Smoky translucent dark glass on light wallpaper for high contrast
                Color.argb(85, 18, 20, 26)
            }
        }
        canvas.drawRect(0f, 0f, w, h, tintPaint)

        // 3. Specular ambient sheen from top-left (315° directional light source)
        val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, w * 0.85f, h * 0.65f,
                intArrayOf(
                    Color.argb(55, 255, 255, 255),
                    Color.argb(18, 255, 255, 255),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.4f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, w, h, sheenPaint)

        // 4. Liquid meniscus highlight curve (simulates fluid surface tension on top third)
        val meniscusPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w, h * 0.22f)
            quadTo(w * 0.5f, h * 0.36f, 0f, h * 0.22f)
            close()
        }
        val meniscusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, h * 0.36f,
                Color.argb(45, 255, 255, 255),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawPath(meniscusPath, meniscusPaint)

        // 5. Specular refractive outer rim stroke
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            shader = LinearGradient(
                0f, 0f, w, h,
                intArrayOf(
                    Color.argb(110, 255, 255, 255),
                    Color.argb(25, 255, 255, 255),
                    Color.argb(35, 0, 0, 0),
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        val bounds = RectF(1.5f, 1.5f, w - 1.5f, h - 1.5f)
        canvas.drawRoundRect(bounds, 32f, 32f, strokePaint)

        return output
    }

    /**
     * Ingests, processes, and persists the raw wallpaper bitmap.
     */
    fun processAndSave(context: Context, rawBitmap: Bitmap) {
        val rawFile = File(context.filesDir, STORED_LIQUID_GLASS_RAW)
        FileOutputStream(rawFile).use { out ->
            rawBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        // 1. Analyze luminance
        val luminance = calculateLuminance(rawBitmap)
        val isDark = luminance < 0.48f

        // 2. Downscale for fast & smooth StackBlur
        val targetBlurWidth = 540
        val targetBlurHeight = (targetBlurWidth * (rawBitmap.height.toFloat() / rawBitmap.width)).toInt()
        val scaled = Bitmap.createScaledBitmap(rawBitmap, targetBlurWidth, targetBlurHeight, true)
        val blurred = stackBlur(scaled, 26)

        // Save full screen master blur for screen-accurate widget cropping
        val masterBlurFile = File(context.filesDir, STORED_LIQUID_GLASS_MASTER_BLUR)
        FileOutputStream(masterBlurFile).use { out ->
            blurred.compress(Bitmap.CompressFormat.PNG, 90, out)
        }

        // 3. Update preferences
        context.preferences.edit {
            putBoolean(PREF_LIQUID_GLASS_IS_DARK, isDark)
            putLong(PREF_LIQUID_GLASS_UPDATED_AT, System.currentTimeMillis())
        }

        // 4. Pre-generate default crop
        val defaultYPos = context.preferences.getInt(PREF_WIDGET_GLASS_Y_POS, DEFAULT_WIDGET_GLASS_Y_POS).toFloat()
        cropAndSaveWidgetLiquidGlass(context, defaultYPos, blurred, isDark)

        // 5. Update widgets
        runCatching {
            update(context, false)
        }
    }

    /**
     * Crops the exact screen portion corresponding to the widget's vertical position
     * on the launcher and composites liquid glass optical shading.
     */
    fun cropWidgetLiquidGlass(
        context: Context,
        yPercent: Float = DEFAULT_WIDGET_GLASS_Y_POS.toFloat(),
        targetWidth: Int = 800,
    ): Bitmap? {
        val masterFile = File(context.filesDir, STORED_LIQUID_GLASS_MASTER_BLUR).takeIf { it.exists() }
            ?: File(context.filesDir, STORED_LIQUID_GLASS_RAW).takeIf { it.exists() }
            ?: return null

        val masterBitmap = BitmapFactory.decodeFile(masterFile.absolutePath) ?: return null
        val screenW = masterBitmap.width
        val screenH = masterBitmap.height
        val isDark = context.preferences.getBoolean(PREF_LIQUID_GLASS_IS_DARK, false)

        // 4x2 widget width spans ~92% of screen width (centered horizontally)
        val cropW = (screenW * 0.92f).toInt().coerceAtLeast(10)
        val cropH = (cropW * (140f / 340f)).toInt().coerceAtLeast(10)
        val cropX = ((screenW - cropW) / 2).coerceAtLeast(0)

        // Vertical position:
        // 0% -> top of screen (5% from status bar)
        // 100% -> bottom of screen (8% above nav bar)
        val minY = (screenH * 0.05f).toInt()
        val maxY = (screenH - cropH - (screenH * 0.08f)).toInt().coerceAtLeast(minY)
        val clampedPercent = (yPercent / 100f).coerceIn(0f, 1f)
        val cropY = (minY + clampedPercent * (maxY - minY)).toInt().coerceIn(0, (screenH - cropH).coerceAtLeast(0))

        val safeCropW = cropW.coerceAtMost(screenW - cropX)
        val safeCropH = cropH.coerceAtMost(screenH - cropY)

        val croppedSubBitmap = Bitmap.createBitmap(masterBitmap, cropX, cropY, safeCropW, safeCropH)
        val targetHeight = (targetWidth * (140f / 340f)).toInt()
        return compositeLiquidGlass(croppedSubBitmap, targetWidth, targetHeight, isDark)
    }

    fun cropAndSaveWidgetLiquidGlass(
        context: Context,
        yPercent: Float = DEFAULT_WIDGET_GLASS_Y_POS.toFloat(),
        blurredMaster: Bitmap? = null,
        isDarkParam: Boolean? = null,
    ): Bitmap? {
        val bitmap = if (blurredMaster != null) {
            val isDark = isDarkParam ?: context.preferences.getBoolean(PREF_LIQUID_GLASS_IS_DARK, false)
            val screenW = blurredMaster.width
            val screenH = blurredMaster.height
            val cropW = (screenW * 0.92f).toInt().coerceAtLeast(10)
            val cropH = (cropW * (140f / 340f)).toInt().coerceAtLeast(10)
            val cropX = ((screenW - cropW) / 2).coerceAtLeast(0)

            val minY = (screenH * 0.05f).toInt()
            val maxY = (screenH - cropH - (screenH * 0.08f)).toInt().coerceAtLeast(minY)
            val clampedPercent = (yPercent / 100f).coerceIn(0f, 1f)
            val cropY = (minY + clampedPercent * (maxY - minY)).toInt().coerceIn(0, (screenH - cropH).coerceAtLeast(0))

            val safeCropW = cropW.coerceAtMost(screenW - cropX)
            val safeCropH = cropH.coerceAtMost(screenH - cropY)
            val croppedSubBitmap = Bitmap.createBitmap(blurredMaster, cropX, cropY, safeCropW, safeCropH)
            compositeLiquidGlass(croppedSubBitmap, 800, (800 * 140f / 340f).toInt(), isDark)
        } else {
            cropWidgetLiquidGlass(context, yPercent)
        }

        if (bitmap != null) {
            val processedFile = File(context.filesDir, STORED_LIQUID_GLASS_PROCESSED)
            FileOutputStream(processedFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
        }
        return bitmap
    }

    fun removeWallpaper(context: Context) {
        File(context.filesDir, STORED_LIQUID_GLASS_RAW).delete()
        File(context.filesDir, STORED_LIQUID_GLASS_MASTER_BLUR).delete()
        File(context.filesDir, STORED_LIQUID_GLASS_PROCESSED).delete()
        context.preferences.edit {
            remove(PREF_LIQUID_GLASS_IS_DARK)
            remove(PREF_LIQUID_GLASS_UPDATED_AT)
            remove(PREF_WIDGET_GLASS_Y_POS)
        }
        runCatching {
            update(context, false)
        }
    }
}
