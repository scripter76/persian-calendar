package com.byagowi.persiancalendar.ui.settings.widgetnotification

import android.widget.RemoteViews
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.core.content.edit
import com.byagowi.persiancalendar.DEFAULT_WIDGET_GLASS_Y_POS
import com.byagowi.persiancalendar.OTHER_CALENDARS_KEY
import com.byagowi.persiancalendar.PREF_WIDGET_GLASS_Y_POS
import com.byagowi.persiancalendar.R
import com.byagowi.persiancalendar.entities.Clock
import com.byagowi.persiancalendar.entities.Jdn
import com.byagowi.persiancalendar.global.coordinates
import com.byagowi.persiancalendar.global.mainCalendar
import com.byagowi.persiancalendar.global.prefersWidgetsDynamicColors
import com.byagowi.persiancalendar.global.spacedComma
import com.byagowi.persiancalendar.global.whatToShowOnWidgets
import com.byagowi.persiancalendar.ui.settings.SettingsSectionLayout
import com.byagowi.persiancalendar.ui.settings.common.LiquidGlassWallpaperSection
import com.byagowi.persiancalendar.ui.settings.interfacecalendar.WeekOfYearSetting
import com.byagowi.persiancalendar.ui.settings.locationathan.LocationSettings
import com.byagowi.persiancalendar.ui.utils.AppBlendAlpha
import com.byagowi.persiancalendar.utils.LiquidGlassEngine
import com.byagowi.persiancalendar.utils.calculatePrayTimes
import com.byagowi.persiancalendar.utils.create1x1RemoteViews
import com.byagowi.persiancalendar.utils.create2x2RemoteViews
import com.byagowi.persiancalendar.utils.create4x1RemoteViews
import com.byagowi.persiancalendar.utils.create4x2RemoteViews
import com.byagowi.persiancalendar.utils.createMapRemoteViews
import com.byagowi.persiancalendar.utils.createMonthViewRemoteViews
import com.byagowi.persiancalendar.utils.createSunViewRemoteViews
import com.byagowi.persiancalendar.utils.createWeekViewRemoteViews
import com.byagowi.persiancalendar.utils.dateStringOfOtherCalendars
import com.byagowi.persiancalendar.utils.dayTitleSummary
import com.byagowi.persiancalendar.utils.preferences
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.byagowi.persiancalendar.PREF_WIDGET_GLASS_THEME
import com.byagowi.persiancalendar.PREF_WIDGET_GLASS_LANGUAGE
import com.byagowi.persiancalendar.PREF_WIDGET_IN_24
import com.byagowi.persiancalendar.PREF_WIDGET_CLOCK_ZERO_PADDING
import com.byagowi.persiancalendar.entities.GlassWidgetLanguage
import com.byagowi.persiancalendar.entities.GlassWidgetTheme
import com.byagowi.persiancalendar.global.clockIn24
import com.byagowi.persiancalendar.global.isClockZeroPadding
import com.byagowi.persiancalendar.ui.settings.SettingsSingleSelect
import com.byagowi.persiancalendar.ui.settings.SettingsSwitch
import com.byagowi.persiancalendar.utils.createGlassRemoteViews
import androidx.compose.ui.unit.dp
import com.byagowi.persiancalendar.tehranCoordinates
import com.byagowi.persiancalendar.global.cityName
import java.util.GregorianCalendar

class Widget1x1ConfigurationActivity : BaseWidgetConfigurationActivity() {
    override val defaultWidgetSize get() = DpSize(100.dp, 100.dp)

    override fun preview(size: DpSize): RemoteViews {
        return create1x1RemoteViews(
            this, size, Jdn.today() on mainCalendar, preferences, appWidgetId,
        )
    }

    @Composable
    override fun ColumnScope.Settings() {
        TextScaleSettings()
        WidgetColoringSettings()
    }
}

class Widget4x1ConfigurationActivity : BaseWidgetConfigurationActivity() {
    override fun preview(size: DpSize): RemoteViews {
        val jdn = Jdn.today()
        val today = jdn on mainCalendar
        val clock = Clock(GregorianCalendar())
        val subtitle = dateStringOfOtherCalendars(jdn, spacedComma)
        val widgetTitle = dayTitleSummary(
            jdn,
            today,
            calendarNameInLinear = OTHER_CALENDARS_KEY in whatToShowOnWidgets,
        )
        return create4x1RemoteViews(
            this, size, jdn, today, widgetTitle, subtitle, clock,
            preferences, appWidgetId,
        )
    }

    @Composable
    override fun ColumnScope.Settings() {
        TextScaleSettings()
        WidgetSettings()
    }
}

class Widget2x2ConfigurationActivity : BaseWidgetConfigurationActivity() {
    override fun preview(size: DpSize): RemoteViews {
        val jdn = Jdn.today()
        val today = jdn on mainCalendar
        val clock = Clock(GregorianCalendar())
        val prayTimes = coordinates?.calculatePrayTimes()
        val subtitle = dateStringOfOtherCalendars(jdn, spacedComma)
        val widgetTitle = dayTitleSummary(
            jdn,
            today,
            calendarNameInLinear = OTHER_CALENDARS_KEY in whatToShowOnWidgets,
        )
        return create2x2RemoteViews(
            this, size, jdn, today, widgetTitle, subtitle, prayTimes,
            clock, preferences, appWidgetId,
        )
    }

    @Composable
    override fun ColumnScope.Settings() {
        TextScaleSettings()
        WidgetSettings()
    }
}

class Widget4x2ConfigurationActivity : BaseWidgetConfigurationActivity() {
    override fun preview(size: DpSize): RemoteViews {
        val jdn = Jdn.today()
        val date = jdn on mainCalendar
        val clock = Clock(GregorianCalendar())
        return create4x2RemoteViews(
            this,
            size,
            Jdn.today(),
            date,
            clock,
            coordinates?.calculatePrayTimes(),
            preferences,
            appWidgetId,
        )
    }

    @Composable
    override fun ColumnScope.Settings() {
        TextScaleSettings()
        WidgetSettings()
        SettingsSectionLayout(R.string.location)
        LocationSettings()
    }
}

@Composable
fun GlassWidgetThemeSetting(widgetId: Int? = null) {
    val context = LocalContext.current
    val key = if (widgetId != null) PREF_WIDGET_GLASS_THEME + widgetId else PREF_WIDGET_GLASS_THEME
    val persistedValue = context.preferences.getString(key, null)
        ?: context.preferences.getString(PREF_WIDGET_GLASS_THEME, null)
        ?: GlassWidgetTheme.DEFAULT.name
    val themes = remember { GlassWidgetTheme.entries }
    SettingsSingleSelect(
        key = key,
        entries = themes.map { stringResource(it.titleRes) },
        entryValues = themes.map { it.name },
        persistedValue = persistedValue,
        dialogTitleResId = R.string.widget_glass_theme,
        title = stringResource(R.string.widget_glass_theme),
    )
    if (persistedValue == GlassWidgetTheme.LIQUID_GLASS.name) {
        LiquidGlassWallpaperSection(modifier = Modifier.padding(horizontal = 16.dp))
        WidgetGlassPositionSetting(widgetId = widgetId)
    }
}

@Composable
fun WidgetGlassPositionSetting(widgetId: Int? = null) {
    val context = LocalContext.current
    val key = if (widgetId != null) PREF_WIDGET_GLASS_Y_POS + widgetId else PREF_WIDGET_GLASS_Y_POS
    val currentVal = context.preferences.getInt(
        key,
        context.preferences.getInt(PREF_WIDGET_GLASS_Y_POS, DEFAULT_WIDGET_GLASS_Y_POS),
    )
    var positionState by rememberSaveable(currentVal) { mutableFloatStateOf(currentVal.toFloat()) }

    Column(Modifier.padding(vertical = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.widget_glass_position),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.widget_glass_position_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = AppBlendAlpha),
                )
            }
            Text(
                text = "${positionState.toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val presets = listOf(
                Pair(R.string.widget_position_top, 10),
                Pair(R.string.widget_position_upper_mid, 28),
                Pair(R.string.widget_position_center, 50),
                Pair(R.string.widget_position_bottom, 75),
            )
            presets.forEach { (titleRes, value) ->
                val isSelected = (positionState.toInt() in (value - 6)..(value + 6))
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        positionState = value.toFloat()
                        context.preferences.edit { putInt(key, value) }
                        LiquidGlassEngine.cropAndSaveWidgetLiquidGlass(context, value.toFloat())
                    },
                    label = { Text(stringResource(titleRes), style = MaterialTheme.typography.labelSmall) },
                )
            }
        }

        Slider(
            value = positionState,
            onValueChange = { positionState = it },
            onValueChangeFinished = {
                val intVal = positionState.toInt()
                context.preferences.edit { putInt(key, intVal) }
                LiquidGlassEngine.cropAndSaveWidgetLiquidGlass(context, intVal.toFloat())
            },
            valueRange = 0f..100f,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
fun GlassWidgetLanguageSetting(widgetId: Int? = null) {
    val context = LocalContext.current
    val key =
        if (widgetId != null) PREF_WIDGET_GLASS_LANGUAGE + widgetId else PREF_WIDGET_GLASS_LANGUAGE
    val persistedValue = context.preferences.getString(key, null)
        ?: context.preferences.getString(PREF_WIDGET_GLASS_LANGUAGE, null)
        ?: GlassWidgetLanguage.DEFAULT.name
    val languages = remember { GlassWidgetLanguage.entries }
    SettingsSingleSelect(
        key = key,
        entries = listOf("فارسی", "English"),
        entryValues = languages.map { it.name },
        persistedValue = persistedValue,
        dialogTitleResId = R.string.widget_glass_language,
        title = stringResource(R.string.widget_glass_language),
    )
}

class WidgetGlassConfigurationActivity : BaseWidgetConfigurationActivity() {
    override val defaultWidgetSize get() = DpSize(320.dp, 150.dp)

    override fun preview(size: DpSize): RemoteViews {
        val jdn = Jdn.today()
        val date = jdn on mainCalendar
        val clock = Clock(GregorianCalendar())
        val effCoords = coordinates ?: tehranCoordinates
        return createGlassRemoteViews(
            this,
            size,
            jdn,
            date,
            clock,
            effCoords.calculatePrayTimes(),
            preferences,
            appWidgetId,
            fallbackCityName = cityName ?: "تهران",
        )
    }

    @Composable
    override fun ColumnScope.Settings() {
        TextScaleSettings()
        GlassWidgetThemeSetting(appWidgetId)
        GlassWidgetLanguageSetting(appWidgetId)
        SettingsSwitch(
            key = PREF_WIDGET_IN_24,
            value = clockIn24,
            title = stringResource(R.string.clock_in_24),
            summary = stringResource(R.string.showing_clock_in_24),
        )
        SettingsSwitch(
            key = PREF_WIDGET_CLOCK_ZERO_PADDING,
            value = isClockZeroPadding,
            title = stringResource(R.string.clock_zero_padding),
            summary = stringResource(R.string.showing_clock_zero_padding),
        )
        SettingsSectionLayout(R.string.location)
        LocationSettings()
    }
}

class WidgetWeekViewConfigurationActivity : BaseWidgetConfigurationActivity() {
    override fun preview(size: DpSize): RemoteViews {
        val today = Jdn.today()
        val date = today on mainCalendar
        return createWeekViewRemoteViews(this, size, date, today, preferences, appWidgetId)
    }

    @Composable
    override fun ColumnScope.Settings() {
        TextScaleSettings()
        WidgetColoringSettings()
    }
}

class WidgetSunViewConfigurationActivity : BaseWidgetConfigurationActivity() {
    override fun preview(size: DpSize): RemoteViews =
        createSunViewRemoteViews(this, size, System.currentTimeMillis())

    @Composable
    override fun ColumnScope.Settings() {
        WidgetColoringSettings()
        SettingsSectionLayout(R.string.location)
        LocationSettings()
    }
}

class WidgetMapConfigurationActivity : BaseWidgetConfigurationActivity() {
    override fun preview(size: DpSize): RemoteViews =
        createMapRemoteViews(this, size, System.currentTimeMillis())

    @Composable
    override fun ColumnScope.Settings() {
        WidgetDynamicColorsGlobalSettings(prefersWidgetsDynamicColors)
    }
}

class WidgetMonthViewConfigurationActivity : BaseWidgetConfigurationActivity() {
    override fun preview(size: DpSize): RemoteViews =
        createMonthViewRemoteViews(this, size, Jdn.today())

    @Composable
    override fun ColumnScope.Settings() {
        WeekOfYearSetting()
        WidgetColoringSettings()
    }
}
