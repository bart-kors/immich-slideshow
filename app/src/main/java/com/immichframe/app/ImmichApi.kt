package com.immichframe.app

import retrofit2.http.GET
import retrofit2.http.Path

interface ImmichApi {
    @GET("api/albums")
    suspend fun getAlbums(): List<AlbumDto>

    @GET("api/albums/{id}")
    suspend fun getAlbum(@Path("id") id: String): AlbumDto
}
