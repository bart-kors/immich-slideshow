package com.immichframe.app

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "immich_frame_settings")

data class ImmichSettings(
    val serverUrl: String = "",
    val apiKey: String = "",
    val selectedAlbumId: String = "",
    val sleepEnabled: Boolean = false,
    val sleepOffTime: String = "23:00",
    val sleepOnTime: String = "08:00",
    val blurredBackground: Boolean = true,
    val cropLandscape: Boolean = false,
    val weatherCityName: String = "Gouda, Zuid-Holland, Netherlands",
    val weatherLatitude: Double = 52.0115,
    val weatherLongitude: Double = 4.7105,
)

class SettingsRepository(private val context: Context) {

    private val keyServerUrl = stringPreferencesKey("server_url")
    private val keyApiKey = stringPreferencesKey("api_key")
    private val keySelectedAlbumId = stringPreferencesKey("selected_album_id")
    private val keySleepEnabled = booleanPreferencesKey("sleep_enabled")
    private val keySleepOffTime = stringPreferencesKey("sleep_off_time")
    private val keySleepOnTime = stringPreferencesKey("sleep_on_time")
    private val keyBlurredBackground = booleanPreferencesKey("blurred_background")
    private val keyCropLandscape = booleanPreferencesKey("crop_landscape")
    private val keyWeatherCityName = stringPreferencesKey("weather_city_name")
    private val keyWeatherLatitude = doublePreferencesKey("weather_latitude")
    private val keyWeatherLongitude = doublePreferencesKey("weather_longitude")

    val settings: Flow<ImmichSettings> = context.dataStore.data.map { prefs ->
        ImmichSettings(
            serverUrl = prefs[keyServerUrl].orEmpty(),
            apiKey = prefs[keyApiKey].orEmpty(),
            selectedAlbumId = prefs[keySelectedAlbumId].orEmpty(),
            sleepEnabled = prefs[keySleepEnabled] ?: false,
            sleepOffTime = prefs[keySleepOffTime] ?: "23:00",
            sleepOnTime = prefs[keySleepOnTime] ?: "08:00",
            blurredBackground = prefs[keyBlurredBackground] ?: true,
            cropLandscape = prefs[keyCropLandscape] ?: false,
            weatherCityName = prefs[keyWeatherCityName] ?: "Gouda, Zuid-Holland, Netherlands",
            weatherLatitude = prefs[keyWeatherLatitude] ?: 52.0115,
            weatherLongitude = prefs[keyWeatherLongitude] ?: 4.7105,
        )
    }

    suspend fun update(serverUrl: String, apiKey: String) {
        context.dataStore.edit { prefs ->
            prefs[keyServerUrl] = serverUrl
            prefs[keyApiKey] = apiKey
        }
    }

    suspend fun setSelectedAlbum(id: String) {
        context.dataStore.edit { prefs ->
            prefs[keySelectedAlbumId] = id
        }
    }

    suspend fun clearSelectedAlbum() {
        context.dataStore.edit { prefs ->
            prefs.remove(keySelectedAlbumId)
        }
    }

    suspend fun setSleepEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[keySleepEnabled] = enabled
        }
    }

    suspend fun setSleepOffTime(time: String) {
        context.dataStore.edit { prefs ->
            prefs[keySleepOffTime] = time
        }
    }

    suspend fun setSleepOnTime(time: String) {
        context.dataStore.edit { prefs ->
            prefs[keySleepOnTime] = time
        }
    }

    suspend fun setBlurredBackground(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[keyBlurredBackground] = enabled
        }
    }

    suspend fun setCropLandscape(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[keyCropLandscape] = enabled
        }
    }

    suspend fun setWeatherLocation(cityName: String, latitude: Double, longitude: Double) {
        context.dataStore.edit { prefs ->
            prefs[keyWeatherCityName] = cityName
            prefs[keyWeatherLatitude] = latitude
            prefs[keyWeatherLongitude] = longitude
        }
    }
}
