package com.lk.musicservicelibrary.models

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaDescription
import android.media.MediaMetadata
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.util.Log
import android.util.Size
import com.lk.musicservicelibrary.R

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
        var nr_of_songs_left: Long = 0,
        var num_tracks_album: Int = 0,
        var cover: Bitmap? = null,
        var content_uri: Uri = Uri.EMPTY
) : Parcelable {

    @IgnoredOnParcel
    lateinit var allTracksFormatted: String
    @IgnoredOnParcel
    private val TAG = "MusicMetadata"

    constructor() : this("","","")

    fun isEmpty(): Boolean = id == ""

    fun getMediaMetadata(): MediaMetadata{
        val builder = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, id)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, album)
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, cover_uri)
                .putString(MediaMetadata.METADATA_KEY_WRITER, path)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, duration)
                .putLong(MediaMetadata.METADATA_KEY_NUM_TRACKS, nr_of_songs_left)
                .putString(MediaMetadata.METADATA_KEY_MEDIA_URI, content_uri.toString())
        if(cover != null){
            builder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, cover)
        } else {
            Log.v("MusicMetadata", "Cover for $title is not available at URI: $cover_uri.")
        }
        return builder.build()
    }

    fun getMediaDescription(): MediaDescription{
        val des = MediaDescription.Builder()
        des.setMediaId(id)
        des.setTitle(album)
        des.setSubtitle(artist)
        des.setDescription(cover_uri + "__" + num_tracks_album + "__" + title + "__" + content_uri.toString())
        return des.build()
    }

    companion object {
        fun createFromMediaMetadata(meta: MediaMetadata): MusicMetadata{
            if(meta.getString(MediaMetadata.METADATA_KEY_MEDIA_ID) == null){
                return MusicMetadata()
            }
            // TODO fix min API-Level for Media_URI
            return MusicMetadata(
                    id = meta.getString(MediaMetadata.METADATA_KEY_MEDIA_ID),
                    album = meta.getString(MediaMetadata.METADATA_KEY_ALBUM),
                    artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST),
                    title = meta.getString(MediaMetadata.METADATA_KEY_TITLE),
                    cover_uri = meta.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI),
                    path = meta.getString(MediaMetadata.METADATA_KEY_WRITER),
                    duration = meta.getLong(MediaMetadata.METADATA_KEY_DURATION),
                    nr_of_songs_left = meta.getLong(MediaMetadata.METADATA_KEY_NUM_TRACKS),
                    cover = meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART),
                    content_uri = Uri.parse(meta.getString(MediaMetadata.METADATA_KEY_MEDIA_URI)))
        }

        fun createFromMediaDescription(item: MediaDescription): MusicMetadata {
            if(item.description != null) {
                val array = item.description!!.split("__")
                return MusicMetadata(
                        item.mediaId!!,
                        item.title!!.toString(),
                        item.subtitle!!.toString(),
                        title = array[2],
                        cover_uri = array[0],
                        num_tracks_album = array[1].toInt(),
                        content_uri = Uri.parse(array[3])
                )
            }
            return MusicMetadata()
        }

        fun formatMilliseconds(millis: Long) : String {
            val seconds = millis / 1000
            val min = (seconds / 60).toInt()
            val sec = (seconds % 60).toInt()
            val s = String.format("%02d", sec)
            return "$min:$s"
        }
    }

}