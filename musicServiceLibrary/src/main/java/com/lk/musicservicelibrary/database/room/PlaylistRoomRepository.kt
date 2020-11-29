package com.lk.musicservicelibrary.database.room

import android.app.Application
import android.util.Log
import com.lk.musicservicelibrary.database.PlaylistRepository
import com.lk.musicservicelibrary.models.MusicList
import com.lk.musicservicelibrary.models.MusicMetadata

/**
 * Erstellt von Lena am 2019-12-18.
 */
class PlaylistRoomRepository(val application: Application): PlaylistRepository {

    private val TAG = "PlaylistRoomRepository"
    private val repository = TrackRepository(application)

    override fun savePlayingQueue(playingQueue: MusicList, playingMetadata: MusicMetadata) {
        val playlist: MutableList<TrackEntity> = mutableListOf()
        playlist.add(TrackEntity.createTrackEntity(playingMetadata))
        playingQueue.forEach {
            Log.v(TAG, "Saving title: ${it.title}")
            playlist.add(TrackEntity.createTrackEntity(it))
        }
        Log.d(TAG, "Converted Playlist (Size ${playlist.size})")
        repository.insertPlaylist(playlist)
    }

    override fun restorePlayingQueue(): MusicList {
        val trackList = repository.getPlaylist()
        val playlist = MusicList()
        trackList.forEach {
            playlist.addItem(TrackEntity.createMusicMetadata(it))
        }
        return playlist
    }

    override fun hasPlaylist(): Boolean = repository.hasItems()

    override fun deletePlaylist() {
        repository.deleteAll()
    }


}