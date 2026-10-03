package com.byagowi.persiancalendar.ui.theme

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.byagowi.persiancalendar.global.isLiquidGlassDark
import com.byagowi.persiancalendar.utils.LiquidGlassEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal data class GlassWallpaper(val raw: Bitmap, val softened: Bitmap)
internal data class GlassBackdrop(val wallpaper: GlassWallpaper, val bounds: Rect)
internal val LocalGlassBackdrop = staticCompositionLocalOf<GlassBackdrop?> { null }

/** Wallpaper-space sampling keeps each lens aligned with the crisp full-screen backdrop. */
@Composable
internal fun Modifier.liquidGlass(shape: Shape, illumination: Float = 0f): Modifier {
    val backdrop = LocalGlassBackdrop.current ?: return this
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val dark = isLiquidGlassDark
    val lens by produceState<Bitmap?>(null, backdrop, bounds, shape, density, direction, dark) {
        if (bounds.width <= 0f || bounds.height <= 0f || backdrop.bounds.width <= 0f || backdrop.bounds.height <= 0f) return@produceState
        value = withContext(Dispatchers.Default) {
            val renderScale = min(1f, min(800f / bounds.width, 1000f / bounds.height))
            val width = (bounds.width * renderScale).roundToInt().coerceAtLeast(1)
            val height = (bounds.height * renderScale).roundToInt().coerceAtLeast(1)
            val raw = backdrop.wallpaper.raw
            // Exactly the same center-crop transform used by AppTheme's Image.
            val scale = max(backdrop.bounds.width / raw.width, backdrop.bounds.height / raw.height)
            val left = backdrop.bounds.left + (backdrop.bounds.width - raw.width * scale) / 2f
            val top = backdrop.bounds.top + (backdrop.bounds.height - raw.height * scale) / 2f
            val source = RectF(
                (bounds.left - left) / scale, (bounds.top - top) / scale,
                (bounds.right - left) / scale, (bounds.bottom - top) / scale,
            )
            val outline = shape.createOutline(Size(width.toFloat(), height.toFloat()), direction, Density(density.density * renderScale))
            val radii = if (outline is Outline.Rounded) outline.roundRect.let {
                floatArrayOf(it.topLeftCornerRadius.x, it.topRightCornerRadius.x, it.bottomRightCornerRadius.x, it.bottomLeftCornerRadius.x)
            } else FloatArray(4)
            LiquidGlassEngine.compositeLiquidGlass(raw, width, height, dark, source, radii, backdrop.wallpaper.softened)
        }
    }
    val image = remember(lens) { lens?.asImageBitmap() }
    return this
        .onGloballyPositioned { bounds = it.boundsInRoot() }
        .shadow(6.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = .12f), spotColor = Color.Black.copy(alpha = .16f))
        .clip(shape)
        .drawWithContent {
            if (image != null) drawImage(image, dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()))
            else drawOutline(shape.createOutline(size, layoutDirection, this), Color.White.copy(alpha = .12f))
            if (illumination > 0f) drawRect(Brush.radialGradient(listOf(Color.White.copy(alpha = .18f * illumination), Color.Transparent)))
            drawContent()
            drawOutline(
                shape.createOutline(size, layoutDirection, this),
                Brush.linearGradient(listOf(Color.White.copy(alpha = .55f), Color.White.copy(alpha = .06f), Color.White.copy(alpha = .28f))),
                style = Stroke(.5.dp.toPx()),
            )
        }
}
