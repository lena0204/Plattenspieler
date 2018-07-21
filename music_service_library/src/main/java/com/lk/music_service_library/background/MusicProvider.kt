package com.lk.music_service_library.background

import android.content.Context
import android.media.browse.MediaBrowser
import android.provider.MediaStore
import android.util.Log
import com.lk.music_service_library.models.MusicList
import com.lk.music_service_library.models.MusicMetadata

/**
 * Created by Lena on 08.06.17.
 * Stellt die Metadaten für die Musik bereit, Zugriff auf die systeminterne Datenbank
 */
class MusicProvider(private val c: Context) {

    // Zugriff auf die Datenbanken mit der Musik und Aufbereitung der Daten
    companion object {
        const val ROOT_ID = "__ ROOT__"
    }

    private val projectionAlbumArt = arrayOf(MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART)
    private val TAG = "com.lk.pl-MusicProvider"

	// ersten Titel festlegen, wenn alle Titel zufällig abgespielt werden sollen
	fun getFirstTitle(): String{
		// Albumdatenbankspalten
		var titleId = ""
        val sortorder = MediaStore.Audio.Media._ID + " LIMIT 1"
        val cursorTitle = this.c.contentResolver.query(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Audio.Media._ID), null,null,sortorder)
        if(cursorTitle.moveToFirst()){
            titleId = cursorTitle.getString(cursorTitle.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
        }
        cursorTitle.close()
		return titleId
	}

    fun getAllTitles(playingTitleId: String): MusicList {
		val liste = MusicList()
        var albumid: String
		// Albumdatenbankspalten
		val projection = arrayOf(MediaStore.Audio.Albums._ID)
		val cursorAlbum = c.contentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, projection, null, null, null)
        if(cursorAlbum.moveToFirst()){
			do {
                // für alle Alben die Titel abfragen und hinzufügen
				albumid = cursorAlbum.getString(cursorAlbum.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID))
                val titelliste = getTitlesForAlbumID(albumid)
                for(item in titelliste){
                    // Überprüfen ob der bereits Spielende dabei ist, falls ja, nicht hinzufügen
                    if(item.id != playingTitleId){
                        liste.addItem(item)
                    }
                }
			} while(cursorAlbum.moveToNext())
		}
		cursorAlbum.close()
		Log.d(TAG, "Anzahl QueueItems: ${liste.countItems()}")
		return liste
    }

    fun getTitlesForAlbumID(albumid: String): MusicList{
        // Albumcover abfragen
        val albumID = albumid.replace("ALBUM-", "")
        val selection = android.provider.MediaStore.Audio.Media.ALBUM_ID + "='" + albumID + "'"
        // Datenbank abfragen
        val cursorTitles = c.contentResolver.query(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,selection,null,null)
        val list = MusicList()
        list.addFlag(MediaBrowser.MediaItem.FLAG_PLAYABLE)
        if(cursorTitles.moveToFirst()){
            do {
                val trackid = cursorTitles.getString(cursorTitles.getColumnIndex(MediaStore.Audio.Media._ID))
                val tracktitle = cursorTitles.getString(cursorTitles.getColumnIndex(MediaStore.Audio.Media.TITLE))
                val interpret = cursorTitles.getString(cursorTitles.getColumnIndex(MediaStore.Audio.Media.ARTIST))
                val album = cursorTitles.getString(cursorTitles.getColumnIndex(MediaStore.Audio.Media.ALBUM))
                val music = MusicMetadata(
                        trackid,
                        album,
                        interpret,
                        tracktitle,
                        cover_uri = getCoverForAlbum(albumID)
                )
                list.addItem(music)
            } while(cursorTitles.moveToNext())
        }
        cursorTitles.close()
        return list
    }
    fun getAlbums(): MusicList{
        // Alben abfragen (SELECT, ORDERBY definieren)
        val orderby = MediaStore.Audio.Albums.ALBUM + " ASC"
        val projection = arrayOf(
                MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ALBUM_ART,
                MediaStore.Audio.Albums.ARTIST,
                MediaStore.Audio.Albums.NUMBER_OF_SONGS)
        // Datenbankabfrage und Weitergabe an den Provider
        val cursorAlbums = this.c.contentResolver.query(android.provider.MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, projection,null,null,orderby)
        val list = MusicList()
        list.addFlag(MediaBrowser.MediaItem.FLAG_BROWSABLE)
        if(cursorAlbums.moveToFirst()) {
            do {
                var albumid = cursorAlbums.getString(cursorAlbums.getColumnIndex(MediaStore.Audio.Media._ID))
                val albumtitle = cursorAlbums.getString(cursorAlbums.getColumnIndex(MediaStore.Audio.Albums.ALBUM))
                val albumtracks = cursorAlbums.getString(cursorAlbums.getColumnIndex(MediaStore.Audio.Albums.NUMBER_OF_SONGS))
                val albumartist = cursorAlbums.getString(cursorAlbums.getColumnIndex(MediaStore.Audio.Albums.ARTIST))
                var albumart = cursorAlbums.getString(cursorAlbums.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ART))
                if(albumart == null){
                    albumart = ""
                }
                albumid = "ALBUM-$albumid"
                val music = MusicMetadata(
                        albumid,
                        albumtitle,
                        albumartist,
                        cover_uri = albumart,
                        num_tracks = albumtracks.toInt()
                )
                list.addItem(music)
            } while (cursorAlbums.moveToNext())
        }
        cursorAlbums.close()
        return list
    }

    private fun getCoverForAlbum(albumid: String): String{
        var cover = ""
        val selection = MediaStore.Audio.Albums._ID + "='" + albumid + "'"
        val cursorAlbum = c.contentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, projectionAlbumArt, selection, null, null)
        if(cursorAlbum.count == 1){
            cursorAlbum.moveToFirst()
            cover = cursorAlbum.getString(cursorAlbum.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ART))
        }
        cursorAlbum.close()
        return cover
    }

    fun getFileFromMediaId(mediaId: String?): String{
        var selection: String? = null
        if(mediaId != null){
            // eine spezifische ID abfragen, sonst den ersten Titel
            selection = MediaStore.Audio.Media._ID + "='" + mediaId + "'"
        }
        var result = "ERROR"
        val projection = arrayOf(MediaStore.Audio.Media._ID,MediaStore.Audio.Media.DATA)
        val cursor = c.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null, null)
        if(cursor != null){
            cursor.moveToFirst()
            result = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
        }
        cursor.close()
        return result
    }

    fun getMediaMetadata(mediaId: String, songnumber: String): MusicMetadata {
        var music = MusicMetadata()
        // eine spezifische ID abfragen, sonst den ersten Titel
        val selection = MediaStore.Audio.Media._ID + "='" + mediaId + "'"
        val projection = arrayOf(
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DATA)
        val cursor = c.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null, null)
        if(cursor != null && cursor.moveToFirst()){
            // Albumcover abfragen, um es in die Wiedergabe einzubinden
            val albumid = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))
            val cover = getCoverForAlbum(albumid)
            music = MusicMetadata(
                    mediaId,
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)),
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)),
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)),
                    cover,
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)),
                    songnumber.toLong())
        }
        cursor.close()
        return music
    }

}