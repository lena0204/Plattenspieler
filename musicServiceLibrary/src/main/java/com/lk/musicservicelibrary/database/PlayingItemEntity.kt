package com.lk.musicservicelibrary.database

import androidx.annotation.NonNull
import androidx.room.*
import com.lk.musicservicelibrary.models.MusicMetadata

/**
 * Erstellt von Lena am 02/04/2019.
 */
@Entity(tableName = "playlist")
class PlayingItemEntity {

    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "music_id")
    var id: String = ""

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
    @NonNull
    var numTracksLeft: Long = 0

    @ColumnInfo(name = "music_duration")
    @NonNull
    var duration: Long = 0L

    @ColumnInfo(name = "music_cover_uri")
    @NonNull
    var coverUri: String = ""

    @ColumnInfo(name = "music_file_uri")
    @NonNull
    var fileUri: String = ""

    override fun toString(): String {
        return "{id: $id, title: $title, album: $album, artist: $artist, "
    }

    companion object {

        fun createPlayingItemEntity(metadata: MusicMetadata): PlayingItemEntity {
            val item = PlayingItemEntity()
            item.id = metadata.id
            item.title = metadata.title
            item.album = metadata.album
            item.artist = metadata.artist
            item.numTracksLeft = metadata.nr_of_songs_left
            item.duration = metadata.duration
            item.coverUri = metadata.cover_uri
            item.fileUri = metadata.path
            return item
        }

        fun createMusicMetadata(item: PlayingItemEntity): MusicMetadata {
            val metadata = MusicMetadata()
            metadata.id = item.id
            metadata.title = item.title
            metadata.album = item.album
            metadata.artist = item.artist
            metadata.nr_of_songs_left = item.numTracksLeft
            metadata.duration = item.duration
            metadata.cover_uri = item.coverUri
            metadata.path = item.fileUri
            return metadata
        }
    }

}