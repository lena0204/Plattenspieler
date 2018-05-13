package com.lk.plattenspieler.models

import android.graphics.Bitmap
import android.media.MediaDescription
import android.media.MediaMetadata
import android.os.Parcelable
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize

/**
 * Erstellt von Lena am 10.05.18.
 * Repräsentiert Metadaten von einem Musiktitel oder Album, kann konvertiert werden
 */
@Parcelize
data class MusicMetadata(
        var id: String,
        var album: String,
        var artist: String,
        var title: String = "",
        var cover_uri: String = "",
        var path: String = "",
        var duration: Long = 0,
        var songnr: Long = 0,
        var num_tracks: Int = 0
) : Parcelable {

    @IgnoredOnParcel
    lateinit var cover: Bitmap
    @IgnoredOnParcel
    lateinit var alltracks: String

    constructor() : this("","","")

    fun isEmpty(): Boolean = id == ""

    fun getDuration(): String{
        var dur = duration
        dur /= 1000
        val min = (dur / 60).toInt()
        val sec = (dur % 60).toInt()
        val s = String.format("%02d", sec)
        return "$min:$s"
    }

    fun getMediaMetadata(): MediaMetadata{
        return MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, id)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, album)
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, cover_uri)
                .putString(MediaMetadata.METADATA_KEY_WRITER, path)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, duration)
                .putLong(MediaMetadata.METADATA_KEY_NUM_TRACKS, songnr)
                .build()
    }

    fun getMediaDescription(): MediaDescription{
        val des = MediaDescription.Builder()
        des.setMediaId(id)
        des.setTitle(album)
        des.setSubtitle(artist)
        des.setDescription(cover_uri + "__" + num_tracks + "__" + title)
        return des.build()
    }

    companion object {
        fun createFromMediaMetadata(meta: MediaMetadata): MusicMetadata{
            if(meta.getString(MediaMetadata.METADATA_KEY_MEDIA_ID) == null){
                return MusicMetadata()
            }
            return MusicMetadata(
                    meta.getString(MediaMetadata.METADATA_KEY_MEDIA_ID),
                    meta.getString(MediaMetadata.METADATA_KEY_ALBUM),
                    meta.getString(MediaMetadata.METADATA_KEY_ARTIST),
                    meta.getString(MediaMetadata.METADATA_KEY_TITLE),
                    meta.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI),
                    meta.getString(MediaMetadata.METADATA_KEY_WRITER),
                    meta.getLong(MediaMetadata.METADATA_KEY_DURATION),
                    meta.getLong(MediaMetadata.METADATA_KEY_NUM_TRACKS))
        }

        fun createFromMediaDescription(item: MediaDescription): MusicMetadata {
            val array = item.description.split("__")
            return MusicMetadata(
                    item.mediaId,
                    item.title.toString(),
                    item.subtitle.toString(),
                    title = array[2],
                    cover_uri = array[0],
                    num_tracks = array[1].toInt()
            )
        }
    }

}