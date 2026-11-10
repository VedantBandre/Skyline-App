package com.skyline.weather.forecast

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.skyline.weather.R
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val hourFormat = DateTimeFormatter.ofPattern("HH:mm")
private val dayFormat = DateTimeFormatter.ofPattern("EEE, MMM d")

@Composable
internal fun ForecastScreen(
    store: ForecastStore,
    service: WeatherService,
    foreground: Boolean,
    onOpenWidgetSample: () -> Unit,
) {
    val saved by store.cities.collectAsState(initial = null)
    val cities = saved?.cities.orEmpty()
    val selected = saved?.selected
    val scope = rememberCoroutineScope()
    var adding by rememberSaveable { mutableStateOf(false) }
    var storageFailed by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF235079), Color(0xFF101D30))))
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 22.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier.widthIn(max = 1060.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
            if (!service.configured) {
                Text(stringResource(R.string.weather_service_unconfigured))
            } else if (saved == null) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            } else {
                if (cities.isNotEmpty()) {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (city in cities) {
                            FilterChip(
                                selected = city.id == selected?.id,
                                onClick = {
                                    scope.launch { storageFailed = !tryWeather { store.select(city) } }
                                },
                                label = {
                                    val name = if (cities.count { it.name == city.name } > 1) {
                                        "${city.name} · ${city.area}"
                                    } else city.name
                                    Text(name)
                                },
                            )
                        }
                    }
                    TextButton(onClick = { adding = !adding }) {
                        Text(stringResource(if (adding) R.string.close_search else R.string.add_city))
                    }
                }
                if (cities.isEmpty() || adding) {
                    CitySearch(service, cities) { city ->
                        scope.launch {
                            storageFailed = !tryWeather { store.select(city) }
                            if (!storageFailed) adding = false
                        }
                    }
                }
                if (selected != null) {
                    CityForecast(selected, store, service, foreground) {
                        scope.launch { storageFailed = !tryWeather { store.remove(selected) } }
                    }
                }
                if (storageFailed) Text(stringResource(R.string.city_storage_failed))
                Text(stringResource(R.string.weather_credit), style = MaterialTheme.typography.labelMedium)
                Text(stringResource(R.string.weather_disclaimer), style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onOpenWidgetSample) { Text(stringResource(R.string.open_widget_sample)) }
        }
    }
}

@Composable
private fun CitySearch(service: WeatherService, saved: List<City>, onSelect: (City) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val input = query.trim()
    var attempt by remember { mutableIntStateOf(0) }
    var results by remember(input, attempt) { mutableStateOf<List<City>?>(null) }
    var failed by remember(input, attempt) { mutableStateOf(false) }
    val focus = LocalFocusManager.current

    LaunchedEffect(input, attempt) {
        if (input.length >= 2) {
            delay(350)
            failed = !tryWeather { results = service.search(input) }
        }
    }

    GlassPanel {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it.take(80) },
            label = { Text(stringResource(R.string.find_city)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        )
        val matches = results
        when {
            input.length < 2 -> Text(stringResource(R.string.search_hint))
            failed -> {
                Text(stringResource(R.string.search_unavailable))
                TextButton(onClick = { attempt++ }) { Text(stringResource(R.string.retry)) }
            }
            matches == null -> LinearProgressIndicator(Modifier.fillMaxWidth())
            matches.isEmpty() -> Text(stringResource(R.string.search_empty))
            else -> {
                for (city in matches) {
                    val canSelect = saved.size < MAX_SAVED_CITIES || saved.any { it.id == city.id }
                    TextButton(enabled = canSelect, onClick = {
                        focus.clearFocus()
                        onSelect(city)
                    }) {
                        Text("${city.name} · ${city.area}")
                    }
                }
                if (saved.size >= MAX_SAVED_CITIES) {
                    Text(stringResource(R.string.saved_city_limit))
                }
            }
        }
    }
}

@Composable
private fun CityForecast(
    city: City,
    store: ForecastStore,
    service: WeatherService,
    foreground: Boolean,
    onRemove: () -> Unit,
) {
    var forecast by remember(city.id) { mutableStateOf<Forecast?>(null) }
    var loading by remember(city.id) { mutableStateOf(true) }
    var failed by remember(city.id) { mutableStateOf(false) }
    var refresh by remember(city.id) { mutableIntStateOf(0) }
    var handledRefresh by remember(city.id) { mutableIntStateOf(0) }
    var now by remember { mutableLongStateOf(Instant.now().epochSecond) }

    LaunchedEffect(city.id, foreground, refresh) {
        if (!foreground) return@LaunchedEffect
        loading = true
        failed = false
        val requestedAt = Instant.now().epochSecond
        now = requestedAt
        tryWeather { store.purgeExpired(requestedAt) }
        tryWeather { forecast = store.cached(city, requestedAt) }
        val cached = forecast
        val manualRefresh = refresh != handledRefresh
        if (manualRefresh || cached == null || requestedAt - cached.fetchedAt >= REFRESH_AFTER_SECONDS) {
            failed = !tryWeather {
                val json = service.forecast(city)
                forecast = Forecast.fromJson(json)
                tryWeather { store.save(city, json) }
            }
        }
        handledRefresh = refresh
        loading = false
    }

    LaunchedEffect(forecast?.fetchedAt, foreground) {
        if (!foreground) return@LaunchedEffect
        while (true) {
            val instant = Instant.now()
            now = instant.epochSecond
            val expiry = forecast?.fetchedAt?.let { fetched ->
                listOf(fetched + CURRENT_TTL_SECONDS, fetched + OUTLOOK_TTL_SECONDS)
                    .firstOrNull { it > now }
            }
            val nextCheck = (expiry ?: (now + 30)).coerceAtMost(now + 30)
            delay((nextCheck * 1000 - instant.toEpochMilli()).coerceAtLeast(1))
        }
    }

    val data = forecast
    val age = data?.let { now - it.fetchedAt }
    val available = if (age != null && age in 0 until OUTLOOK_TTL_SECONDS) data else null
    val showCurrent = age != null && age in 0 until CURRENT_TTL_SECONDS
    LaunchedEffect(city.id, showCurrent, available != null) {
        tryWeather { store.purgeExpired(Instant.now().epochSecond) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(city.name, style = MaterialTheme.typography.headlineMedium)
        Text(city.area, style = MaterialTheme.typography.bodyMedium)
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (failed) {
            Text(stringResource(
                if (available == null) R.string.forecast_unavailable else R.string.cached_forecast,
            ))
        }
        if (available != null) ForecastCards(available, showCurrent, now)
        if (!loading && !failed && available == null) Text(stringResource(R.string.forecast_unavailable))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { refresh++ }, enabled = !loading) { Text(stringResource(R.string.refresh)) }
            TextButton(onClick = onRemove) { Text(stringResource(R.string.remove_city)) }
        }
    }
}

@Composable
private fun ForecastCards(forecast: Forecast, showCurrent: Boolean, now: Long) {
    val localDate = Instant.ofEpochSecond(now).atZone(forecast.timeZone).toLocalDate()
    val days = forecast.daily.filter { !it.date.isBefore(localDate) }
    val hours = forecast.hourly.filter { it.time.epochSecond > now }.take(12)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 700.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CurrentCard(forecast, showCurrent)
                    HourlyCard(forecast, hours)
                }
                DailyCard(days, localDate, Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                CurrentCard(forecast, showCurrent)
                HourlyCard(forecast, hours)
                DailyCard(days, localDate)
            }
        }
    }
}

@Composable
private fun CurrentCard(forecast: Forecast, showCurrent: Boolean) = GlassPanel {
    Text(stringResource(R.string.current_conditions), style = MaterialTheme.typography.titleMedium)
    val current = forecast.current.takeIf { showCurrent }
    if (current != null) {
        Text(temperature(current.temperature), style = MaterialTheme.typography.displayLarge)
        Text(current.text, style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.feels_like, temperature(current.feelsLike)))
        val time = current.observedAt.atZone(forecast.timeZone).format(hourFormat)
        Text(stringResource(R.string.observed_at, time), style = MaterialTheme.typography.bodySmall)
    } else {
        Text(stringResource(R.string.current_unavailable))
    }
}

@Composable
private fun HourlyCard(forecast: Forecast, hours: List<WeatherHour>) = GlassPanel {
    Text(stringResource(R.string.hourly_forecast), style = MaterialTheme.typography.titleMedium)
    if (hours.isEmpty()) Text(stringResource(R.string.hourly_unavailable))
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        for (hour in hours) {
            Column(Modifier.widthIn(min = 58.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(hour.time.atZone(forecast.timeZone).format(hourFormat))
                Text(temperature(hour.temperature), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.rain_percent, hour.rainChance))
            }
        }
    }
}

@Composable
private fun DailyCard(days: List<WeatherDay>, today: LocalDate, modifier: Modifier = Modifier) =
    GlassPanel(modifier) {
        Text(stringResource(R.string.daily_forecast), style = MaterialTheme.typography.titleMedium)
        for (day in days) {
            HorizontalDivider(color = Color.White.copy(alpha = 0.18f))
            val date = if (day.date == today) stringResource(R.string.today) else day.date.format(dayFormat)
            Text(date, style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.daily_temperatures, temperature(day.low), temperature(day.high)))
            Text("${day.text} · ${stringResource(R.string.rain_percent, day.rainChance)}")
        }
    }

@Composable
private fun GlassPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.10f), shape)
            .border(1.dp, Color.White.copy(alpha = 0.22f), shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun temperature(degrees: Double) =
    stringResource(R.string.sample_temperature, degrees.roundToInt())

private suspend fun tryWeather(action: suspend () -> Unit): Boolean = try {
    action()
    true
} catch (error: CancellationException) {
    throw error
} catch (_: Exception) {
    false
}
