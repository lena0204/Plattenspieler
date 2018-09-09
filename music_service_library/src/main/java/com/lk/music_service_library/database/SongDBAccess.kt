package com.lk.music_service_library.database

import android.content.ContentResolver
import android.content.ContentValues
import android.util.Log
import com.lk.music_service_library.models.MusicList
import com.lk.music_service_library.models.MusicMetadata

/**
 * Erstellt von Lena am 11.05.18.
 * Zugriff auf die appeigene Datenbank, die die aktuelle Wiedergabeliste speichert
 */
object SongDBAccess{

    private var TAG = "SongDBAccess"
    private lateinit var contentResolver: ContentResolver

    fun savePlayingQueue(cr: ContentResolver, playingQueue: MusicList, metadata: MusicMetadata){
        Log.d(TAG, "savePlayingQueue(): Evtl Warteschlange abspeichern")
        contentResolver = cr
        // wenn die Wartschlange etwas enthält, muss es auch aktuelle Metadaten geben und nur wenn
        // nicht abgespielt wird -> sonst Sicherung über die Session
        contentResolver.delete(SongContentProvider.CONTENT_URI, null, null)
        if(playingQueue.countItems() > 0){
            var values = ContentValues()
            // aktuelle Metadaten sichern
            // IDEA_ Wiedergabestelle speichern (Position und passend wiederherstellen)
            values.put(SongDB.COLUMN_ID, metadata.id)
            values.put(SongDB.COLUMN_TITLE, metadata.title)
            values.put(SongDB.COLUMN_ARTIST, metadata.artist)
            values.put(SongDB.COLUMN_ALBUM, metadata.album)
            values.put(SongDB.COLUMN_COVER_URI, metadata.cover_uri)
            values.put(SongDB.COLUMN_DURATION, metadata.duration.toString())
            values.put(SongDB.COLUMN_NUMTRACKS, metadata.nr_of_songs_left.toString())
            values.put(SongDB.COLUMN_FILE, metadata.path)
            contentResolver.insert(SongContentProvider.CONTENT_URI, values)
            // Warteschlange sichern
            Log.d(TAG, "Saving: Länge der Schlange: " + playingQueue.countItems())
            for(item in playingQueue) {
                values = ContentValues()
                values.put(SongDB.COLUMN_ID, item.id)
                values.put(SongDB.COLUMN_TITLE, item.title)
                values.put(SongDB.COLUMN_ARTIST, item.artist)
                values.put(SongDB.COLUMN_ALBUM, item.album)
                values.put(SongDB.COLUMN_COVER_URI, item.cover_uri)
                values.put(SongDB.COLUMN_DURATION, "")
                values.put(SongDB.COLUMN_NUMTRACKS, "")
                values.put(SongDB.COLUMN_FILE, "")
                contentResolver.insert(SongContentProvider.CONTENT_URI, values)
            }
        }
    }

    fun restoreFirstQueueItem(cr: ContentResolver): MusicMetadata?{
        contentResolver = cr
        val projection = getProjection()
        val orderby = SongDB.COLUMN_ID + " LIMIT 1"
        val c = contentResolver.query(SongContentProvider.CONTENT_URI, projection, null, null, orderby)
        if(c != null && c.count != 0) {
            Log.d(TAG, c.count.toString() + " Zeilen bei restoring")
            c.moveToFirst()
            // ersten Datensatz in die aktuelle Wiedergabe schreiben und an den Service weitergeben
            // falls die Spalten "" leer sind
            var dur = c.getString(c.getColumnIndex(SongDB.COLUMN_DURATION)).toLongOrNull()
            if(dur == null)  dur = 0L
            var songnr = c.getString(c.getColumnIndex(SongDB.COLUMN_NUMTRACKS)).toLongOrNull()
            if(songnr == null) songnr = 0

            val music = MusicMetadata(
                    c.getString(c.getColumnIndex(SongDB.COLUMN_ID)),
                    c.getString(c.getColumnIndex(SongDB.COLUMN_ALBUM)),
                    c.getString(c.getColumnIndex(SongDB.COLUMN_ARTIST)),
                    c.getString(c.getColumnIndex(SongDB.COLUMN_TITLE)),
                    c.getString(c.getColumnIndex(SongDB.COLUMN_COVER_URI)),
                    duration = dur,
                    nr_of_songs_left = songnr
            )
            c.close()
            return music
        }
        c?.close()
        return null
    }

    fun restorePlayingQueue(contentResolver: ContentResolver): MusicList?{
        val playingQueue = MusicList()
        Log.d(TAG, "Restoring")
        val projection = getProjection()
        val c = contentResolver.query(SongContentProvider.CONTENT_URI, projection, null, null, null)
        if(c != null && c.moveToFirst()) {
            Log.d(TAG, c.count.toString() + " Zeilen bei restoring")
            c.moveToNext()
            // Metadata zusammenstellen und an den Service weitergeben
            while (!c.isAfterLast) {
                val item = MusicMetadata(
                        c.getString(c.getColumnIndex(SongDB.COLUMN_ID)),
                        c.getString(c.getColumnIndex(SongDB.COLUMN_ALBUM)),
                        c.getString(c.getColumnIndex(SongDB.COLUMN_ARTIST)),
                        c.getString(c.getColumnIndex(SongDB.COLUMN_TITLE)),
                        c.getString(c.getColumnIndex(SongDB.COLUMN_COVER_URI))
                )
                playingQueue.addItem(item)
                c.moveToNext()
            }
            c.close()
            return playingQueue
        }
        c?.close()
        return null
    }

    private fun getProjection(): Array<String>{
            val p = Array(7) { _ -> "" }
            p[0] = SongDB.COLUMN_ID
            p[1] = SongDB.COLUMN_TITLE
            p[2] = SongDB.COLUMN_ARTIST
            p[3] = SongDB.COLUMN_ALBUM
            p[4] = SongDB.COLUMN_COVER_URI
            p[5] = SongDB.COLUMN_DURATION
            p[6] = SongDB.COLUMN_NUMTRACKS
            return p
        }

}