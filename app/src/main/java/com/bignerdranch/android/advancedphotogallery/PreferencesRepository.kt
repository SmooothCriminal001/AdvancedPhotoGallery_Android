package com.bignerdranch.android.advancedphotogallery

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.beust.klaxon.Klaxon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private const val TAG = "PreferencesRepository"
private const val MAX_HISTORY = 30

class PreferencesRepository private constructor(
    val dataStore: DataStore<Preferences>,
    private val coroutineScope: CoroutineScope = GlobalScope
) {

    /*val queryFlow: Flow<String> = dataStore.data.map {
    	it[SEARCH_QUERY_KEY] ?: ""
    }.distinctUntilChanged() */

    val historyFlow: Flow<List<String>> = dataStore.data.map {
        //it[SEARCH_HISTORY_KEY]?.split(',') ?: emptyList()
        it[SEARCH_HISTORY_KEY]?.let {
            Klaxon().parseArray<History>(it)?.map {
                it.query
            }?.reversed()
        } ?: emptyList()
    }.distinctUntilChanged()

    /*
    suspend fun updateQuery(updatedQuery: String) {
    	dataStore.edit {
            //Log.d(TAG, "Current value ${it[SEARCH_QUERY_KEY]}")
    		it[SEARCH_QUERY_KEY] = updatedQuery
    	}
    }

     */

    suspend fun updateHistory(newQuery: String){
        dataStore.edit{ preference ->
            val existingList = preference[SEARCH_HISTORY_KEY]?.let{
                Klaxon().parseArray<History>(it)?.toMutableList()
            } ?: mutableListOf()

            if(existingList.any { it.query == newQuery }){
                existingList.forEach {
                    if(it.query == newQuery){
                        it.timestamp = (System.currentTimeMillis()) / 1000L
                        return@forEach
                    }
                }
            }else{
                existingList.add(History(newQuery, (System.currentTimeMillis())/ 1000L))
            }

            existingList.sortBy { it.timestamp }

            while(existingList.size > MAX_HISTORY){
                existingList.removeFirst()
            }

            preference[SEARCH_HISTORY_KEY] = Klaxon().toJsonString(existingList)
        }
    }

    companion object {
        private var INSTANCE: PreferencesRepository? = null
        //private val SEARCH_QUERY_KEY = stringPreferencesKey("SEARCH_QUERY")
        private val SEARCH_HISTORY_KEY = stringPreferencesKey("SEARCH_HISTORY")

        fun initialize(context: Context) {

            val dataStore = PreferenceDataStoreFactory.create {
                context.preferencesDataStoreFile("flickr_settings")
            }

            if (INSTANCE == null) {
                INSTANCE = PreferencesRepository(dataStore)
            }
        }

        fun get(): PreferencesRepository {
            return INSTANCE
                ?: throw IllegalStateException("PreferencesRepository must be initialized")
        }
    }

    data class History(
        val query: String,
        var timestamp: Long
    )
}