package com.byagowi.persiancalendar

import com.byagowi.persiancalendar.entities.GlassWidgetTheme
import com.byagowi.persiancalendar.ui.theme.Theme
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class LiquidGlassThemeTests {

    @Test
    fun `liquid glass app theme is defined properly`() {
        val theme = Theme.LIQUID_GLASS
        assertEquals("LiquidGlass", theme.key)
        assertEquals(R.string.theme_liquid_glass, theme.title)
        assertFalse(theme.hasGradient)
        assertEquals(false, theme.isDark)
    }

    @Test
    fun `liquid glass widget theme is defined properly`() {
        val widgetTheme = GlassWidgetTheme.LIQUID_GLASS
        assertEquals(R.string.theme_liquid_glass, widgetTheme.titleRes)
        assertEquals(R.drawable.bg_glass_card_liquid, widgetTheme.cardBackgroundDrawable)
        assertEquals(R.drawable.bg_glass_bottom_bar_liquid, widgetTheme.bottomBarBackgroundDrawable)
        assertFalse(widgetTheme.isDark)

        val resolved = GlassWidgetTheme.fromName("LIQUID_GLASS")
        assertEquals(GlassWidgetTheme.LIQUID_GLASS, resolved)
    }

    @Test
    fun `liquid glass constants are defined`() {
        assertNotNull(STORED_LIQUID_GLASS_RAW)
        assertNotNull(STORED_LIQUID_GLASS_MASTER_BLUR)
        assertNotNull(STORED_LIQUID_GLASS_PROCESSED)
        assertNotNull(PREF_LIQUID_GLASS_IS_DARK)
        assertNotNull(PREF_LIQUID_GLASS_UPDATED_AT)
        assertNotNull(PREF_WIDGET_GLASS_Y_POS)
        assertEquals(15, DEFAULT_WIDGET_GLASS_Y_POS)
    }
}
