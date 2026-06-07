package com.immichframe.app

import android.content.Context
import com.immichframe.app.db.AlbumEntity
import com.immichframe.app.db.ImmichDatabase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow

class ImmichRepository(context: Context) {

    private val dao = ImmichDatabase.get(context).immichDao()

    fun observeAlbums(): Flow<List<AlbumEntity>> = dao.observeAlbums()

    suspend fun refreshAlbums(serverUrl: String, apiKey: String) {
        val remote = ImmichClient.api(serverUrl, apiKey).getAlbums()
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
                        else -> ImmichClient.fetchThumbnailBytes(serverUrl, apiKey, newThumbId)
                    }
                    AlbumEntity(
                        id = dto.id,
                        albumName = dto.albumName,
                        description = dto.description,
                        assetCount = dto.assetCount,
                        thumbnailAssetId = newThumbId,
                        thumbnail = bytes,
                        sortDate = dto.endDate ?: dto.createdAt,
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
}
