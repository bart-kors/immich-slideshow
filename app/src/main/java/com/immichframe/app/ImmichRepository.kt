package com.immichframe.app

import android.content.Context
import com.immichframe.app.db.AlbumEntity
import com.immichframe.app.db.ImmichDatabase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow

class ImmichRepository(
    context: Context,
    private val client: ImmichClient = ImmichClient(),
) {

    private val dao = ImmichDatabase.get(context).immichDao()

    fun observeAlbums(): Flow<List<AlbumEntity>> = dao.observeAlbums()

    suspend fun refreshAlbums(serverUrl: String, apiKey: String) {
        val api = client.api(serverUrl, apiKey)
        val remote = api.getAlbums()
        val existing = dao.getAlbumsOnce().associateBy { it.id }
        val now = System.currentTimeMillis()

        val entities = coroutineScope {
            remote.map { dto ->
                async {
                    val prev = existing[dto.id]
                    val newThumbId = dto.albumThumbnailAssetId
                    val bytes = when {
                        newThumbId == null -> null
                        prev != null && prev.thumbnailAssetId == newThumbId && prev.thumbnail != null ->
                            prev.thumbnail
                        else -> client.fetchThumbnailBytes(serverUrl, apiKey, newThumbId)
                    }
                    // Sort strictly by the album's newest photo date. The album
                    // list endpoint sometimes omits endDate, so fall back to the
                    // single-album endpoint — never to the album's creation date.
                    val newestPhotoDate = dto.endDate
                        ?: runCatching { api.getAlbum(dto.id).endDate }.getOrNull()
                    AlbumEntity(
                        id = dto.id,
                        albumName = dto.albumName,
                        description = dto.description,
                        assetCount = dto.assetCount,
                        thumbnailAssetId = newThumbId,
                        thumbnail = bytes,
                        sortDate = newestPhotoDate,
                        updatedAt = now,
                    )
                }
            }.awaitAll()
        }

        dao.upsertAlbums(entities)
        dao.deleteAlbumsNotIn(remote.map { it.id })
    }

    suspend fun clearAll() {
        dao.deleteAllAlbums()
    }

    /**
     * Fetches a single album's assets from the server, filters out OTHER types,
     * returns them in a stable shuffled order.
     */
    suspend fun loadAlbumAssets(
        serverUrl: String,
        apiKey: String,
        albumId: String,
    ): AlbumAssets {
        val api = client.api(serverUrl, apiKey)
        val album = api.getAlbum(albumId)

        // Immich 3.0 dropped the embedded assets array from the album-info
        // response, so page through the search endpoint to collect them.
        val assets = mutableListOf<AssetDto>()
        var page = 1
        while (true) {
            val resp = api.searchAlbumAssets(MetadataSearchDto(albumIds = listOf(albumId), page = page))
            assets += resp.assets.items
            page = resp.assets.nextPage?.toIntOrNull() ?: break
        }

        val supported = assets.filter { it.assetType() != AssetType.OTHER }
        return AlbumAssets(
            albumName = album.albumName,
            assets = supported.shuffled(),
        )
    }
}

data class AlbumAssets(
    val albumName: String,
    val assets: List<AssetDto>,
)
