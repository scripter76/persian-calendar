package com.byagowi.persiancalendar.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.byagowi.persiancalendar.R
import com.byagowi.persiancalendar.WidgetGlass
import com.byagowi.persiancalendar.utils.LiquidGlassEngine
import com.byagowi.persiancalendar.utils.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Foreground service executing a brief screen capture of the home screen
 * to generate the Liquid Glass wallpaper background.
 */
class LiquidGlassCaptureService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val dataIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_DATA_INTENT, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_DATA_INTENT)
        }

        if (resultCode == 0 || dataIntent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.day19)
            .setContentTitle(getString(R.string.theme_liquid_glass))
            .setContentText(getString(R.string.liquid_glass_capturing_notification))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        serviceScope.launch {
            var mediaProjection: MediaProjection? = null
            var virtualDisplay: VirtualDisplay? = null
            var imageReader: ImageReader? = null
            val widgetManager = AppWidgetManager.getInstance(applicationContext)
            val widgetIds = widgetManager.getAppWidgetIds(ComponentName(applicationContext, WidgetGlass::class.java))
            try {
                // Capture the wallpaper below our widgets, avoiding a baked-in copy of old text/cards.
                if (widgetIds.isNotEmpty()) widgetManager.partiallyUpdateAppWidget(
                    widgetIds,
                    RemoteViews(packageName, R.layout.widget_glass).apply {
                        setViewVisibility(R.id.widget_layout_glass, View.INVISIBLE)
                    },
                )
                // 1. Switch to Home Screen to capture the actual wallpaper
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(homeIntent)

                // 2. Wait for launcher transition to complete
                delay(650)

                val projectionManager = getSystemService<MediaProjectionManager>()
                mediaProjection = projectionManager?.getMediaProjection(resultCode, dataIntent)
                if (mediaProjection == null) {
                    notifyResult(false)
                    return@launch
                }

                val metrics = resources.displayMetrics
                val width = metrics.widthPixels
                val height = metrics.heightPixels
                val densityDpi = metrics.densityDpi

                imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
                virtualDisplay = mediaProjection.createVirtualDisplay(
                    "LiquidGlassCapture",
                    width,
                    height,
                    densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.surface,
                    null,
                    null,
                )

                // 3. Acquire captured image
                var capturedBitmap: Bitmap? = null
                for (attempt in 0 until 12) {
                    delay(120)
                    val image = imageReader.acquireLatestImage()
                    if (image != null) {
                        val planes = image.planes
                        val buffer = planes[0].buffer
                        val pixelStride = planes[0].pixelStride
                        val rowStride = planes[0].rowStride
                        val rowPadding = rowStride - pixelStride * width

                        val tempBitmap = Bitmap.createBitmap(
                            width + rowPadding / pixelStride,
                            height,
                            Bitmap.Config.ARGB_8888,
                        )
                        tempBitmap.copyPixelsFromBuffer(buffer)
                        image.close()

                        capturedBitmap = if (rowPadding > 0) {
                            val cropped = Bitmap.createBitmap(tempBitmap, 0, 0, width, height)
                            tempBitmap.recycle()
                            cropped
                        } else {
                            tempBitmap
                        }
                        break
                    }
                }

                if (capturedBitmap != null) {
                    LiquidGlassEngine.processAndSave(applicationContext, capturedBitmap)
                    notifyResult(true)
                } else {
                    notifyResult(false)
                }
            } catch (e: Exception) {
                notifyResult(false)
            } finally {
                // Restore widgets even if projection permission is revoked or capture fails.
                if (widgetIds.isNotEmpty()) runCatching { update(applicationContext, false) }
                runCatching { virtualDisplay?.release() }
                runCatching { imageReader?.close() }
                runCatching { mediaProjection?.stop() }
                stopForegroundNotification()
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private suspend fun notifyResult(success: Boolean) {
        withContext(Dispatchers.Main) {
            val msgRes = if (success) {
                R.string.liquid_glass_capture_success
            } else {
                R.string.liquid_glass_capture_error
            }
            Toast.makeText(applicationContext, msgRes, Toast.LENGTH_LONG).show()
        }
    }

    private fun stopForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.theme_liquid_glass),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.theme_liquid_glass_desc)
                setShowBadge(false)
            }
            getSystemService<NotificationManager>()?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_DATA_INTENT = "extra_data_intent"
        private const val CHANNEL_ID = "liquid_glass_capture_channel"
        private const val NOTIFICATION_ID = 202609

        fun start(context: Context, resultCode: Int, dataIntent: Intent) {
            val serviceIntent = Intent(context, LiquidGlassCaptureService::class.java).apply {
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_DATA_INTENT, dataIntent)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
