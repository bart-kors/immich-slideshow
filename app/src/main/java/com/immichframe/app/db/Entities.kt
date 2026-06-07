package com.immichframe.app.db

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "albums")
class AlbumEntity(
    @PrimaryKey val id: String,
    val albumName: String,
    val description: String?,
    val assetCount: Int,
    val thumbnailAssetId: String?,
    val thumbnail: ByteArray?,
    val sortDate: String?,
    val updatedAt: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AlbumEntity) return false
        return id == other.id &&
            albumName == other.albumName &&
            description == other.description &&
            assetCount == other.assetCount &&
            thumbnailAssetId == other.thumbnailAssetId &&
            sortDate == other.sortDate &&
            updatedAt == other.updatedAt &&
            (thumbnail?.contentEquals(other.thumbnail) ?: (other.thumbnail == null))
    }

    override fun hashCode(): Int {
        var r = id.hashCode()
        r = 31 * r + albumName.hashCode()
        r = 31 * r + (description?.hashCode() ?: 0)
        r = 31 * r + assetCount
        r = 31 * r + (thumbnailAssetId?.hashCode() ?: 0)
        r = 31 * r + (thumbnail?.contentHashCode() ?: 0)
        r = 31 * r + (sortDate?.hashCode() ?: 0)
        r = 31 * r + updatedAt.hashCode()
        return r
    }
}
