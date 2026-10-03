package com.byagowi.persiancalendar

import com.byagowi.persiancalendar.utils.LiquidGlassOptics
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LiquidGlassOpticsTests {
    private val optics = LiquidGlassOptics(340f, 140f, FloatArray(4) { 26f })

    @Test
    fun `center does not shift and body has no bevel distortion`() {
        val sample = FloatArray(3)
        optics.sample(170f, 70f, sample)
        assertEquals(170f, sample[0])
        assertEquals(70f, sample[1])
        assertEquals(0f, sample[2])
        optics.sample(100f, 70f, sample)
        assertEquals(0f, sample[2])
        assertTrue(abs(sample[0] - 100f) < 2f)
    }

    @Test
    fun `lensing follows opposing edge normals symmetrically`() {
        val left = FloatArray(3)
        val right = FloatArray(3)
        optics.sample(5f, 70f, left)
        optics.sample(335f, 70f, right)
        assertTrue(left[0] < 5f)
        assertTrue(right[0] > 335f)
        assertEquals(340f, left[0] + right[0], .001f)
        assertEquals(left[2], right[2], .001f)
    }

    @Test
    fun `round corners refract along both axes`() {
        val sample = FloatArray(3)
        optics.sample(12f, 12f, sample)
        assertTrue(sample[0] < 12f && sample[1] < 12f)
        assertTrue(sample[2] > 0f)
    }

    @Test
    fun `bevel joins the body continuously`() {
        val before = FloatArray(3)
        val after = FloatArray(3)
        optics.sample(10.499f, 70f, before)
        optics.sample(10.501f, 70f, after)
        assertTrue(abs(before[0] - after[0]) < .01f)
        assertTrue(before[2] < .001f && after[2] == 0f)
    }

    @Test
    fun `small lenses and square lower corners remain finite`() {
        val sample = FloatArray(3)
        val small = LiquidGlassOptics(1f, 1f, FloatArray(4) { 26f })
        small.sample(.5f, .5f, sample)
        assertTrue(sample.all(Float::isFinite))
        val panel = LiquidGlassOptics(340f, 140f, floatArrayOf(26f, 26f, 0f, 0f))
        panel.sample(1f, 139f, sample)
        assertTrue(sample.all(Float::isFinite))
    }
}
