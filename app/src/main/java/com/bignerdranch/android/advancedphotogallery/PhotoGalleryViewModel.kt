package com.bignerdranch.android.advancedphotogallery

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.bignerdranch.android.advancedphotogallery.api.FlickrPagingDataSource
import com.bignerdranch.android.advancedphotogallery.api.GalleryItem
import com.bignerdranch.android.advancedphotogallery.api.PhotoRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val TAG = "PhotoGalleryViewModel"
class PhotoGalleryViewModel : ViewModel() {

    private val photoRepository = PhotoRepository()
    private val preferencesRepository = PreferencesRepository.get()
    private val _uiState: MutableStateFlow<PhotoUiState> = MutableStateFlow(PhotoUiState())
    val uiState: StateFlow<PhotoUiState>
    	get() = _uiState.asStateFlow()


    data class PhotoUiState(
    	//val query: String = "",
        val history: List<String> = emptyList()
    )

    var galleryItems = preferencesRepository.historyFlow.flatMapLatest { history ->
        val query = if(history.isNotEmpty()) history.first() else ""
        Log.d(TAG, "querying ${history.first()}")

        Pager(
            config = PagingConfig(pageSize = 100),
            pagingSourceFactory = {
                FlickrPagingDataSource(photoRepository, query)
            }
        ).flow.cachedIn(viewModelScope)
    }

    init {
        /*
        viewModelScope.launch{
            preferencesRepository.queryFlow.collectLatest { storedQuery ->
                Log.d(TAG, "Collecting $storedQuery")
                _uiState.update { oldState ->
                    oldState.copy(
                        query = storedQuery
                    )
                }
            }
        }
         */

        viewModelScope.launch{
            preferencesRepository.historyFlow.collectLatest { searchHistory ->
                Log.d(TAG, "History updated $searchHistory")
                _uiState.update{ oldState ->
                    oldState.copy(
                        history = searchHistory
                    )
                }
            }
        }
    }

    fun searchItems(query: String){
        viewModelScope.launch{
            Log.d(TAG, "Search set $query")
            //preferencesRepository.updateQuery(query)
            preferencesRepository.updateHistory(query);
        }
    }

    /*
    fun onQueryChange(query: String){
        Log.d(TAG, "onQueryChange called")
        viewModelScope.launch{
            try{
                Log.d(TAG, "Entered Viewmodel scope")
                mutableQuery.value = query

                galleryItems =

                galleryItems = Pager(
                    config = PagingConfig(pageSize = 100),
                    pagingSourceFactory = {
                        FlickrPagingDataSource(photoRepository, query)
                    }
                ).flow.cachedIn(viewModelScope)


            }
            catch(ex: Exception){
                Log.e(TAG, "Failed to fetch gallery items", ex)
            }
        }
    }
     */

    override fun onCleared() {
        super.onCleared()
    }
}