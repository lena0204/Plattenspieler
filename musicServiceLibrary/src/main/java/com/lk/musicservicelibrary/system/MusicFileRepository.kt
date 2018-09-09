package com.lk.musicservicelibrary.system

import android.content.Context
import android.database.Cursor
import android.media.browse.MediaBrowser
import android.provider.MediaStore.Audio as MSAudio
import android.util.Log
import com.lk.musicservicelibrary.models.*
import com.lk.musicservicelibrary.utils.Columns

/**
 * Erstellt von Lena am 02.09.18.
 */
class MusicFileRepository(private val context: Context) {

    companion object {
        const val ROOT_ID = "__ ROOT__"
    }
    private val TAG = "com.lk.pl-MusicProvider"

    private var currentMusicList = MusicList()

    fun getFirstTitleForShuffle(): String{
        val sortorder = MSAudio.Media._ID + " LIMIT 1"
        val titleCursor = queryCursor(arrayOf(MSAudio.Media._ID), sortOrder = sortorder)
        val titleId = if (titleCursor != null && titleCursor.moveToFirst()) {
            titleCursor.fetchString(MSAudio.Media._ID)
        } else {
            ""
        }
        titleCursor?.close()
        return titleId
    }

    fun getAllTitles(playingTitleId: String): MusicList {
        var allTitles = MusicList()
        val albumCursor = queryCursorAlbum(arrayOf(MSAudio.Albums._ID))
        if(albumCursor != null && albumCursor.moveToFirst()){
            allTitles = addAllTitlesToList(allTitles, albumCursor, playingTitleId)
        }
        albumCursor?.close()
        Log.d(TAG, "Anzahl QueueItems: ${currentMusicList.countItems()}")
        return allTitles
    }

    private fun addAllTitlesToList(allTitles: MusicList, albumCursor: Cursor, playingTitleId: String): MusicList{
        do {
            val albumid = albumCursor.fetchString(MSAudio.Albums._ID)
            val titelliste = getTitlesForAlbumID(albumid)
            for(item in titelliste){
                if(item.id != playingTitleId){
                    allTitles.addItem(getMediaMetadata(item.id))
                }
            }
        } while(albumCursor.moveToNext())
        return allTitles
    }

    fun getTitlesForAlbumID(albumid: String): MusicList{
        val albumID = albumid.replace("ALBUM-", "")
        val selection = MSAudio.Media.ALBUM_ID + "='" + albumID + "'"
        val titleCursor = queryCursor(Columns.titleListColumns, where = selection)
        if(titleCursor != null && titleCursor.moveToFirst()){
            currentMusicList = MusicList()
            currentMusicList.addFlag(MediaBrowser.MediaItem.FLAG_PLAYABLE)
            writeTitlesToList(titleCursor, albumid)
        }
        titleCursor?.close()
        return currentMusicList
    }

    private fun writeTitlesToList(titleCursor: Cursor, albumid: String){
        do {
            val trackid = titleCursor.fetchString(MSAudio.Media._ID)
            val tracktitle = titleCursor.fetchString(MSAudio.Media.TITLE)
            val interpret = titleCursor.fetchString(MSAudio.Media.ARTIST)
            val album = titleCursor.fetchString(MSAudio.Media.ALBUM)
            val music = MusicMetadata(
                    trackid,
                    album,
                    interpret,
                    tracktitle,
                    cover_uri = getCoverPathForAlbum(albumid)
            )
            currentMusicList.addItem(music)
        } while(titleCursor.moveToNext())
    }

    fun getAlbums(): MusicList{
        val orderby = MSAudio.Albums.ALBUM + " ASC"
        val albumCursor = queryCursorAlbum(Columns.albumlistColumns, sortOrder = orderby)
        currentMusicList = MusicList()
        currentMusicList.addFlag(MediaBrowser.MediaItem.FLAG_BROWSABLE)
        if(albumCursor != null && albumCursor.moveToFirst()) {
            writeAlbumsToList(albumCursor)
        }
        albumCursor?.close()
        return currentMusicList
    }

    private fun writeAlbumsToList(albumCursor: Cursor){
        do {
            var albumid = albumCursor.fetchString(MSAudio.Media._ID)
            val albumtitle = albumCursor.fetchString(MSAudio.Albums.ALBUM)
            val albumtracks = albumCursor.fetchString(MSAudio.Albums.NUMBER_OF_SONGS)
            val albumartist = albumCursor.fetchString(MSAudio.Albums.ARTIST)
            val albumart = albumCursor.fetchString(MSAudio.Albums.ALBUM_ART)
            /*if(albumart == null){
                albumart = ""
            }*/
            albumid = "ALBUM-$albumid"
            val music = MusicMetadata(
                    albumid,
                    albumtitle,
                    albumartist,
                    cover_uri = albumart,
                    num_tracks_album = albumtracks.toInt()
            )
            currentMusicList.addItem(music)
        } while (albumCursor.moveToNext())
    }

    private fun getCoverPathForAlbum(albumid: String): String{
        var cover = ""
        val selection = MSAudio.Albums._ID + "='" + albumid + "'"
        val cursorAlbum = queryCursorAlbum(Columns.albumartColumns, where = selection)
        if(cursorAlbum != null && cursorAlbum.count == 1){
            cursorAlbum.moveToFirst()
            cover = cursorAlbum.fetchString(MSAudio.Albums.ALBUM_ART)
        }
        cursorAlbum?.close()
        return cover
    }

    fun getMediaMetadata(mediaId: String): MusicMetadata {
        var music = MusicMetadata()
        val selection = MSAudio.Media._ID + "='" + mediaId + "'"
        val titleCursor = queryCursor(Columns.metadataColumns, where = selection)
        if(titleCursor  != null && titleCursor.moveToFirst()){
            music = writeMetadata(titleCursor, mediaId)
        }
        titleCursor?.close()
        return music
    }

    private fun writeMetadata(titleCursor: Cursor, mediaId: String): MusicMetadata{
        val albumid = titleCursor.fetchString(MSAudio.Media.ALBUM_ID)
        val coverUri = getCoverPathForAlbum(albumid)
        return MusicMetadata(
                id = mediaId,
                album = titleCursor.fetchString(MSAudio.Media.ALBUM),
                artist = titleCursor.fetchString(MSAudio.Media.ARTIST),
                title = titleCursor.fetchString(MSAudio.Media.TITLE),
                cover_uri = coverUri,
                path = titleCursor.fetchString(MSAudio.Media.DATA),
                duration = titleCursor.fetchLong(MSAudio.Media.DURATION))
    }

    private fun Cursor.fetchString(column: String): String =
        this.getString(this.getColumnIndexOrThrow(column))

    private fun Cursor.fetchLong(column: String): Long =
        this.getLong(this.getColumnIndexOrThrow(column))

    private fun queryCursor(columns: Array<String>,
                            where: String? = null,
                            whereArgs: Array<String>? = null,
                            sortOrder: String? = null): Cursor? {
        return context.contentResolver.query(MSAudio.Media.EXTERNAL_CONTENT_URI,
                columns, where, whereArgs, sortOrder)
    }

    private fun queryCursorAlbum(columns: Array<String>,
                            where: String? = null,
                            whereArgs: Array<String>? = null,
                            sortOrder: String? = null): Cursor? {
        return context.contentResolver.query(MSAudio.Albums.EXTERNAL_CONTENT_URI,
                columns, where, whereArgs, sortOrder)
    }
}