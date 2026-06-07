package com.immichframe.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object WeatherApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetch(latitude: Double, longitude: Double): WeatherSnapshot? =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$latitude&longitude=$longitude" +
                    "&current=temperature_2m,weather_code" +
                    "&timezone=auto"
                val req = Request.Builder().url(url).build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    val body = resp.body?.string() ?: return@use null
                    val parsed = json.decodeFromString<OpenMeteoResponse>(body)
                    parsed.current?.let {
                        WeatherSnapshot(tempCelsius = it.temperature_2m, weatherCode = it.weather_code)
                    }
                }
            }.getOrNull()
        }

    suspend fun searchCities(query: String, languageCode: String = "en"): List<CityResult> =
        withContext(Dispatchers.IO) {
            if (query.length < 2) return@withContext emptyList()
            runCatching {
                val encoded = java.net.URLEncoder.encode(query.trim(), "UTF-8")
                val url = "https://geocoding-api.open-meteo.com/v1/search" +
                    "?name=$encoded&count=10&language=$languageCode&format=json"
                val req = Request.Builder().url(url).build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use emptyList<CityResult>()
                    val body = resp.body?.string() ?: return@use emptyList<CityResult>()
                    json.decodeFromString<GeocodingResponse>(body).results
                        ?.map {
                            CityResult(
                                id = it.id,
                                name = it.name,
                                country = it.country.orEmpty(),
                                admin1 = it.admin1.orEmpty(),
                                latitude = it.latitude,
                                longitude = it.longitude,
                            )
                        } ?: emptyList()
                }
            }.getOrDefault(emptyList())
        }
}

data class CityResult(
    val id: Long,
    val name: String,
    val country: String,
    val admin1: String,
    val latitude: Double,
    val longitude: Double,
) {
    val displayName: String
        get() = listOf(name, admin1, country).filter { it.isNotBlank() }.joinToString(", ")
}

@Serializable
private data class GeocodingResponse(
    val results: List<GeocodingResult>? = null,
)

@Serializable
private data class GeocodingResult(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    val admin1: String? = null,
)

data class WeatherSnapshot(
    val tempCelsius: Double,
    val weatherCode: Int,
)

@Serializable
private data class OpenMeteoResponse(
    val current: OpenMeteoCurrent? = null,
)

@Suppress("PropertyName")
@Serializable
private data class OpenMeteoCurrent(
    val temperature_2m: Double = 0.0,
    val weather_code: Int = 0,
)
