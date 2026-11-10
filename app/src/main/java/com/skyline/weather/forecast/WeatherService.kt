package com.skyline.weather.forecast

import android.net.Uri
import com.skyline.weather.BuildConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal class WeatherService(private val origin: String = BuildConfig.WEATHER_BASE_URL) {
    val configured: Boolean get() = origin.isNotEmpty()

    suspend fun search(query: String): List<City> =
        request("search", "q", query).getJSONArray("cities").objects().map(City::fromJson)

    suspend fun forecast(city: City): JSONObject {
        val json = request("forecast", "id", city.id)
        if (json.getString("cityId") != city.id) throw IOException("Forecast city mismatch")
        return json
    }

    private suspend fun request(route: String, parameter: String, value: String): JSONObject =
        withContext(Dispatchers.IO) {
            val url = URI.create("$origin/api/v1/$route?$parameter=${Uri.encode(value)}").toURL()
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 5_000
                connection.readTimeout = 10_000
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("Accept", "application/json")
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException("Weather request failed (HTTP ${connection.responseCode})")
                }
                connection.inputStream.bufferedReader(Charsets.UTF_8).use {
                    JSONObject(it.readText())
                }
            } finally {
                connection.disconnect()
            }
        }
}
