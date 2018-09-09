package com.lk.music_service_library.background

import android.content.Context
import android.database.Cursor
import android.media.browse.MediaBrowser
import android.provider.MediaStore
import android.util.Log
import com.lk.music_service_library.models.MusicList
import com.lk.music_service_library.models.MusicMetadata

/**
 * Created by Lena on 08.06.17.
 * Stellt die Metadaten für die Musik bereit, Zugriff auf die systeminterne Datenbank
 */
class MusicProvider(private val context: Context) {

    companion object {
        const val ROOT_ID = "__ ROOT__"
    }

    private var currentMusicList = MusicList()

    private val albumartColumns = arrayOf(MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART)
    private val albumDatabaseColumns = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ALBUM_ART,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS)
    private val metadataColumns = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA)
    private val TAG = "com.lk.pl-MusicProvider"

	fun getFirstTitleForShuffle(): String{
        val sortorder = MediaStore.Audio.Media._ID + " LIMIT 1"
        val titleCursor = this.context.contentResolver.query(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Media._ID), null,null,sortorder)
        val titleId = if (titleCursor != null && titleCursor.moveToFirst())
            titleCursor.getString(titleCursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
        else
            ""
        titleCursor?.close()
		return titleId
	}

    fun getAllTitles(playingTitleId: String): MusicList {
		currentMusicList = MusicList()
		val albumDatabaseColumns = arrayOf(MediaStore.Audio.Albums._ID)
        val albumCursor = context.contentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumDatabaseColumns,
                null, null, null)
        if(albumCursor != null && albumCursor.moveToFirst()){
            addAllTitlesToList(albumCursor, playingTitleId)
		}
		albumCursor?.close()
		Log.d(TAG, "Anzahl QueueItems: ${currentMusicList.countItems()}")
		return currentMusicList
    }

    private fun addAllTitlesToList(albumCursor: Cursor, playingTitleId: String){
        do {
            val albumid = albumCursor.getString(albumCursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID))
            val titelliste = getTitlesForAlbumID(albumid)
            for(item in titelliste){
                if(item.id != playingTitleId){
                    currentMusicList.addItem(item)
                }
            }
        } while(albumCursor.moveToNext())
    }

    fun getTitlesForAlbumID(albumid: String): MusicList{
        val albumID = albumid.replace("ALBUM-", "")
        val selection = android.provider.MediaStore.Audio.Media.ALBUM_ID + "='" + albumID + "'"
        val titleCursor = context.contentResolver.query(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,selection,null,null)
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
            val trackid = titleCursor.getString(titleCursor.getColumnIndex(MediaStore.Audio.Media._ID))
            val tracktitle = titleCursor.getString(titleCursor.getColumnIndex(MediaStore.Audio.Media.TITLE))
            val interpret = titleCursor.getString(titleCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST))
            val album = titleCursor.getString(titleCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM))
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
        val orderby = MediaStore.Audio.Albums.ALBUM + " ASC"
        val albumCursor = this.context.contentResolver.query(android.provider.MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                albumDatabaseColumns,null,null,orderby)
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
            var albumid = albumCursor.getString(albumCursor.getColumnIndex(MediaStore.Audio.Media._ID))
            val albumtitle = albumCursor.getString(albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM))
            val albumtracks = albumCursor.getString(albumCursor.getColumnIndex(MediaStore.Audio.Albums.NUMBER_OF_SONGS))
            val albumartist = albumCursor.getString(albumCursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST))
            var albumart = albumCursor.getString(albumCursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ART))
            if(albumart == null){
                albumart = ""
            }
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
        val selection = MediaStore.Audio.Albums._ID + "='" + albumid + "'"
        val cursorAlbum = context.contentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                albumartColumns, selection, null, null)
        if(cursorAlbum != null && cursorAlbum.count == 1){
            cursorAlbum.moveToFirst()
            cover = cursorAlbum.getString(cursorAlbum.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ART))
        }
        cursorAlbum?.close()
        return cover
    }

    fun getMediaMetadata(mediaId: String): MusicMetadata {
        var music = MusicMetadata()
        val selection = MediaStore.Audio.Media._ID + "='" + mediaId + "'"
        val titleCursor = context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                metadataColumns, selection, null, null)
        if(titleCursor  != null && titleCursor.moveToFirst()){
            music = writeMetadata(titleCursor, mediaId)
        }
        titleCursor?.close()
        return music
    }

    private fun writeMetadata(titleCursor: Cursor, mediaId: String): MusicMetadata{
        val albumid = titleCursor.getString(titleCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))
        val coverUri = getCoverPathForAlbum(albumid)
        return MusicMetadata(
                id = mediaId,
                album = titleCursor.getString(titleCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)),
                artist = titleCursor.getString(titleCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)),
                title = titleCursor.getString(titleCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)),
                cover_uri = coverUri,
                path = titleCursor.getString(titleCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)),
                duration = titleCursor.getLong(titleCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)))
    }

}