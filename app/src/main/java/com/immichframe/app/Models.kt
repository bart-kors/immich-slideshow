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
