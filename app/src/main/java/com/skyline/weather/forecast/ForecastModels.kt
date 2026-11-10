package com.skyline.weather.forecast

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.json.JSONArray
import org.json.JSONObject

internal const val CURRENT_TTL_SECONDS = 3_600L
internal const val OUTLOOK_TTL_SECONDS = 86_400L
internal const val REFRESH_AFTER_SECONDS = 900L

internal data class City(val id: String, val name: String, val region: String, val country: String) {
    val area: String
        get() = listOf(region, country).filter(String::isNotBlank).distinct().joinToString(", ")

    fun toJson() = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("region", region)
        .put("country", country)

    companion object {
        fun fromJson(json: JSONObject) = City(
            id = json.getString("id"),
            name = json.getString("name"),
            region = json.optString("region"),
            country = json.getString("country"),
        )
    }
}

internal data class Conditions(val temperature: Double, val feelsLike: Double, val observedAt: Instant, val text: String)
internal data class WeatherHour(val time: Instant, val temperature: Double, val rainChance: Int)
internal data class WeatherDay(val date: LocalDate, val low: Double, val high: Double, val rainChance: Int, val text: String)

internal data class Forecast(
    val fetchedAt: Long,
    val timeZone: ZoneId,
    val current: Conditions?,
    val hourly: List<WeatherHour>,
    val daily: List<WeatherDay>,
) {
    companion object {
        fun fromJson(json: JSONObject): Forecast {
            val current = json.optJSONObject("current")
            return Forecast(
                fetchedAt = json.getLong("fetchedAt"),
                timeZone = ZoneId.of(json.getString("timeZone")),
                current = current?.let {
                    Conditions(
                        it.getDouble("temperature"),
                        it.getDouble("feelsLike"),
                        Instant.ofEpochSecond(it.getLong("observedAt")),
                        it.getJSONObject("condition").getString("text"),
                    )
                },
                hourly = json.getJSONArray("hourly").objects().map {
                    WeatherHour(
                        Instant.ofEpochSecond(it.getLong("time")),
                        it.getDouble("temperature"),
                        it.getInt("rainChance"),
                    )
                },
                daily = json.getJSONArray("daily").objects().map {
                    WeatherDay(
                        LocalDate.parse(it.getString("date")),
                        it.getDouble("low"),
                        it.getDouble("high"),
                        it.getInt("rainChance"),
                        it.getJSONObject("condition").getString("text"),
                    )
                },
            )
        }
    }
}

internal fun JSONArray.objects() = List(length()) { getJSONObject(it) }
