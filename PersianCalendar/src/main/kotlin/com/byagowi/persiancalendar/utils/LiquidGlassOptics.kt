package com.byagowi.persiancalendar.utils

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Rounded lens geometry, independent of Android so the optical mapping can be tested on the JVM. */
internal class LiquidGlassOptics(
    private val width: Float,
    private val height: Float,
    // Clockwise from top left. Screen surfaces can have square bottom corners.
    private val radii: FloatArray,
) {
    private val halfWidth = width / 2f
    private val halfHeight = height / 2f
    private val bevel = (min(width, height) * .075f).coerceAtLeast(.5f)

    /** Writes source x, source y and rim strength into [result], without allocating per pixel. */
    fun sample(x: Float, y: Float, result: FloatArray) {
        val px = x - halfWidth
        val py = y - halfHeight
        val radius = radii[if (py < 0) { if (px < 0) 0 else 1 } else { if (px < 0) 3 else 2 }]
            .coerceIn(0f, min(halfWidth, halfHeight))
        val qx = abs(px) - halfWidth + radius
        val qy = abs(py) - halfHeight + radius
        val ox = max(qx, 0f)
        val oy = max(qy, 0f)
        val length = sqrt(ox * ox + oy * oy)
        val distance = -(length + min(max(qx, qy), 0f) - radius)
        val nx = (if (length > 0f) ox / length else if (qx > qy) 1f else 0f) * if (px < 0) -1f else 1f
        val ny = (if (length > 0f) oy / length else if (qx > qy) 0f else 1f) * if (py < 0) -1f else 1f
        val t = (distance / bevel).coerceIn(0f, 1f)
        // A smooth meniscus confined to the bevel. Zero at both ends avoids a visible seam.
        val rim = if (distance > 0f && distance < bevel) sin(t * Math.PI).toFloat() else 0f
        val bend = rim * bevel * .42f
        // Gentle magnification in the body; stronger, normal-directed refraction at the rim.
        result[0] = x - px * .018f + nx * bend
        result[1] = y - py * .018f + ny * bend
        result[2] = rim
    }
}
