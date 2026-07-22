package com.immichframe.app

import android.content.Context
import coil.ImageLoader

/**
 * Hands out one process-wide Coil [ImageLoader] for the current API key.
 *
 * Every ImageLoader owns a bitmap memory cache sized as a fraction of the app
 * heap, so building one per screen entry piles up caches faster than GC
 * reclaims them on the frame's 512 MB. Single-entry cache: a settings edit
 * that changes the key builds a fresh loader and shuts the old one down.
 */
class ImageLoaderProvider(context: Context, private val client: ImmichClient) {

    private val appContext = context.applicationContext

    private var cached: Pair<String, ImageLoader>? = null

    @Synchronized
    fun get(apiKey: String): ImageLoader {
        cached?.let { (key, loader) -> if (key == apiKey) return loader }
        val loader = ImageLoader.Builder(appContext)
            .okHttpClient(client.okHttp(apiKey))
            .crossfade(true)
            .build()
        cached?.second?.shutdown()
        cached = apiKey to loader
        return loader
    }
}
