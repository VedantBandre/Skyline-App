package com.skyline.weather

import android.content.Context
import androidx.annotation.StringRes
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

internal enum class SampleWeather(val degreesCelsius: Int, @get:StringRes val condition: Int) {
    CLEAR(19, R.string.sample_clear),
    RAIN(12, R.string.sample_rain),
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
