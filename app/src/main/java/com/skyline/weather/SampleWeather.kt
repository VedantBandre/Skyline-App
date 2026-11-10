package com.skyline.weather

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

internal data class SampleHour(val time: String, val degreesCelsius: Int, val rainChance: Int)

internal enum class SampleWeather(
    val degreesCelsius: Int,
    @get:StringRes val condition: Int,
    @get:DrawableRes val icon: Int,
    @get:DrawableRes val widgetBackground: Int,
    val hours: List<SampleHour>,
) {
    CLEAR(
        degreesCelsius = 19,
        condition = R.string.sample_clear,
        icon = R.drawable.ic_widget_sun,
        widgetBackground = R.drawable.widget_surface,
        hours = listOf(
            SampleHour("18:00", 19, 5),
            SampleHour("19:00", 18, 5),
            SampleHour("20:00", 17, 10),
            SampleHour("21:00", 16, 10),
        ),
    ),
    RAIN(
        degreesCelsius = 12,
        condition = R.string.sample_rain,
        icon = R.drawable.ic_widget_rain,
        widgetBackground = R.drawable.widget_surface_rain,
        hours = listOf(
            SampleHour("18:00", 12, 80),
            SampleHour("19:00", 11, 75),
            SampleHour("20:00", 11, 60),
            SampleHour("21:00", 10, 50),
        ),
    ),
}

private val Context.sampleWeatherStore by preferencesDataStore(name = "sample_weather")
private val rainSelected = booleanPreferencesKey("rain_selected")

internal fun sampleWeather(context: Context) = context.sampleWeatherStore.data
    .catch { error ->
        if (error !is IOException) throw error
        emit(emptyPreferences())
    }
    .map { preferences ->
        if (preferences[rainSelected] == true) SampleWeather.RAIN else SampleWeather.CLEAR
    }

internal suspend fun switchSampleWeather(context: Context) {
    context.sampleWeatherStore.edit { preferences ->
        preferences[rainSelected] = preferences[rainSelected] != true
    }
}
