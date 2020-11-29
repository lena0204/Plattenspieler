package com.lk.musicservicelibrary.system

import android.content.*
import android.database.Cursor
import android.media.browse.MediaBrowser
import android.provider.MediaStore
import android.util.Log
import com.lk.musicservicelibrary.models.MusicList
import com.lk.musicservicelibrary.models.MusicMetadata
import com.lk.musicservicelibrary.utils.AudioColumns
import java.lang.Exception

/**
 * Erstellt von Lena am 01/04/2019.
 * Columns zu den Projections hinzufügen!!!
 */
class LocalMusicFileRepository(private val context: Context): MusicDataRepository {

    private val TAG = "LocalMusicFileRepo"
    private val mediaStoreURI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    private val albumStoreURI = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI

    private var currentMusicList = MusicList()

    override fun queryAlbums(): MusicList {
        val sortOrder = MediaStore.Audio.Albums.ALBUM + " ASC"
        val albumCursor = queryAlbumTable(AudioColumns.albumsColumns, sortOrder = sortOrder)
        currentMusicList = MusicList()
        currentMusicList.setMediaType(MediaBrowser.MediaItem.FLAG_BROWSABLE)
        if(albumCursor != null && albumCursor.moveToFirst()) {
            addAlbumsToList(albumCursor)
        }
        albumCursor?.close()
        return currentMusicList
    }

    private fun addAlbumsToList(albumCursor: Cursor){
        val indexID = albumCursor.getColumnIndex(MediaStore.Audio.Albums._ID)
        val indexAlbum = albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM)
        val indexNumberOfSongs = albumCursor.getColumnIndex(MediaStore.Audio.Albums.NUMBER_OF_SONGS)
        val indexArtist = albumCursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST)
        var indexAlbumArt = -1
        try {
            indexAlbumArt = albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART)
        } catch (e: Exception) {
            Log.e(TAG, "addAlbumsToList: Couldn't get index for album art, seems to miss", e)
        }

        do {
            var albumId = albumCursor.fetchString(indexID)
            val albumTitle = albumCursor.fetchString(indexAlbum)
            val albumTracks = albumCursor.fetchString(indexNumberOfSongs)
            val albumArtist = albumCursor.fetchString(indexArtist)
            var albumArt = ""
            if(indexAlbumArt >= 0) {
                albumArt = albumCursor.fetchString(indexAlbumArt)
            }
            val uri = ContentUris.withAppendedId(albumStoreURI, albumId.toLong())
            val album = MusicMetadata(
                albumId, albumTitle, albumArtist,
                cover_uri = albumArt,
                num_tracks_album = albumTracks.toInt(),
                content_uri = uri
            )
            currentMusicList.addItem(album)
        } while (albumCursor.moveToNext())
    }

    override fun queryTitlesByAlbumID(albumId: String): MusicList {
        val selection = MediaStore.Audio.Media.ALBUM_ID + "='" + albumId + "'"
        val titleCursor = queryTitleTable(AudioColumns.metadataColumns, where = selection)
        if(titleCursor != null && titleCursor.moveToFirst()){
            currentMusicList = MusicList()
            currentMusicList.setMediaType(MediaBrowser.MediaItem.FLAG_PLAYABLE)
            addTitlesToAlbumList(titleCursor)
        }
        titleCursor?.close()
        return currentMusicList
    }

    private fun addTitlesToAlbumList(titleCursor: Cursor){
        do {
            val trackId = titleCursor.fetchString(MediaStore.Audio.Media._ID)
            val music = parseMetadata(titleCursor, trackId)
            currentMusicList.addItem(music)
        } while(titleCursor.moveToNext())
    }

    // query the first title in the database for shuffle of all titles
    override fun queryFirstTitle(): MusicMetadata {
        val sortOrder = MediaStore.Audio.Media._ID + " LIMIT 1"
        val titleCursor = queryTitleTable(arrayOf(MediaStore.Audio.Media._ID), sortOrder = sortOrder)
        val metadata = if (titleCursor != null && titleCursor.moveToFirst()) {
            parseMetadata(titleCursor, titleCursor.fetchString(MediaStore.Audio.Media._ID))
        } else {
            MusicMetadata()
        }
        titleCursor?.close()
        return metadata
    }

    // query all titles of database
    override fun queryTitles(playingTitleId: String): MusicList {
        var allTitles = MusicList()
        val albumCursor = queryAlbumTable(arrayOf(MediaStore.Audio.Albums._ID))
        if(albumCursor != null && albumCursor.moveToFirst()){
            allTitles = addAllTitlesToList(albumCursor, playingTitleId)
        }
        albumCursor?.close()
        Log.d(TAG, "queryTitles: Anzahl QueueItems: ${currentMusicList.size()}")
        return allTitles
    }

    private fun addAllTitlesToList(albumCursor: Cursor, playingTitleId: String): MusicList {
        val indexAlbumID = MediaStore.Audio.Albums._ID
        val allTitles = MusicList()
        do {
            val albumId = albumCursor.fetchString(indexAlbumID)
            val titleList = queryTitlesByAlbumID(albumId)
            for(item in titleList){
                if(item.id != playingTitleId){
                    allTitles.addItem(item)
                }
            }
        } while(albumCursor.moveToNext())
        return allTitles
    }

    private fun parseMetadata(titleCursor: Cursor, mediaId: String): MusicMetadata {
        val albumId = titleCursor.fetchString(MediaStore.Audio.Media.ALBUM_ID)
        val coverUri = getCoverPathForAlbum(albumId)     // notwendig für backward compatibility?
        val uri = ContentUris.withAppendedId(mediaStoreURI, mediaId.toLong())
        // TODO get duration by other means ex. by MediaMetadataRetriever, when API < 29
        return MusicMetadata(
            id = mediaId,
            album = titleCursor.fetchString(MediaStore.Audio.Media.ALBUM),
            artist = titleCursor.fetchString(MediaStore.Audio.Media.ARTIST),
            title = titleCursor.fetchString(MediaStore.Audio.Media.TITLE),
            cover_uri = coverUri,
            path = titleCursor.fetchString(MediaStore.Audio.Media.DATA),
            duration = titleCursor.fetchLong(MediaStore.Audio.Media.DURATION),
            content_uri = uri,
            display_name = titleCursor.fetchString(MediaStore.Audio.Media.DISPLAY_NAME))
    }

    private fun getCoverPathForAlbum(albumId: String): String{
        var cover = ""
        val selection = MediaStore.Audio.Albums._ID + "='" + albumId + "'"
        val cursorAlbum = queryAlbumTable(AudioColumns.albumArtColumns, where = selection)
        if(cursorAlbum != null && cursorAlbum.count == 1){
            cursorAlbum.moveToFirst()
            cover = cursorAlbum.fetchString(MediaStore.Audio.Albums.ALBUM_ART)
        }
        cursorAlbum?.close()
        return cover
    }

    private fun Cursor.fetchString(column: String): String{
        val index = this.getColumnIndex(column)
        return this.fetchString(index)
    }

    private fun Cursor.fetchString(index: Int): String{
        return if(index != -1) {
            this.getString(index) ?: ""
        } else {
            ""
        }
    }

    private fun Cursor.fetchLong(column: String): Long{
        val index = this.getColumnIndex(column)
        return if(index >= 0) {
            this.getLong(index)
        } else {
            0L
        }
    }


    private fun queryTitleTable(columns: Array<String>,
                                where: String? = null,
                                whereArgs: Array<String>? = null,
                                sortOrder: String? = null): Cursor? {
        return context.contentResolver.query(
            mediaStoreURI, columns, where, whereArgs, sortOrder)
    }

    private fun queryAlbumTable(columns: Array<String>,
                                where: String? = null,
                                whereArgs: Array<String>? = null,
                                sortOrder: String? = null): Cursor? {
        return context.contentResolver.query(
            albumStoreURI,
            columns, where, whereArgs, sortOrder)
    }

}