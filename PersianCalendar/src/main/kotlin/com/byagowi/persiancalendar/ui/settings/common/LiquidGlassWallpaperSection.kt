package com.byagowi.persiancalendar.ui.settings.common

import android.app.Activity
import android.graphics.BitmapFactory
import android.media.projection.MediaProjectionManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import com.byagowi.persiancalendar.R
import com.byagowi.persiancalendar.STORED_LIQUID_GLASS_PROCESSED
import com.byagowi.persiancalendar.STORED_LIQUID_GLASS_RAW
import com.byagowi.persiancalendar.global.liquidGlassWallpaperVersion
import com.byagowi.persiancalendar.ui.utils.AppBlendAlpha
import com.byagowi.persiancalendar.utils.LiquidGlassCaptureHelper
import com.byagowi.persiancalendar.utils.LiquidGlassEngine
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiquidGlassWallpaperSection(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val version = liquidGlassWallpaperVersion

    val isSet = remember(version) {
        LiquidGlassEngine.isWallpaperSet(context)
    }

    val previewBitmap = remember(version) {
        val file = File(context.filesDir, STORED_LIQUID_GLASS_PROCESSED).takeIf { it.exists() }
            ?: File(context.filesDir, STORED_LIQUID_GLASS_RAW).takeIf { it.exists() }
        if (file != null) BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() else null
    }

    // Screen Capture Launcher via MediaProjection
    val screenCaptureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            LiquidGlassCaptureHelper.startScreenCapture(
                context,
                result.resultCode,
                result.data!!,
            )
        }
    }

    // Gallery Photo Picker Launcher
    val galleryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            val success = LiquidGlassCaptureHelper.captureFromUri(context, uri)
            val msgRes = if (success) {
                R.string.liquid_glass_capture_success
            } else {
                R.string.liquid_glass_capture_error
            }
            Toast.makeText(context, msgRes, Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = stringResource(R.string.liquid_glass_wallpaper_setup),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.liquid_glass_wallpaper_setup_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = AppBlendAlpha),
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Wallpaper status & thumbnail preview
        AnimatedVisibility(visible = isSet && previewBitmap != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(12.dp),
                    )
                    .padding(8.dp),
            ) {
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap,
                        contentDescription = null,
                        modifier = Modifier
                            .size(width = 64.dp, height = 36.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50)),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.liquid_glass_wallpaper_active),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
                OutlinedIconButton(
                    onClick = {
                        LiquidGlassEngine.removeWallpaper(context)
                    },
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.liquid_glass_remove_wallpaper),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        // Action Buttons
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 1. Primary: Screen Capture (MediaProjection)
            Button(
                onClick = {
                    val projectionManager = context.getSystemService<MediaProjectionManager>()
                    if (projectionManager != null) {
                        runCatching {
                            screenCaptureLauncher.launch(projectionManager.createScreenCaptureIntent())
                        }.onFailure {
                            Toast.makeText(
                                context,
                                R.string.liquid_glass_capture_error,
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                },
            ) {
                Icon(Icons.Default.Screenshot, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.liquid_glass_capture_screen))
            }

            // 2. Auto from System Wallpaper (WallpaperManager)
            FilledTonalButton(
                onClick = {
                    val success = LiquidGlassCaptureHelper.captureFromWallpaperManager(context)
                    if (success) {
                        Toast.makeText(
                            context,
                            R.string.liquid_glass_capture_success,
                            Toast.LENGTH_SHORT,
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            R.string.liquid_glass_auto_failed,
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                },
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.liquid_glass_auto_wallpaper))
            }

            // 3. Pick screenshot/image from Gallery
            OutlinedButton(
                onClick = {
                    runCatching {
                        galleryPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    }
                },
            ) {
                Icon(Icons.Default.Image, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.liquid_glass_pick_image))
            }
        }
    }
}
