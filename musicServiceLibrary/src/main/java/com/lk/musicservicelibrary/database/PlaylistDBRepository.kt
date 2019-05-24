package com.lk.musicservicelibrary.database

import android.app.Application
import com.lk.musicservicelibrary.models.MusicList
import com.lk.musicservicelibrary.models.MusicMetadata

/**
 * Erstellt von Lena am 03/04/2019.
 */
class PlaylistDBRepository (private val application: Application): PlaylistRepository {

    // TODO Change playlistRepo to Room and also test restoring

    private val TAG = "PlaylistDBAccess"
    private val repository = PlayingItemRepository(application)

    override fun savePlayingQueue(playingQueue: MusicList, playingMetadata: MusicMetadata) {
        repository.deleteAll()
        if(playingQueue.size() > 0) {
            insertMetadata(playingQueue, playingMetadata)
        }
    }

    private fun insertMetadata(playingQueue: MusicList, playingMetadata: MusicMetadata){
        repository.insertPlayingItem(PlayingItemEntity.createPlayingItemEntity(playingMetadata))
        for(item in playingQueue){
            repository.insertPlayingItem(PlayingItemEntity.createPlayingItemEntity(item))
        }
    }

    override fun restoreFirstItem(): MusicMetadata? {
        if(repository.hasItems()){
            return PlayingItemEntity.createMusicMetadata(repository.getFirstPlayingItem())
        }
        return null
    }

    override fun restorePlayingQueue(): MusicList? {
        if(repository.hasItems()){
            val playingItems = repository.getPlayingItems()
            playingItems.removeAt(0)
            val musicList = MusicList()
            for(item in playingItems) {
                musicList.addItem(PlayingItemEntity.createMusicMetadata(item))
            }
            return musicList
        }
        return null
    }

}