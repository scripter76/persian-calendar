package com.byagowi.persiancalendar.utils

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import com.byagowi.persiancalendar.service.LiquidGlassCaptureService

object LiquidGlassCaptureHelper {

    /**
     * Tries to capture the current system wallpaper using WallpaperManager.
     * Returns true if successful, false if restricted or null.
     */
    fun captureFromWallpaperManager(context: Context): Boolean {
        return runCatching {
            val wm = WallpaperManager.getInstance(context)
            val drawable: Drawable? = wm.drawable ?: wm.fastDrawable ?: wm.peekFastDrawable()
            if (drawable != null) {
                val bitmap = drawableToBitmap(drawable)
                LiquidGlassEngine.processAndSave(context, bitmap)
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    /**
     * Ingests a wallpaper or home screen screenshot picked from the device gallery.
     */
    fun captureFromUri(context: Context, uri: Uri): Boolean {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    LiquidGlassEngine.processAndSave(context, bitmap)
                    true
                } else {
                    false
                }
            } ?: false
        }.getOrDefault(false)
    }

    /**
     * Initiates the background home screen capture service with the user-granted MediaProjection intent.
     */
    fun startScreenCapture(context: Context, resultCode: Int, dataIntent: Intent) {
        LiquidGlassCaptureService.start(context, resultCode, dataIntent)
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 720
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1280
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
