package com.byagowi.persiancalendar.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.annotation.ColorInt
import androidx.core.content.res.ResourcesCompat
import com.byagowi.persiancalendar.R
import java.io.File
import kotlin.math.ceil

object WidgetGlassFontHelper {
    private var regularTypeface: Typeface? = null
    private var boldTypeface: Typeface? = null

    private fun getFont(context: Context, isBold: Boolean, customFontFile: File?): Typeface {
        if (customFontFile != null) {
            try {
                val custom = Typeface.createFromFile(customFontFile)
                if (custom != null) {
                    return if (isBold) Typeface.create(custom, Typeface.BOLD) else custom
                }
            } catch (e: Exception) {
                // fallback to bundled font
            }
        }

        if (isBold) {
            if (boldTypeface == null) {
                boldTypeface = try {
                    ResourcesCompat.getFont(context, R.font.vazirmatn_bold)
                } catch (e: Exception) {
                    Typeface.DEFAULT_BOLD
                }
            }
            return boldTypeface ?: Typeface.DEFAULT_BOLD
        } else {
            if (regularTypeface == null) {
                regularTypeface = try {
                    ResourcesCompat.getFont(context, R.font.vazirmatn_regular)
                } catch (e: Exception) {
                    Typeface.DEFAULT
                }
            }
            return regularTypeface ?: Typeface.DEFAULT
        }
    }

    fun createTextBitmap(
        context: Context,
        text: String?,
        textSizeSp: Float,
        @ColorInt textColor: Int,
        isBold: Boolean = false,
        paddingHorizontalPx: Int = 4,
        paddingVerticalPx: Int = 2,
        customFontFile: File? = null,
    ): Bitmap {
        if (text.isNullOrEmpty()) {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }

        val density = context.resources.displayMetrics.density
        val textSizePx = textSizeSp * density

        val typeface = getFont(context, isBold, customFontFile)

        val paint = Paint().apply {
            isAntiAlias = true
            isSubpixelText = true
            color = textColor
            textSize = textSizePx
            this.typeface = typeface
            textAlign = Paint.Align.LEFT
        }

        val fontMetrics = paint.fontMetrics
        val textWidth = paint.measureText(text)
        val textHeight = fontMetrics.bottom - fontMetrics.top

        val bitmapWidth = (ceil(textWidth) + paddingHorizontalPx * 2).toInt().coerceAtLeast(1)
        val bitmapHeight = (ceil(textHeight) + paddingVerticalPx * 2).toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val x = paddingHorizontalPx.toFloat()
        val y = paddingVerticalPx.toFloat() - fontMetrics.top

        canvas.drawText(text, x, y, paint)
        return bitmap
    }
}
