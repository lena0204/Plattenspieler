package com.lk.musicservicelibrary.database

import com.lk.musicservicelibrary.models.MusicList
import com.lk.musicservicelibrary.models.MusicMetadata

/**
 * Erstellt von Lena am 01/04/2019.
 */
interface PlaylistRepository {

    fun savePlayingQueue(playingQueue: MusicList, playingMetadata: MusicMetadata)
    fun restorePlayingQueue(): MusicList
    fun hasPlaylist(): Boolean
    fun deletePlaylist()

}