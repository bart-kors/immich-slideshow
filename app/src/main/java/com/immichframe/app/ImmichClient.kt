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
 * OkHttpClients are heavyweight (each owns a dispatcher thread pool and a
 * connection pool), so a single base client is built lazily and every keyed
 * variant is derived from it via newBuilder(), which shares both pools. The
 * app only ever talks to one server with one key at a time, so the derived
 * client and the Retrofit API are single-entry caches keyed on the current
 * credentials — a settings edit swaps them, everything else reuses.
 */
class ImmichClient {

    private val baseClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Volatile
    private var cachedOkHttp: Pair<String, OkHttpClient>? = null

    @Volatile
    private var cachedApi: Triple<String, String, ImmichApi>? = null

    fun okHttp(apiKey: String): OkHttpClient {
        cachedOkHttp?.let { (key, client) -> if (key == apiKey) return client }
        synchronized(this) {
            cachedOkHttp?.let { (key, client) -> if (key == apiKey) return client }
            val client = baseClient.newBuilder()
                .addInterceptor { chain ->
                    val req = chain.request().newBuilder()
                        .header("x-api-key", apiKey)
                        .header("Accept", "application/json")
                        .build()
                    chain.proceed(req)
                }
                .build()
            cachedOkHttp = apiKey to client
            return client
        }
    }

    fun api(baseUrl: String, apiKey: String): ImmichApi {
        val normalized = normalizeBaseUrl(baseUrl)
        cachedApi?.let { (url, key, api) -> if (url == normalized && key == apiKey) return api }
        synchronized(this) {
            cachedApi?.let { (url, key, api) -> if (url == normalized && key == apiKey) return api }
            val api = Retrofit.Builder()
                .baseUrl(normalized)
                .client(okHttp(apiKey))
                .addConverterFactory(jsonForRetrofit.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(ImmichApi::class.java)
            cachedApi = Triple(normalized, apiKey, api)
            return api
        }
    }

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

        fun previewUrl(baseUrl: String, assetId: String): String =
            "${normalizeBaseUrl(baseUrl)}api/assets/$assetId/thumbnail?size=preview"

        fun videoPlaybackUrl(baseUrl: String, assetId: String): String =
            "${normalizeBaseUrl(baseUrl)}api/assets/$assetId/video/playback"
    }
}
