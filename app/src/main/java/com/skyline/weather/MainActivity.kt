package com.skyline.weather

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.skyline.weather.forecast.ForecastScreen
import com.skyline.weather.forecast.ForecastStore
import com.skyline.weather.forecast.WeatherService

class MainActivity : ComponentActivity() {
    private var foreground by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val darkBar = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = darkBar, navigationBarStyle = darkBar)

        val service = WeatherService()
        val store = ForecastStore(applicationContext)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFFB6DFFF),
                onPrimary = Color(0xFF102842),
            )) {
                Surface {
                    ForecastScreen(store, service, foreground) {
                        startActivity(Intent(this, SampleWeatherActivity::class.java))
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        foreground = true
    }

    override fun onStop() {
        foreground = false
        super.onStop()
    }
}
