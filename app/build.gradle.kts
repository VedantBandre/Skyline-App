import java.net.URI
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val weatherBaseUrl = providers.gradleProperty("skylineWeatherBaseUrl")
    .orElse("")
    .get()
    .trim()
    .trimEnd('/')
val weatherUri = weatherBaseUrl.takeIf(String::isNotEmpty)?.let(::URI)
if (weatherUri != null) {
    require(
        (weatherUri.scheme == "https" ||
            (weatherUri.scheme == "http" && weatherUri.host == "10.0.2.2")) &&
            weatherUri.host != null && weatherUri.rawUserInfo == null &&
            weatherUri.rawQuery == null && weatherUri.rawFragment == null &&
            weatherUri.path.isEmpty(),
    ) { "skylineWeatherBaseUrl must be an HTTPS origin or the debug emulator host." }
}

android {
    namespace = "com.skyline.weather"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.skyline.weather"
        minSdk = 31
        targetSdk = 36
        versionCode = 3
        versionName = "0.3.0"
        val releaseOrigin = weatherBaseUrl.takeIf { weatherUri?.scheme == "https" }.orEmpty()
        buildConfigField("String", "WEATHER_BASE_URL", "\"$releaseOrigin\"")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".prototype"
            versionNameSuffix = "-debug"
            buildConfigField("String", "WEATHER_BASE_URL", "\"$weatherBaseUrl\"")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.foundation:foundation:1.9.4")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.glance:glance-appwidget:1.2.0")
}
