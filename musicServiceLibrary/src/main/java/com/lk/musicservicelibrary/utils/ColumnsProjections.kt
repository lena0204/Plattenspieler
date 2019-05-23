package com.lk.musicservicelibrary.utils

import android.provider.MediaStore.Audio as MSAudio

/**
 * Erstellt von Lena am 07.09.18.
 */
object AudioColumns {
    
    val albumArtColumns = arrayOf(
            MSAudio.Albums._ID,
            MSAudio.Albums.ALBUM_ART)

    val albumsColumns = arrayOf(
            MSAudio.Albums._ID,
            MSAudio.Albums.ALBUM,
            MSAudio.Albums.ALBUM_ART,
            MSAudio.Albums.ARTIST,
            MSAudio.Albums.NUMBER_OF_SONGS)

    val titlesColumns = arrayOf(
            MSAudio.Media._ID,
            MSAudio.Media.TITLE,
            MSAudio.Media.ARTIST,
            MSAudio.Media.ALBUM
    )
    
    val metadataColumns = arrayOf(
            MSAudio.Media._ID,
            MSAudio.Media.TITLE,
            MSAudio.Media.ALBUM,
            MSAudio.Media.ARTIST,
            MSAudio.Media.DURATION,
            MSAudio.Media.ALBUM_ID,
            MSAudio.Media.DATA)
}