package com.lk.musicservicelibrary.system

import android.provider.MediaStore.Audio as MSAudio
import com.lk.musicservicelibrary.models.*

/**
 * Erstellt von Lena am 02.09.18.
 */
interface MusicDataRepository {

    companion object {
        const val ROOT_ID = "__ ROOT__"
    }

    fun queryFirstTitle(): MusicMetadata

    fun queryTitles(playingTitleId: String): MusicList

    fun queryTitlesByAlbumID(albumId: String): MusicList

    fun queryAlbums(): MusicList
}