package com.skyline.weather

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val darkBar = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = darkBar, navigationBarStyle = darkBar)

        val weather = sampleWeather(applicationContext)
        setContent {
            val currentWeather by weather.collectAsState(SampleWeather.CLEAR)
            val scope = rememberCoroutineScope()
            var refreshing by remember { mutableStateOf(false) }
            var refreshFailed by remember { mutableStateOf(false) }

            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFB6DFFF))) {
                Surface(
                    color = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ) {
                    PrototypeScreen(
                        weather = currentWeather,
                        refreshing = refreshing,
                        refreshFailed = refreshFailed,
                        onSwitchSample = {
                            refreshing = true
                            refreshFailed = false
                            scope.launch {
                                try {
                                    switchSampleWeather(applicationContext)
                                    WeatherWidget().updateAll(applicationContext)
                                } catch (error: CancellationException) {
                                    throw error
                                } catch (error: Exception) {
                                    Log.w("Skyline", "Could not refresh the sample widget", error)
                                    refreshFailed = true
                                } finally {
                                    refreshing = false
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PrototypeScreen(
    weather: SampleWeather,
    refreshing: Boolean,
    refreshFailed: Boolean,
    onSwitchSample: () -> Unit,
) {
    val topColor = if (weather == SampleWeather.CLEAR) Color(0xFF26527C) else Color(0xFF37475F)
    val cardShape = RoundedCornerShape(28.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(topColor, Color(0xFF101D30))))
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.10f), cardShape)
                    .border(1.dp, Color.White.copy(alpha = 0.24f), cardShape)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.prototype_label),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = stringResource(R.string.sample_location),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(R.string.sample_temperature, weather.degreesCelsius),
                    style = MaterialTheme.typography.displayLarge,
                )
                Text(
                    text = stringResource(weather.condition),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                text = stringResource(R.string.prototype_help),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onSwitchSample, enabled = !refreshing) {
                Text(stringResource(R.string.switch_sample))
            }
            if (refreshFailed) {
                Text(
                    text = stringResource(R.string.refresh_failed),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
