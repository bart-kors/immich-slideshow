package com.immichframe.app

import kotlinx.serialization.Serializable

@Serializable
data class AlbumDto(
    val id: String,
    val albumName: String,
    val description: String? = null,
    val assetCount: Int = 0,
    val albumThumbnailAssetId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val endDate: String? = null,
    val startDate: String? = null,
    val assets: List<AssetDto> = emptyList(),
)

/**
 * Request body for `POST /api/search/metadata`. As of Immich 3.0 the album-info
 * response no longer embeds its assets, so an album's assets are fetched here by
 * filtering on [albumIds]. Results are paginated ([size] max 1000).
 */
@Serializable
data class MetadataSearchDto(
    val albumIds: List<String>,
    val size: Int = 1000,
    val page: Int = 1,
    val withExif: Boolean = true,
)

@Serializable
data class SearchResponseDto(
    val assets: SearchAssetsDto,
)

@Serializable
data class SearchAssetsDto(
    val items: List<AssetDto> = emptyList(),
    val nextPage: String? = null,
)

@Serializable
data class AssetDto(
    val id: String,
    val type: String,
    val originalFileName: String? = null,
    val fileCreatedAt: String? = null,
    val localDateTime: String? = null,
    val exifInfo: ExifInfoDto? = null,
)

@Serializable
data class ExifInfoDto(
    val dateTimeOriginal: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
)

enum class AssetType { IMAGE, VIDEO, OTHER }

fun AssetDto.assetType(): AssetType = when (type.uppercase()) {
    "IMAGE" -> AssetType.IMAGE
    "VIDEO" -> AssetType.VIDEO
    else -> AssetType.OTHER
}
