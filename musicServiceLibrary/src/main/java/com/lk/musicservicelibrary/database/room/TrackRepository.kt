package com.lk.musicservicelibrary.database.room

import android.app.Application
import android.util.Log
import com.lk.musicservicelibrary.database.PlaylistRepository
import kotlinx.coroutines.*

/**
 * Erstellt von Lena am 03/04/2019.
 */
class TrackRepository(application: Application) {

    private val TAG = "PlayingItemRepository"

    private val db = PlaylistDatabase.getInstance(application)
    private val dao = db.playlistDao()
    private var currentPlaylist: List<TrackEntity> = listOf()

    init {
        // observe is necessary to let LiveData emit data
        dao.selectAll().observeForever {
            currentPlaylist = it
            Log.d(TAG, "Playlist was updated to $it")
        }
    }

    fun insertPlaylist(list: List<TrackEntity>) {
        GlobalScope.launch {
            dao.deleteAll()
            for(item in list) {
                dao.insert(item)
                Log.d(TAG, "inserted item")
            }
            Log.d(TAG, "Inserted all (${list.size}) with playlist: $currentPlaylist")
        }
    }

    fun deleteAll(){
        GlobalScope.launch(Dispatchers.Default) {
            dao.deleteAll()
            Log.d(TAG, "Deleted all")
        }
    }

    fun hasItems(): Boolean = currentPlaylist.isNotEmpty()

    fun getPlaylist(): List<TrackEntity> {
        Log.d(TAG, "Rowcount in DB: ${currentPlaylist.size}")
        return currentPlaylist
    }

}