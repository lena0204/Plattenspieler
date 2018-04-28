package com.lk.plattenspieler.background

import android.content.Context
import android.database.Cursor
import android.graphics.BitmapFactory
import android.media.MediaDescription
import android.media.MediaMetadata
import android.media.browse.MediaBrowser
import android.media.session.MediaSession
import android.provider.MediaStore
import android.util.Log

/**
 * Created by Lena on 08.06.17.
 */
class MusicProvider(private val c: Context) {

    // Zugriff auf die Datenbanken mit der Musik und Aufbereitung der Daten

    val ROOT_ID = "__ ROOT__"
    val TAG = "com.lk.pl-MusicProvider"

	// ersten Titel festlegen, wenn alle Titel zufällig abgespielt werden sollen
	fun getFirstTitle(): String{
		// Albumdatenbankspalten
		val projection = Array(1, init = {_ -> ""})
		var result = ""
		projection[0] = MediaStore.Audio.Albums._ID
		val cursorAlbum = c.contentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, projection, null, null, null)
		// Titeldatenbankspalten
		projection[0] = MediaStore.Audio.Media.ALBUM_ID
		if(cursorAlbum.moveToFirst()){
			val albumid = cursorAlbum.getString(cursorAlbum.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID))
			val selection = android.provider.MediaStore.Audio.Media.ALBUM_ID + "='" + albumid + "'"
			// Titeldatenbank abfragen
			val cursorTitle = this.c.contentResolver.query(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,selection,null,null)
			if(cursorTitle.moveToFirst()){
				result = cursorTitle.getString(cursorTitle.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
			}
			cursorTitle.close()
		}
		cursorAlbum.close()
		return result
	}

    fun getAllTitle(playingTitleId: String): MutableList<MediaSession.QueueItem>{
		val liste = mutableListOf<MediaSession.QueueItem>()
		var i = 1L // Counter für die Queue
		// Albumdatenbankspalten
		val projection = Array(3, init = {_ -> ""})
		projection[0] = MediaStore.Audio.Albums._ID
		projection[1] = MediaStore.Audio.Albums.ALBUM_ART
		projection[2] = MediaStore.Audio.Albums.ALBUM
		val cursorAlbum = c.contentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, projection, null, null, null)
		// Titeldatenbankspalten
		projection[0] = MediaStore.Audio.Media._ID
		projection[1] = MediaStore.Audio.Media.TITLE
		projection[2] = MediaStore.Audio.Media.ARTIST
		var cover: String
		var albumid: String
		if(cursorAlbum.moveToFirst()){
			while(cursorAlbum.moveToNext()) { 	// für alle Alben die Titel abfragen und hinzufügen
				albumid = cursorAlbum.getString(cursorAlbum.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID))
				cover = cursorAlbum.getString(cursorAlbum.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ART))
				val selection = android.provider.MediaStore.Audio.Media.ALBUM_ID + "='" + albumid + "'"
				// Titeldatenbank abfragen
				val cursorTitle = this.c.contentResolver.query(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,selection,null,null)
				val list = MutableList(cursorTitle.count,
						{ _ -> MediaBrowser.MediaItem(MediaDescription.Builder().setMediaId(i.toString()).build(), MediaBrowser.MediaItem.FLAG_PLAYABLE) } )
				if(cursorTitle.moveToFirst()){
					// alle Titel des Albums auslesen
					val titelliste = getTitles(cursorTitle, list, cover)
					for(item in titelliste){
						// Überprüfen ob der bereits Spielende dabei ist, falls ja, nicht hinzufügen
						if(item.mediaId != playingTitleId){
							liste.add(MediaSession.QueueItem(item.description, i++))
						}
					}
				}
				cursorTitle.close()
			}
		}
		cursorAlbum.close()
		Log.d(TAG, "Anzahl QueueItems: ${i-1}")
		return liste
    }

    fun getTitles(c: Cursor, list: MutableList<MediaBrowser.MediaItem>, cover: String): MutableList<MediaBrowser.MediaItem>{
        var i = 0
        c.moveToFirst()
        do {
            val trackid = c.getString(c.getColumnIndex(MediaStore.Audio.Media._ID))
            val tracktitle = c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE))
            val interpret = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ARTIST))
            val album = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ALBUM))
            val description = MediaDescription.Builder()
                    .setMediaId(trackid)
                    .setDescription(album + "__" + cover)
                    .setTitle(tracktitle)
                    .setSubtitle(interpret)
            list[i] = MediaBrowser.MediaItem(description.build(), MediaBrowser.MediaItem.FLAG_PLAYABLE)
            i++
        } while(c.moveToNext())
        return list
    }
    fun getAlbums(c: Cursor, list: MutableList<MediaBrowser.MediaItem>): MutableList<MediaBrowser.MediaItem>{
        var i = 0
        c.moveToFirst()
        do {
            var albumid = c.getString(c.getColumnIndex(MediaStore.Audio.Media._ID))
            val albumtitle = c.getString(c.getColumnIndex(MediaStore.Audio.Albums.ALBUM))
            val albumtracks = c.getString(c.getColumnIndex(MediaStore.Audio.Albums.NUMBER_OF_SONGS))
            val albumartist = c.getString(c.getColumnIndex(MediaStore.Audio.Albums.ARTIST))
            val albumart = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ART))
            albumid = "ALBUM-$albumid"
            val description = MediaDescription.Builder()
                    .setMediaId(albumid)
                    .setTitle(albumtitle)
                    .setSubtitle(albumartist)
                    .setDescription(albumart + "__" + albumtracks)
            list[i] = MediaBrowser.MediaItem(description.build(), MediaBrowser.MediaItem.FLAG_BROWSABLE)
            i++
        } while(c.moveToNext())
        return list
    }
    fun getFileFromMediaId(mediaId: String?): String{
        var selection: String? = null
        if(mediaId != null){
            // eine spezifische ID abfragen, sonst den ersten Titel
            selection = MediaStore.Audio.Media._ID + "='" + mediaId + "'"
        }
        var result = "ERROR"
        val projection = Array(2, init = { _ -> "" } )
        projection[0] = MediaStore.Audio.Media._ID
        projection[1] = MediaStore.Audio.Media.DATA
        val cursor = c.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null, null)
        if(cursor != null){
            cursor.moveToFirst()
            result = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
        }
        cursor.close()
        return result
    }
    fun getMediaDescription(mediaId: String?, songnumber: String?): MediaMetadata?{
        val selection: String?
		//var datafile = ""
        if(mediaId != null){
            val result = MediaMetadata.Builder()
            // eine spezifische ID abfragen, sonst den ersten Titel
            selection = MediaStore.Audio.Media._ID + "='" + mediaId + "'"
            val projection = Array(7, init = { _ -> "" } )
            projection[0] = MediaStore.Audio.Media._ID
            projection[1] = MediaStore.Audio.Media.TITLE
            projection[2] = MediaStore.Audio.Media.ALBUM
            projection[3] = MediaStore.Audio.Media.ARTIST
            projection[4] = MediaStore.Audio.Media.DURATION
            projection[5] = MediaStore.Audio.Media.ALBUM_ID
            projection[6] = MediaStore.Audio.Media.DATA
            val cursor = c.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null, null)
            if(cursor != null){
                cursor.moveToFirst()
                result.putString(MediaMetadata.METADATA_KEY_MEDIA_ID, mediaId)
                result.putString(MediaMetadata.METADATA_KEY_TITLE,
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)))
                result.putString(MediaMetadata.METADATA_KEY_ARTIST,
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)))
                result.putString(MediaMetadata.METADATA_KEY_ALBUM,
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)))
                // Duration verlangt einen Long
                result.putLong(MediaMetadata.METADATA_KEY_DURATION,
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)))
                result.putString(MediaMetadata.METADATA_KEY_WRITER,
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)))
                // Liederanzahl
                if(!songnumber.isNullOrEmpty()){
                    val nr = songnumber!!.toLong()
                    result.putLong(MediaMetadata.METADATA_KEY_NUM_TRACKS, nr)
                }
                // Albumcover abfragen, um es in die Wiedergabe einzubinden
                val albumid = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))
                val projectionAlbum = Array(2, init = { _ -> "" } )
                projectionAlbum[0] = MediaStore.Audio.Albums._ID
                projectionAlbum[1] = MediaStore.Audio.Albums.ALBUM_ART
                val select = MediaStore.Audio.Albums._ID + "='" + albumid + "'"
                val calbums = c.contentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, projectionAlbum, select, null, null)
                if(calbums != null){
                    calbums.moveToFirst()
                    result.putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, calbums.getString(calbums.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART)))
                    val bitmap = BitmapFactory.decodeFile(calbums.getString(calbums.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART)))
                    result.putBitmap(MediaMetadata.METADATA_KEY_ART, bitmap)
                }
                calbums.close()
            }
            cursor.close()
            return result.build()
        }
        return null
    }

}