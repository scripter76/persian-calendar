package com.byagowi.persiancalendar

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.test.platform.app.InstrumentationRegistry
import com.byagowi.persiancalendar.utils.LiquidGlassEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LiquidGlassRenderingTest {
    @Test
    fun roundedMaskAndAdaptiveTint() {
        for (color in listOf(Color.rgb(235, 240, 245), Color.rgb(35, 40, 50))) {
            val raw = Bitmap.createBitmap(340, 140, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
            val glass = LiquidGlassEngine.compositeLiquidGlass(raw, 340, 140, Color.red(color) < 128)
            assertEquals(0, Color.alpha(glass.getPixel(0, 0)))
            assertEquals(255, Color.alpha(glass.getPixel(170, 70)))
            // The old bright-wallpaper tint reduced channels by ~85. Clear glass keeps their tone.
            assertTrue(kotlin.math.abs(Color.red(glass.getPixel(170, 70)) - Color.red(color)) < 25)
            raw.recycle()
            glass.recycle()
        }
    }

    @Test
    fun lensUsesWallpaperCoordinatesAndPreservesDetails() {
        val raw = Bitmap.createBitmap(680, 280, Bitmap.Config.ARGB_8888)
        val paint = Paint()
        val canvas = Canvas(raw)
        canvas.drawColor(Color.rgb(226, 232, 240))
        for (x in 0 until 680 step 20) {
            paint.color = if (x % 40 == 0) Color.rgb(85, 145, 210) else Color.rgb(245, 173, 143)
            canvas.drawRect(x.toFloat(), 0f, x + 10f, 280f, paint)
        }
        val glass = LiquidGlassEngine.compositeLiquidGlass(raw, 340, 140, false, RectF(150f, 70f, 490f, 210f))
        val center = glass.getPixel(170, 70)
        assertTrue(Color.blue(center) > Color.red(center))
        assertTrue(Color.blue(glass.getPixel(170, 70)) - Color.blue(glass.getPixel(190, 70)) > 20)
        // Save the real Android renderer's output for visual QA, not a separate approximation.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.cacheDir, "liquid-glass-lens-qa.png").outputStream().use { glass.compress(Bitmap.CompressFormat.PNG, 100, it) }
        raw.recycle()
        glass.recycle()
    }

    @Test
    fun resizedWidgetAndSquareBottomCorners() {
        val raw = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        val glass = LiquidGlassEngine.compositeLiquidGlass(raw, 240, 260, false, cornerRadii = floatArrayOf(26f, 26f, 0f, 0f))
        assertEquals(240, glass.width)
        assertEquals(260, glass.height)
        assertEquals(0, Color.alpha(glass.getPixel(0, 0)))
        assertTrue(Color.alpha(glass.getPixel(2, 257)) > 240)
        raw.recycle()
        glass.recycle()
    }
}
