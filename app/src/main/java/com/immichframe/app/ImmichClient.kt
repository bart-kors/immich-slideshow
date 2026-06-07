package com.immichframe.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object ImmichClient {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    fun normalizeBaseUrl(raw: String): String {
        val trimmed = raw.trim().trimEnd('/')
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "http://$trimmed"
        }
        return "$withScheme/"
    }

    fun okHttp(apiKey: String): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("x-api-key", apiKey)
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(req)
            }
            .build()

    fun retrofit(baseUrl: String, apiKey: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(normalizeBaseUrl(baseUrl))
            .client(okHttp(apiKey))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    fun api(baseUrl: String, apiKey: String): ImmichApi =
        retrofit(baseUrl, apiKey).create(ImmichApi::class.java)

    fun thumbnailUrl(baseUrl: String, assetId: String): String =
        "${normalizeBaseUrl(baseUrl)}api/assets/$assetId/thumbnail?size=thumbnail"

    private const val MAX_THUMBNAIL_BYTES = 300_000

    suspend fun fetchThumbnailBytes(
        baseUrl: String,
        apiKey: String,
        assetId: String,
    ): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val client = okHttp(apiKey)
            val req = Request.Builder().url(thumbnailUrl(baseUrl, assetId)).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val bytes = resp.body?.bytes() ?: return@use null
                if (bytes.size > MAX_THUMBNAIL_BYTES) null else bytes
            }
        }.getOrNull()
    }

    fun previewUrl(baseUrl: String, assetId: String): String =
        "${normalizeBaseUrl(baseUrl)}api/assets/$assetId/thumbnail?size=preview"

    fun videoPlaybackUrl(baseUrl: String, assetId: String): String =
        "${normalizeBaseUrl(baseUrl)}api/assets/$assetId/video/playback"
}
