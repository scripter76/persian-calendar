package com.byagowi.persiancalendar.entities

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.byagowi.persiancalendar.R

enum class GlassWidgetTheme(
    @get:StringRes val titleRes: Int,
    @get:DrawableRes val backgroundDrawable: Int,
    @get:DrawableRes val cardBackgroundDrawable: Int,
    @get:DrawableRes val bottomBarBackgroundDrawable: Int,
    val isDark: Boolean,
) {
    LIQUID_GLASS(
        R.string.theme_liquid_glass,
        R.drawable.bg_glass_ios_clear,
        R.drawable.bg_glass_card_liquid,
        R.drawable.bg_glass_bottom_bar_liquid,
        isDark = false,
    ),
    MOSQUE_SUNSET(
        R.string.theme_glass_mosque,
        R.drawable.bg_glass_mosque,
        R.drawable.bg_glass_card,
        R.drawable.bg_glass_bottom_bar,
        isDark = false,
    ),
    NIGHT_VECTOR(
        R.string.theme_glass_night_vector,
        R.drawable.bg_glass_night,
        R.drawable.bg_glass_card,
        R.drawable.bg_glass_bottom_bar,
        isDark = true,
    ),
    IOS_CLEAR(
        R.string.theme_glass_ios_clear,
        R.drawable.bg_glass_ios_clear,
        R.drawable.bg_glass_card,
        R.drawable.bg_glass_bottom_bar,
        isDark = false,
    ),
    DARK_OBSIDIAN(
        R.string.theme_glass_dark,
        R.drawable.bg_glass_dark,
        R.drawable.bg_glass_card_dark,
        R.drawable.bg_glass_bottom_bar_dark,
        isDark = true,
    ),
    AZURE_PERSIAN(
        R.string.theme_glass_azure,
        R.drawable.bg_glass_azure,
        R.drawable.bg_glass_card,
        R.drawable.bg_glass_bottom_bar,
        isDark = false,
    );

    companion object {
        val DEFAULT = MOSQUE_SUNSET
        fun fromName(name: String?): GlassWidgetTheme =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
