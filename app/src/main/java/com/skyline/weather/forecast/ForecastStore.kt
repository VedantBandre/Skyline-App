package com.skyline.weather.forecast

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

internal const val MAX_SAVED_CITIES = 8

private val Context.forecastStore by preferencesDataStore(name = "forecast")
private val cityListKey = stringPreferencesKey("saved_cities")
private val selectedIdKey = stringPreferencesKey("selected_city")

internal data class SavedCities(val cities: List<City>, val selectedId: String?) {
    val selected: City? get() = cities.firstOrNull { it.id == selectedId } ?: cities.firstOrNull()
}

internal class ForecastStore(context: Context) {
    private val dataStore = context.applicationContext.forecastStore

    val cities = dataStore.data.map { SavedCities(readCities(it), it[selectedIdKey]) }

    suspend fun select(city: City) {
        dataStore.edit { prefs ->
            val existing = readCities(prefs)
            if (existing.none { it.id == city.id }) {
                if (existing.size >= MAX_SAVED_CITIES) return@edit
                prefs[cityListKey] = JSONArray((existing + city).map(City::toJson)).toString()
            }
            prefs[selectedIdKey] = city.id
        }
    }

    suspend fun remove(city: City) {
        dataStore.edit { prefs ->
            val remaining = readCities(prefs).filterNot { it.id == city.id }
            prefs[cityListKey] = JSONArray(remaining.map(City::toJson)).toString()
            prefs.remove(forecastKey(city.id))
            if (prefs[selectedIdKey] == city.id) {
                val next = remaining.firstOrNull()
                if (next == null) prefs.remove(selectedIdKey) else prefs[selectedIdKey] = next.id
            }
        }
    }

    suspend fun cached(city: City, now: Long): Forecast? {
        val raw = dataStore.data.first()[forecastKey(city.id)] ?: return null
        val json = JSONObject(raw)
        val age = now - json.getLong("fetchedAt")
        if (age !in 0 until OUTLOOK_TTL_SECONDS) return null
        if (age >= CURRENT_TTL_SECONDS) json.remove("current")
        return Forecast.fromJson(json)
    }

    suspend fun save(city: City, json: JSONObject) {
        dataStore.edit { prefs ->
            if (readCities(prefs).any { it.id == city.id }) {
                val key = forecastKey(city.id)
                val previous = prefs[key]?.let(::JSONObject)?.getLong("fetchedAt")
                if (previous == null || previous <= json.getLong("fetchedAt")) {
                    prefs[key] = json.toString()
                }
            }
        }
    }

    suspend fun purgeExpired(now: Long) {
        dataStore.edit { prefs ->
            for (city in readCities(prefs)) {
                val key = forecastKey(city.id)
                val raw = prefs[key] ?: continue
                val json = JSONObject(raw)
                val age = now - json.getLong("fetchedAt")
                if (age !in 0 until OUTLOOK_TTL_SECONDS) {
                    prefs.remove(key)
                } else if (age >= CURRENT_TTL_SECONDS && json.has("current")) {
                    json.remove("current")
                    prefs[key] = json.toString()
                }
            }
        }
    }

    private fun readCities(prefs: Preferences): List<City> =
        JSONArray(prefs[cityListKey] ?: "[]").objects().map(City::fromJson)

    private fun forecastKey(id: String) = stringPreferencesKey("forecast_$id")
}
