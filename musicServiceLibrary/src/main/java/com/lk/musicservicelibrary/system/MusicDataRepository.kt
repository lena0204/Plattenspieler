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

    fun getFirstTitleIDForShuffle(): String

    fun getAllTitles(playingTitleId: String): MusicList

    fun getTitlesByAlbumID(albumId: String): MusicList

    fun getAllAlbums(): MusicList

    fun getTitleByID(titleId: String): MusicMetadata

}