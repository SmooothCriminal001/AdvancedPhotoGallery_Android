package com.bignerdranch.android.advancedphotogallery.api

import android.util.Log
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create

private const val TAG = "PhotoRepository"
class PhotoRepository {
    private val flickrApi: FlickrApi
    
    init {

		val okHttpClient = OkHttpClient.Builder()
		            .addInterceptor(FlickrInterceptor())
		            .build()

    	val retrofit: Retrofit = Retrofit.Builder()
			.baseUrl("https://api.flickr.com")
			.addConverterFactory(MoshiConverterFactory.create())		//Uses Moshi converter on the response object to parse response
			.client(okHttpClient)
			.build()
    	flickrApi = retrofit.create()
    }
    
    suspend fun fetchPhotos(page: String) = flickrApi.fetchPhotos(page).photos.galleryItems

	suspend fun searchPhotos(query: String, page: String) = flickrApi.searchPhotos(query, page).photos.galleryItems
}