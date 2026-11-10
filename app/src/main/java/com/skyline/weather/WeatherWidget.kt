package com.skyline.weather

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.flow.first

class WeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeatherWidget()
}

class HourlyWeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HourlyWeatherWidget()
}

internal class WeatherWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) =
        provideSample(context) { weather, scale -> CompactTile(weather, scale) }
}

internal class HourlyWeatherWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) =
        provideSample(context) { weather, scale -> HourlyTile(weather, scale) }
}

private suspend fun GlanceAppWidget.provideSample(
    context: Context,
    tile: @Composable (SampleWeather, Float) -> Unit,
) {
    val fontScale = context.resources.configuration.fontScale
    val weather = sampleWeather(context)
    val initialWeather = weather.first()
    provideContent {
        val currentWeather by weather.collectAsState(initialWeather)
        tile(currentWeather, fontScale)
    }
}

private val widgetText = ColorProvider(Color.White)
private val widgetMuted = ColorProvider(Color(0xFFD9EBFF))

@Composable
private fun CompactTile(weather: SampleWeather, fontScale: Float) {
    val context = LocalContext.current
    val size = LocalSize.current
    val short = size.height < 78.dp * fontScale
    val condition = context.getString(weather.condition)
    val sample = context.getString(R.string.sample_label)

    Column(
        modifier = tileSurface(weather).padding(if (short) 3.dp else 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (size.height >= 104.dp * fontScale) {
            Text(context.getString(R.string.sample_location), style = TextStyle(color = widgetMuted, fontSize = 11.sp), maxLines = 1)
        }
        Temperature(weather, showIcon = size.width >= 100.dp * fontScale)
        if (short) {
            Text(
                "$condition · $sample",
                style = TextStyle(color = widgetMuted, fontSize = 10.sp, textAlign = TextAlign.Center),
                maxLines = 2,
            )
        } else {
            Text(condition, style = TextStyle(color = widgetText, fontSize = 12.sp), maxLines = 1)
            Text(sample, style = TextStyle(color = widgetMuted, fontSize = 10.sp), maxLines = 1)
        }
    }
}

@Composable
private fun HourlyTile(weather: SampleWeather, fontScale: Float) {
    val context = LocalContext.current
    val size = LocalSize.current

    if (size.width < 162.dp * fontScale || size.height < 96.dp * fontScale) {
        CompactTile(weather, fontScale)
        return
    }
    val hourCount = when {
        size.width >= 244.dp * fontScale -> 4
        size.width >= 194.dp * fontScale -> 3
        else -> 2
    }

    Column(
        modifier = tileSurface(weather).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(GlanceModifier.defaultWeight()) {
                Text(
                    context.getString(R.string.sample_location),
                    style = TextStyle(color = widgetText, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
                Text(context.getString(R.string.widget_hourly_label), style = TextStyle(color = widgetMuted, fontSize = 10.sp), maxLines = 1)
            }
            Temperature(weather, showIcon = size.width >= 205.dp * fontScale)
        }
        Spacer(GlanceModifier.height(5.dp))
        Spacer(GlanceModifier.fillMaxWidth().height(1.dp).background(Color(0x55DAEBFF)))
        Spacer(GlanceModifier.height(5.dp))
        Row(GlanceModifier.fillMaxWidth()) {
            for (hour in weather.hours.take(hourCount)) {
                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(hour.time, style = TextStyle(color = widgetMuted, fontSize = 11.sp), maxLines = 1)
                    Text(
                        context.getString(R.string.sample_temperature, hour.degreesCelsius),
                        style = TextStyle(color = widgetText, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                    )
                    Text(
                        context.getString(R.string.rain_percent, hour.rainChance),
                        style = TextStyle(color = widgetMuted, fontSize = 10.sp),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun Temperature(weather: SampleWeather, showIcon: Boolean) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (showIcon) {
            Image(ImageProvider(weather.icon), contentDescription = null, modifier = GlanceModifier.size(22.dp))
            Spacer(GlanceModifier.width(4.dp))
        }
        Text(
            context.getString(R.string.sample_temperature, weather.degreesCelsius),
            style = TextStyle(color = widgetText, fontSize = 24.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
    }
}

@Composable
private fun tileSurface(weather: SampleWeather): GlanceModifier {
    val context = LocalContext.current
    return GlanceModifier
        .fillMaxSize()
        .appWidgetBackground()
        .background(ImageProvider(weather.widgetBackground))
        .cornerRadius(android.R.dimen.system_app_widget_background_radius)
        .clickable(actionStartActivity(Intent(context, SampleWeatherActivity::class.java)))
}
