package com.immichframe.app.ui.slideshow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.immichframe.app.SlideshowDefaults
import com.immichframe.app.WeatherApi
import com.immichframe.app.WeatherSnapshot
import kotlinx.coroutines.delay

/** Top-left pill showing current temperature + condition emoji for the given coords. */
@Composable
internal fun WeatherOverlay(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier,
) {
    val weatherApi: WeatherApi = org.koin.compose.koinInject()
    @Suppress("ProduceStateDoesNotAssignValue")
    val weather by produceState<WeatherSnapshot?>(null, latitude, longitude, weatherApi) {
        value = weatherApi.fetch(latitude, longitude)
        while (true) {
            delay(SlideshowDefaults.WEATHER_REFRESH_MS)
            val fresh = weatherApi.fetch(latitude, longitude)
            if (fresh != null) value = fresh
        }
    }
    val w = weather ?: return
    Row(
        modifier = modifier
            .padding(start = 24.dp, top = 24.dp)
            .background(Color(0x66000000), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = emojiForWmo(w.weatherCode), style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.padding(start = 12.dp))
        Text(
            text = "${w.tempCelsius.toInt()}°C",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}

internal fun emojiForWmo(code: Int): String = when (code) {
    0 -> "☀️"
    1 -> "🌤️"
    2 -> "⛅"
    3 -> "☁️"
    45, 48 -> "🌫️"
    in 51..57 -> "🌦️"
    in 61..67 -> "🌧️"
    in 71..77 -> "❄️"
    in 80..82 -> "🌧️"
    in 85..86 -> "🌨️"
    95 -> "⛈️"
    96, 99 -> "⛈️"
    else -> "☁️"
}
