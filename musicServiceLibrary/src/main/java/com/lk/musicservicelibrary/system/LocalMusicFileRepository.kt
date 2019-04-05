package com.lk.musicservicelibrary.system

import android.content.Context
import android.database.Cursor
import android.media.browse.MediaBrowser
import android.provider.MediaStore
import android.util.Log
import com.lk.musicservicelibrary.models.MusicList
import com.lk.musicservicelibrary.models.MusicMetadata
import com.lk.musicservicelibrary.utils.Columns

/**
 * Erstellt von Lena am 01/04/2019.
 */
class LocalMusicFileRepository(private val context: Context): MusicDataRepository() {

    private val TAG = "LocalMusicFileRepo"

    private var currentMusicList = MusicList()

    override fun getFirstTitleIDForShuffle(): String{
        val sortOrder = MediaStore.Audio.Media._ID + " LIMIT 1"
        val titleCursor = queryTitleTable(arrayOf(MediaStore.Audio.Media._ID), sortOrder = sortOrder)
        val titleId = if (titleCursor != null && titleCursor.moveToFirst()) {
            titleCursor.fetchString(MediaStore.Audio.Media._ID)
        } else {
            ""
        }
        titleCursor?.close()
        return titleId
    }

    override fun getAllTitles(playingTitleId: String): MusicList {
        var allTitles = MusicList()
        val albumCursor = queryAlbumTable(arrayOf(MediaStore.Audio.Albums._ID))
        if(albumCursor != null && albumCursor.moveToFirst()){
            allTitles = addAllTitlesToList(allTitles, albumCursor, playingTitleId)
        }
        albumCursor?.close()
        Log.d(TAG, "Anzahl QueueItems: ${currentMusicList.size()}")
        return allTitles
    }

    private fun addAllTitlesToList(allTitles: MusicList, albumCursor: Cursor, playingTitleId: String): MusicList {
        do {
            val albumId = albumCursor.fetchString(MediaStore.Audio.Albums._ID)
            val titleList = getTitlesByAlbumID(albumId)
            for(item in titleList){
                if(item.id != playingTitleId){
                    allTitles.addItem(getTitleByID(item.id))
                }
            }
        } while(albumCursor.moveToNext())
        return allTitles
    }



    override fun getTitlesByAlbumID(albumId: String): MusicList {
        val albumID = albumId.replace("ALBUM-", "")
        val selection = MediaStore.Audio.Media.ALBUM_ID + "='" + albumID + "'"
        val titleCursor = queryTitleTable(Columns.titlesColumns, where = selection)
        if(titleCursor != null && titleCursor.moveToFirst()){
            currentMusicList = MusicList()
            currentMusicList.setMediaType(MediaBrowser.MediaItem.FLAG_PLAYABLE)
            addTitlesToAlbumList(titleCursor, albumId)
        }
        titleCursor?.close()
        return currentMusicList
    }

    private fun addTitlesToAlbumList(titleCursor: Cursor, albumId: String){
        do {
            val trackId = titleCursor.fetchString(MediaStore.Audio.Media._ID)
            val trackTitle = titleCursor.fetchString(MediaStore.Audio.Media.TITLE)
            val interpret = titleCursor.fetchString(MediaStore.Audio.Media.ARTIST)
            val album = titleCursor.fetchString(MediaStore.Audio.Media.ALBUM)
            val music = MusicMetadata(
                trackId,
                album,
                interpret,
                trackTitle,
                cover_uri = getCoverPathForAlbum(albumId)
            )
            currentMusicList.addItem(music)
        } while(titleCursor.moveToNext())
    }



    override fun getAllAlbums(): MusicList {
        val sortOrder = MediaStore.Audio.Albums.ALBUM + " ASC"
        val albumCursor = queryAlbumTable(Columns.albumsColumns, sortOrder = sortOrder)
        currentMusicList = MusicList()
        currentMusicList.setMediaType(MediaBrowser.MediaItem.FLAG_BROWSABLE)
        if(albumCursor != null && albumCursor.moveToFirst()) {
            addAlbumsToList(albumCursor)
        }
        albumCursor?.close()
        return currentMusicList
    }

    // TODO Fix für Android Q, Cover darf so nicht mehr geladen werden, stattdessen ContentResolver.loadThumbnail()
    private fun addAlbumsToList(albumCursor: Cursor){
        do {
            var albumId = albumCursor.fetchString(MediaStore.Audio.Media._ID)
            val albumTitle = albumCursor.fetchString(MediaStore.Audio.Albums.ALBUM)
            val albumTracks = albumCursor.fetchString(MediaStore.Audio.Albums.NUMBER_OF_SONGS)
            val albumArtist = albumCursor.fetchString(MediaStore.Audio.Albums.ARTIST)
            val albumArt = albumCursor.fetchString(MediaStore.Audio.Albums.ALBUM_ART)
            albumId = "ALBUM-$albumId"
            val music = MusicMetadata(
                albumId,
                albumTitle,
                albumArtist,
                cover_uri = albumArt,
                num_tracks_album = albumTracks.toInt()
            )
            currentMusicList.addItem(music)
        } while (albumCursor.moveToNext())
    }

    private fun getCoverPathForAlbum(albumId: String): String{
        var cover = ""
        val selection = MediaStore.Audio.Albums._ID + "='" + albumId + "'"
        val cursorAlbum = queryAlbumTable(Columns.albumArtColumns, where = selection)
        if(cursorAlbum != null && cursorAlbum.count == 1){
            cursorAlbum.moveToFirst()
            cover = cursorAlbum.fetchString(MediaStore.Audio.Albums.ALBUM_ART)
        }
        cursorAlbum?.close()
        return cover
    }



    override fun getTitleByID (titleId: String): MusicMetadata {
        var music = MusicMetadata()
        val selection = MediaStore.Audio.Media._ID + "='" + titleId + "'"
        val titleCursor = queryTitleTable(Columns.metadataColumns, where = selection)
        if(titleCursor  != null && titleCursor.moveToFirst()){
            music = parseMetadata(titleCursor, titleId)
        }
        titleCursor?.close()
        return music
    }

    private fun parseMetadata(titleCursor: Cursor, mediaId: String): MusicMetadata {
        val albumId = titleCursor.fetchString(MediaStore.Audio.Media.ALBUM_ID)
        val coverUri = getCoverPathForAlbum(albumId)
        return MusicMetadata(
            id = mediaId,
            album = titleCursor.fetchString(MediaStore.Audio.Media.ALBUM),
            artist = titleCursor.fetchString(MediaStore.Audio.Media.ARTIST),
            title = titleCursor.fetchString(MediaStore.Audio.Media.TITLE),
            cover_uri = coverUri,
            path = titleCursor.fetchString(MediaStore.Audio.Media.DATA),
            duration = titleCursor.fetchLong(MediaStore.Audio.Media.DURATION))
    }



    private fun Cursor.fetchString(column: String): String =
        this.getString(this.getColumnIndexOrThrow(column))

    private fun Cursor.fetchLong(column: String): Long =
        this.getLong(this.getColumnIndexOrThrow(column))

    private fun queryTitleTable(columns: Array<String>,
                                where: String? = null,
                                whereArgs: Array<String>? = null,
                                sortOrder: String? = null): Cursor? {
        return context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            columns, where, whereArgs, sortOrder)
    }

    private fun queryAlbumTable(columns: Array<String>,
                                where: String? = null,
                                whereArgs: Array<String>? = null,
                                sortOrder: String? = null): Cursor? {
        return context.contentResolver.query(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            columns, where, whereArgs, sortOrder)
    }

}