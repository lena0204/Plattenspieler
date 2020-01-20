package com.lk.musicservicelibrary.database.room

import android.net.Uri
import androidx.annotation.NonNull
import androidx.room.*
import com.lk.musicservicelibrary.models.MusicMetadata

/**
 * Erstellt von Lena am 02/04/2019.
 */
@Entity(tableName = "playlist")
class TrackEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "music_id")
    var id: Int = 0

    @ColumnInfo(name = "music_media_id")
    var mediaId: String  = ""

    @ColumnInfo(name = "music_title")
    @NonNull
    var title: String = ""

    @ColumnInfo(name = "music_album")
    @NonNull
    var album: String = ""

    @ColumnInfo(name = "music_artist")
    @NonNull
    var artist: String = ""

    @ColumnInfo(name = "music_num_tracks_left")
    var numTracksLeft: Long = 0L

    @ColumnInfo(name = "music_duration")
    var duration: Long = 0L

    @ColumnInfo(name = "music_cover_uri")
    var coverUri: String = ""

    @ColumnInfo(name = "music_file_uri")
    var fileUri: String = ""

    @ColumnInfo(name = "music_display_name")
    var displayName: String = ""

    @ColumnInfo(name = "music_content_uri")
    var contentUri: String = ""

    override fun toString(): String {
        return "{id: $id, title: $title, album: $album, artist: $artist}"
    }

    companion object {

        fun createTrackEntity(metadata: MusicMetadata): TrackEntity {
            val item = TrackEntity()
            item.mediaId = metadata.id
            item.title = metadata.title
            item.album = metadata.album
            item.artist = metadata.artist
            item.numTracksLeft = metadata.nr_of_songs_left
            item.duration = metadata.duration
            item.coverUri = metadata.cover_uri
            item.fileUri = metadata.path
            item.contentUri = metadata.content_uri.toString()
            item.displayName = metadata.display_name
            return item
        }

        fun createMusicMetadata(item: TrackEntity): MusicMetadata {
            val metadata = MusicMetadata()
            metadata.id = item.mediaId
            metadata.title = item.title
            metadata.album = item.album
            metadata.artist = item.artist
            metadata.nr_of_songs_left = item.numTracksLeft
            metadata.duration = item.duration
            metadata.cover_uri = item.coverUri
            metadata.path = item.fileUri
            metadata.content_uri = Uri.parse(item.contentUri)
            metadata.display_name = item.displayName
            return metadata
        }
    }

}