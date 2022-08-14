package com.bignerdranch.android.advancedphotogallery.api

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState

private const val TAG = "FlickrPagingDataSource"

class FlickrPagingDataSource(val repository: PhotoRepository, val query: String) : PagingSource<Int, GalleryItem>() {
    override fun getRefreshKey(state: PagingState<Int, GalleryItem>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GalleryItem> {

        return try {
            val currentPage = params.key ?: 1
            val galleryitemList = if(query.isNotBlank()){
                Log.d(TAG, "searching photos for $query")
                repository.searchPhotos(query, currentPage.toString())
            }else{
                Log.d(TAG, "Fetching all photos")
                repository.fetchPhotos(currentPage.toString())
            }

            LoadResult.Page(
                data = galleryitemList,
                prevKey = if (currentPage > 1) (currentPage - 1) else null,
                nextKey = currentPage + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}