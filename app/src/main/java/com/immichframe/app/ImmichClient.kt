package com.immichframe.app

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Thin wrapper over OkHttp + Retrofit for talking to an Immich server.
 *
 * The methods are stateless — the class only exists so a test can pass a fake
 * via [LocalImmichClient].
 */
class ImmichClient {

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
            .addConverterFactory(jsonForRetrofit.asConverterFactory("application/json".toMediaType()))
            .build()

    fun api(baseUrl: String, apiKey: String): ImmichApi =
        retrofit(baseUrl, apiKey).create(ImmichApi::class.java)

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

    companion object {
        private const val MAX_THUMBNAIL_BYTES = 300_000

        private val jsonForRetrofit = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

        // Pure URL builders kept as companion functions so callers don't need an instance
        // (they don't depend on per-request state).

        fun normalizeBaseUrl(raw: String): String {
            val trimmed = raw.trim().trimEnd('/')
            val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                trimmed
            } else {
                "http://$trimmed"
            }
            return "$withScheme/"
        }

        fun thumbnailUrl(baseUrl: String, assetId: String): String =
            "${normalizeBaseUrl(baseUrl)}api/assets/$assetId/thumbnail?size=thumbnail"

        fun originalUrl(baseUrl: String, assetId: String): String =
            "${normalizeBaseUrl(baseUrl)}api/assets/$assetId/original"

        fun previewUrl(baseUrl: String, assetId: String): String =
            "${normalizeBaseUrl(baseUrl)}api/assets/$assetId/thumbnail?size=preview"

        fun videoPlaybackUrl(baseUrl: String, assetId: String): String =
            "${normalizeBaseUrl(baseUrl)}api/assets/$assetId/video/playback"
    }
}
