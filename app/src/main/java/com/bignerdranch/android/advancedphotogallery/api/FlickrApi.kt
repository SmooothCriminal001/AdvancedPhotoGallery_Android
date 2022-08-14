package com.bignerdranch.android.advancedphotogallery.api

import retrofit2.http.GET
import retrofit2.http.Query

interface FlickrApi {

    @GET("services/rest/?method=flickr.interestingness.getList")
    suspend fun fetchPhotos(@Query(value="page")page: String = "1"): FlickrResponse

    @GET("services/rest?method=flickr.photos.search")
    suspend fun searchPhotos(@Query(value="text") query: String, @Query(value="page")page: String = "1"): FlickrResponse
}