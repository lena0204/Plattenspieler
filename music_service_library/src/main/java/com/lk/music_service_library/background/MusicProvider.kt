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
    private lateinit var currentAlbumCursor: Cursor
    private lateinit var currentTitleCursor: Cursor

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
		var titleId = ""
        val sortorder = MediaStore.Audio.Media._ID + " LIMIT 1"
        val currentTitleCursor = this.context.contentResolver.query(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Media._ID), null,null,sortorder)
        if(currentTitleCursor.moveToFirst()){
            titleId = currentTitleCursor.getString(
                    currentTitleCursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
        }
        currentTitleCursor.close()
		return titleId
	}

    fun getAllTitles(playingTitleId: String): MusicList {
		currentMusicList = MusicList()
		val albumDatabaseColumns = arrayOf(MediaStore.Audio.Albums._ID)
		val currentAlbumCursor = context.contentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumDatabaseColumns,
                null, null, null)
        if(currentAlbumCursor.moveToFirst()){
            addAllTitlesToList(playingTitleId)
		}
		currentAlbumCursor.close()
		Log.d(TAG, "Anzahl QueueItems: ${currentMusicList.countItems()}")
		return currentMusicList
    }

    private fun addAllTitlesToList(playingTitleId: String){
        do {
            val albumid = currentAlbumCursor.getString(currentAlbumCursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID))
            val titelliste = getTitlesForAlbumID(albumid)
            for(item in titelliste){
                if(item.id != playingTitleId){
                    currentMusicList.addItem(item)
                }
            }
        } while(currentAlbumCursor.moveToNext())
    }

    fun getTitlesForAlbumID(albumid: String): MusicList{
        val albumID = albumid.replace("ALBUM-", "")
        val selection = android.provider.MediaStore.Audio.Media.ALBUM_ID + "='" + albumID + "'"
        currentTitleCursor = context.contentResolver.query(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,selection,null,null)
        currentMusicList = MusicList()
        currentMusicList.addFlag(MediaBrowser.MediaItem.FLAG_PLAYABLE)
        if(currentTitleCursor.moveToFirst()){
            writeTitlesToList(albumid)
        }
        currentTitleCursor.close()
        return currentMusicList
    }

    private fun writeTitlesToList(albumid: String){
        do {
            val trackid = currentTitleCursor.getString(currentTitleCursor.getColumnIndex(MediaStore.Audio.Media._ID))
            val tracktitle = currentTitleCursor.getString(currentTitleCursor.getColumnIndex(MediaStore.Audio.Media.TITLE))
            val interpret = currentTitleCursor.getString(currentTitleCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST))
            val album = currentTitleCursor.getString(currentTitleCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM))
            val music = MusicMetadata(
                    trackid,
                    album,
                    interpret,
                    tracktitle,
                    cover_uri = getCoverPathForAlbum(albumid)
            )
            currentMusicList.addItem(music)
        } while(currentTitleCursor.moveToNext())
    }

    fun getAlbums(): MusicList{
        val orderby = MediaStore.Audio.Albums.ALBUM + " ASC"
        val currentAlbumCursor = this.context.contentResolver.query(android.provider.MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                albumDatabaseColumns,null,null,orderby)
        currentMusicList = MusicList()
        currentMusicList.addFlag(MediaBrowser.MediaItem.FLAG_BROWSABLE)
        if(currentAlbumCursor.moveToFirst()) {
            writeAlbumsToList()
        }
        currentAlbumCursor.close()
        return currentMusicList
    }

    private fun writeAlbumsToList(){
        do {
            var albumid = currentAlbumCursor.getString(currentAlbumCursor.getColumnIndex(MediaStore.Audio.Media._ID))
            val albumtitle = currentAlbumCursor.getString(currentAlbumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM))
            val albumtracks = currentAlbumCursor.getString(currentAlbumCursor.getColumnIndex(MediaStore.Audio.Albums.NUMBER_OF_SONGS))
            val albumartist = currentAlbumCursor.getString(currentAlbumCursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST))
            var albumart = currentAlbumCursor.getString(currentAlbumCursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ART))
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
        } while (currentAlbumCursor.moveToNext())
    }

    private fun getCoverPathForAlbum(albumid: String): String{
        var cover = ""
        val selection = MediaStore.Audio.Albums._ID + "='" + albumid + "'"
        val cursorAlbum = context.contentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                albumartColumns, selection, null, null)
        if(cursorAlbum.count == 1){
            cursorAlbum.moveToFirst()
            cover = cursorAlbum.getString(cursorAlbum.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ART))
        }
        cursorAlbum.close()
        return cover
    }

    fun getFilePathFromMediaId(mediaId: String?): String{
        var selection: String? = null
        if(mediaId != null){
            selection = MediaStore.Audio.Media._ID + "='" + mediaId + "'"
        }
        var result = "ERROR"
        val columns = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA)
        val cursor = context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                columns, selection, null, null)
        if(cursor != null){
            cursor.moveToFirst()
            result = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
        }
        cursor.close()
        return result
    }

    fun getMediaMetadata(mediaId: String, songnumber: String): MusicMetadata {
        var music = MusicMetadata()
        val selection = MediaStore.Audio.Media._ID + "='" + mediaId + "'"
        currentTitleCursor = context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                metadataColumns, selection, null, null)
        if(currentTitleCursor.moveToFirst()){
            music = writeMetadata(mediaId, songnumber)
        }
        currentTitleCursor.close()
        return music
    }

    private fun writeMetadata(mediaId: String, songnumber: String): MusicMetadata{
        val albumid = currentTitleCursor.getString(currentTitleCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))
        val cover = getCoverPathForAlbum(albumid)
        return MusicMetadata(
                id = mediaId,
                album = currentTitleCursor.getString(currentTitleCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)),
                artist = currentTitleCursor.getString(currentTitleCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)),
                title = currentTitleCursor.getString(currentTitleCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)),
                cover_uri = cover,
                path = currentTitleCursor.getString(currentTitleCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)),
                duration = currentTitleCursor.getLong(currentTitleCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)),
                nr_of_songs_left = songnumber.toLong())
    }

}