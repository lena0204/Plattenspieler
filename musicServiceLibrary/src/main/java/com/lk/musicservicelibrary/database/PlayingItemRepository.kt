package com.lk.musicservicelibrary.database

import android.app.Application
import android.util.Log
import kotlinx.coroutines.*

/**
 * Erstellt von Lena am 03/04/2019.
 */
class PlayingItemRepository(application: Application) {
    private val TAG = "PlayingItemRepository"

    private val db = PlaylistDatabase.getInstance(application)
    private val dao = db.playlistDao()
    private val hasItems = dao.hasItems()

    fun insertPlayingItem(playingItem: PlayingItemEntity){
        GlobalScope.launch(Dispatchers.Default){
            dao.insertPlayingItem(playingItem)
            Log.d(TAG, "insert value $playingItem")
        }
    }

    fun deleteAll(){
        GlobalScope.launch(Dispatchers.Default) {
            dao.deleteAll()
        }
    }

    fun hasItems() = hasItems.value!! > 0

    fun getFirstPlayingItem(): PlayingItemEntity {
        // TODO sinnvoller Zugriff auf die zeitversetzte Abfrage
        return dao.getFirstPlayingItem()
    }

    fun getPlayingItems(): MutableList<PlayingItemEntity> {
        // TODO sinnvoller Zugriff auf die zeitversetzte Abfrage
        return dao.getPlayingItems()
    }

}