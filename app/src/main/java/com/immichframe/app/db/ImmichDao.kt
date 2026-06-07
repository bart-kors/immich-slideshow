package com.immichframe.app.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ImmichDao {

    @Query("SELECT * FROM albums ORDER BY sortDate IS NULL, sortDate DESC, albumName")
    fun observeAlbums(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums")
    suspend fun getAlbumsOnce(): List<AlbumEntity>

    @Upsert
    suspend fun upsertAlbums(albums: List<AlbumEntity>)

    @Query("DELETE FROM albums WHERE id NOT IN (:ids)")
    suspend fun deleteAlbumsNotIn(ids: List<String>)

    @Query("DELETE FROM albums")
    suspend fun deleteAllAlbums()
}
