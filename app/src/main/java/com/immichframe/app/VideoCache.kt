package com.immichframe.app

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Process-wide singleton disk cache for slideshow videos.
 *
 * Media3 permits only one [SimpleCache] per directory per process, so every
 * playback and prefetch path must share this instance. The cache lets the
 * slideshow warm the next video ahead of time and replays album loops straight
 * from flash instead of re-streaming — cutting the startup buffering spinner on
 * the frame's limited Wi-Fi. Bounded LRU so it never fills the device storage.
 */
@UnstableApi
object VideoCache {
    private const val MAX_BYTES = 512L * 1024 * 1024 // 512 MB

    @Volatile
    private var instance: SimpleCache? = null

    fun get(context: Context): SimpleCache {
        val app = context.applicationContext
        return instance ?: synchronized(this) {
            instance ?: SimpleCache(
                File(app.cacheDir, "video_cache"),
                LeastRecentlyUsedCacheEvictor(MAX_BYTES),
                StandaloneDatabaseProvider(app),
            ).also { instance = it }
        }
    }
}
