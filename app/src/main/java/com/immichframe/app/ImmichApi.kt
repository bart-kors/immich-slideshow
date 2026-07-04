package com.immichframe.app

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ImmichApi {
    @GET("api/albums")
    suspend fun getAlbums(): List<AlbumDto>

    @GET("api/albums/{id}")
    suspend fun getAlbum(@Path("id") id: String): AlbumDto

    // Immich 3.0+: album assets are no longer embedded in the album-info
    // response and must be retrieved through the search endpoint.
    @POST("api/search/metadata")
    suspend fun searchAlbumAssets(@Body body: MetadataSearchDto): SearchResponseDto
}
