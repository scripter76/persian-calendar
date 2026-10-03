package com.byagowi.persiancalendar.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
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
import android.util.LruCache
import kotlin.math.max
import kotlin.math.min

/** Wallpaper-backed lens rendering, including refraction, soft diffusion and directional rims. */
object LiquidGlassEngine {
    private val widgetCache = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

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
     * Samples the wallpaper through a rounded lens. Refraction can sample outside the panel,
     * retaining the wallpaper's position. Works on Android 6+ and in launcher RemoteViews.
     */
    fun compositeLiquidGlass(
        rawBase: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        isDark: Boolean,
        sourceRect: RectF = RectF(0f, 0f, rawBase.width.toFloat(), rawBase.height.toFloat()),
        cornerRadii: FloatArray = FloatArray(4) { targetWidth * 26f / 340f },
        softenedBase: Bitmap? = null,
    ): Bitmap {
        require(targetWidth > 0 && targetHeight > 0)
        require(cornerRadii.size == 4)
        val soft = softenedBase ?: stackBlur(rawBase, 4)
        val sourcePixels = IntArray(rawBase.width * rawBase.height)
        rawBase.getPixels(sourcePixels, 0, rawBase.width, 0, 0, rawBase.width, rawBase.height)
        val softPixels = IntArray(soft.width * soft.height)
        soft.getPixels(softPixels, 0, soft.width, 0, 0, soft.width, soft.height)
        val pixels = IntArray(targetWidth * targetHeight)
        val optics = LiquidGlassOptics(targetWidth.toFloat(), targetHeight.toFloat(), cornerRadii)
        val sample = FloatArray(3)
        val scaleX = sourceRect.width() / targetWidth
        val scaleY = sourceRect.height() / targetHeight
        for (y in 0 until targetHeight) for (x in 0 until targetWidth) {
            optics.sample(x + .5f, y + .5f, sample)
            val sx = sourceRect.left + sample[0] * scaleX - .5f
            val sy = sourceRect.top + sample[1] * scaleY - .5f
            val clear = sampleBilinear(sourcePixels, rawBase.width, rawBase.height, sx, sy)
            val diffuse = sampleBilinear(
                softPixels, soft.width, soft.height,
                (sx + .5f) * soft.width / rawBase.width - .5f,
                (sy + .5f) * soft.height / rawBase.height - .5f,
            )
            // Preserve detail in the body, and even more in the refractive bevel.
            pixels[y * targetWidth + x] = mixColor(clear, diffuse, .48f * (1f - sample[2] * .7f))
        }
        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        output.setPixels(pixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)
        val canvas = Canvas(output)
        val w = targetWidth.toFloat()
        val h = targetHeight.toFloat()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val path = Path().apply {
            addRoundRect(RectF(0f, 0f, w, h), androidRadii(cornerRadii, w, h), Path.Direction.CW)
        }
        paint.color = if (isDark) Color.argb(20, 14, 18, 24) else Color.argb(28, 255, 255, 255)
        canvas.drawPath(path, paint)
        paint.shader = LinearGradient(
            0f, 0f, w, h,
            intArrayOf(Color.argb(24, 255, 255, 255), Color.TRANSPARENT, Color.argb(8, 255, 255, 255)),
            floatArrayOf(0f, .45f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawPath(path, paint)
        val unit = (w / 340f).coerceAtLeast(.5f)
        val inset = min(unit * .6f, min(w, h) * .25f)
        val rimPath = Path().apply {
            addRoundRect(
                RectF(inset, inset, w - inset, h - inset),
                androidRadii(cornerRadii.map { (it - inset).coerceAtLeast(0f) }.toFloatArray(), w, h),
                Path.Direction.CW,
            )
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = unit * 1.2f
        paint.shader = LinearGradient(
            0f, 0f, w, h,
            intArrayOf(
                Color.argb(190, 255, 255, 255), Color.argb(65, 255, 255, 255),
                Color.argb(18, 255, 255, 255), Color.argb(100, 255, 255, 255),
            ), floatArrayOf(0f, .28f, .65f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawPath(rimPath, paint)
        paint.strokeWidth = unit * 2.5f
        paint.shader = LinearGradient(0f, 0f, 0f, h, Color.TRANSPARENT, Color.argb(24, 0, 0, 0), Shader.TileMode.CLAMP)
        canvas.drawPath(rimPath, paint)
        paint.shader = null
        paint.style = Paint.Style.FILL
        // Software clipPath is not antialiased; use a full-size alpha mask for smooth widget corners.
        val mask = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        Canvas(mask).drawPath(path, paint.apply { color = Color.WHITE })
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        canvas.drawBitmap(mask, 0f, 0f, paint)
        mask.recycle()
        if (softenedBase == null && soft !== rawBase) soft.recycle()
        return output
    }

    private fun androidRadii(radii: FloatArray, width: Float, height: Float): FloatArray =
        FloatArray(8) { radii[it / 2].coerceIn(0f, min(width, height) / 2f) }

    private fun mixColor(a: Int, b: Int, amount: Float): Int = Color.rgb(
        (Color.red(a) + (Color.red(b) - Color.red(a)) * amount).toInt(),
        (Color.green(a) + (Color.green(b) - Color.green(a)) * amount).toInt(),
        (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * amount).toInt(),
    )

    private fun sampleBilinear(pixels: IntArray, width: Int, height: Int, x: Float, y: Float): Int {
        val sx = x.coerceIn(0f, (width - 1).toFloat())
        val sy = y.coerceIn(0f, (height - 1).toFloat())
        val x0 = sx.toInt()
        val y0 = sy.toInt()
        val x1 = min(x0 + 1, width - 1)
        val y1 = min(y0 + 1, height - 1)
        return mixColor(
            mixColor(pixels[y0 * width + x0], pixels[y0 * width + x1], sx - x0),
            mixColor(pixels[y1 * width + x0], pixels[y1 * width + x1], sx - x0), sy - y0,
        )
    }

    /** Bounded software bitmap; the app always uses the raw full wallpaper, never a widget crop. */
    fun loadWallpaper(context: Context): Bitmap {
        val file = File(context.filesDir, STORED_LIQUID_GLASS_RAW)
        if (file.exists()) {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)
            options.inSampleSize = 1
            while (options.outWidth / options.inSampleSize > 960) options.inSampleSize *= 2
            options.inJustDecodeBounds = false
            BitmapFactory.decodeFile(file.absolutePath, options)?.let { return it }
        }
        val fallback = BitmapFactory.decodeResource(
            context.resources, com.byagowi.persiancalendar.R.drawable.bg_glass_ios_clear,
            BitmapFactory.Options().apply { inScaled = false },
        )
        val bitmap = Bitmap.createScaledBitmap(fallback, 640, (640f * fallback.height / fallback.width).toInt().coerceAtLeast(1), true)
            .copy(Bitmap.Config.ARGB_8888, true)
        fallback.recycle()
        // A light neutral default matches the light app theme before a wallpaper is selected.
        Canvas(bitmap).drawColor(Color.argb(100, 255, 255, 255))
        return bitmap
    }

    fun processAndSave(context: Context, rawBitmap: Bitmap) {
        FileOutputStream(File(context.filesDir, STORED_LIQUID_GLASS_RAW)).use {
            rawBitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        val isDark = calculateLuminance(rawBitmap) < .48f
        val blurWidth = min(640, rawBitmap.width)
        val scaled = Bitmap.createScaledBitmap(
            rawBitmap, blurWidth, (blurWidth * rawBitmap.height.toFloat() / rawBitmap.width).toInt().coerceAtLeast(1), true,
        )
        val blurred = stackBlur(scaled, 4)
        FileOutputStream(File(context.filesDir, STORED_LIQUID_GLASS_MASTER_BLUR)).use {
            blurred.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        blurred.recycle()
        if (scaled !== rawBitmap) scaled.recycle()
        widgetCache.evictAll()
        context.preferences.edit {
            putBoolean(PREF_LIQUID_GLASS_IS_DARK, isDark)
            putLong(PREF_LIQUID_GLASS_UPDATED_AT, System.currentTimeMillis())
        }
        cropAndSaveWidgetLiquidGlass(context)
        runCatching { update(context, false) }
    }

    fun cropWidgetLiquidGlass(
        context: Context,
        yPercent: Float = DEFAULT_WIDGET_GLASS_Y_POS.toFloat(),
        targetWidth: Int = 800,
        targetHeight: Int = (targetWidth * 140f / 340f).toInt().coerceAtLeast(1),
    ): Bitmap {
        val rawFile = File(context.filesDir, STORED_LIQUID_GLASS_RAW)
        val percent = yPercent.coerceIn(0f, 100f)
        val key = "${rawFile.absolutePath}:${rawFile.lastModified()}:$percent:$targetWidth:$targetHeight"
        widgetCache.get(key)?.let { return it }
        // Reading raw also upgrades old captures with the former heavy blur, without recapture.
        val wallpaper = loadWallpaper(context)
        val screenW = wallpaper.width.toFloat()
        val screenH = wallpaper.height.toFloat()
        val cropW = min(screenW * .92f, screenH * targetWidth / targetHeight)
        val cropH = cropW * targetHeight / targetWidth
        val cropX = (screenW - cropW) / 2f
        val minY = min(screenH * .05f, screenH - cropH)
        val maxY = (screenH - cropH - screenH * .08f).coerceAtLeast(minY)
        val cropY = minY + percent / 100f * (maxY - minY)
        val sourceRect = RectF(cropX, cropY, cropX + cropW, cropY + cropH)
        val crop = Bitmap.createBitmap(
            wallpaper, cropX.toInt(), cropY.toInt(), cropW.toInt().coerceIn(1, wallpaper.width - cropX.toInt()), cropH.toInt().coerceIn(1, wallpaper.height - cropY.toInt()),
        )
        val isDark = calculateLuminance(crop) < .48f
        if (crop !== wallpaper) crop.recycle()
        val result = compositeLiquidGlass(wallpaper, targetWidth, targetHeight, isDark, sourceRect)
        wallpaper.recycle()
        widgetCache.put(key, result)
        return result
    }

    fun cropAndSaveWidgetLiquidGlass(
        context: Context,
        yPercent: Float = context.preferences.getInt(PREF_WIDGET_GLASS_Y_POS, DEFAULT_WIDGET_GLASS_Y_POS).toFloat(),
    ): Bitmap {
        val bitmap = cropWidgetLiquidGlass(context, yPercent)
        FileOutputStream(File(context.filesDir, STORED_LIQUID_GLASS_PROCESSED)).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        return bitmap
    }

    fun removeWallpaper(context: Context) {
        widgetCache.evictAll()
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
