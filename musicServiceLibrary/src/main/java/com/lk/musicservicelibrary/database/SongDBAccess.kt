package com.lk.musicservicelibrary.database

import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import androidx.core.content.contentValuesOf
import com.lk.musicservicelibrary.models.MusicList
import com.lk.musicservicelibrary.models.MusicMetadata

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
        contentResolver.delete(SongContentProvider.CONTENT_URI, null, null)
        if(playingQueue.countItems() > 0){
            saveCurrentMetadata(metadata)
            saveQueue(playingQueue)
        }
    }
    
    private fun saveCurrentMetadata(metadata: MusicMetadata) {
        // IDEA_ Wiedergabestelle speichern (Position und passend wiederherstellen)
        val values = contentValuesOf(
                SongDB.COLUMN_ID to metadata.id,
                SongDB.COLUMN_TITLE to metadata.title,
                SongDB.COLUMN_ARTIST to  metadata.artist,
                SongDB.COLUMN_ALBUM to metadata.album,
                SongDB.COLUMN_COVER_URI to metadata.cover_uri,
                SongDB.COLUMN_DURATION to metadata.duration.toString(),
                SongDB.COLUMN_NUMTRACKS to metadata.nr_of_songs_left.toString(),
                SongDB.COLUMN_FILE to metadata.path)
        contentResolver.insert(SongContentProvider.CONTENT_URI, values)
    }
    
    private fun saveQueue(playingQueue: MusicList) {
        var values: ContentValues
        Log.d(TAG, "Saving: Länge der Schlange: " + playingQueue.countItems())
        for(item in playingQueue) {
            values = contentValuesOf(
                    SongDB.COLUMN_ID to item.id,
                    SongDB.COLUMN_TITLE to item.title,
                    SongDB.COLUMN_ARTIST to item.artist,
                    SongDB.COLUMN_ALBUM to item.album,
                    SongDB.COLUMN_COVER_URI to item.cover_uri,
                    SongDB.COLUMN_DURATION to "",
                    SongDB.COLUMN_NUMTRACKS to "",
                    SongDB.COLUMN_FILE to "")
            contentResolver.insert(SongContentProvider.CONTENT_URI, values)
        }
    }

    fun restoreFirstQueueItem(cr: ContentResolver): MusicMetadata?{
        contentResolver = cr
        val projection = getProjection()
        val orderby = SongDB.COLUMN_ID + " LIMIT 1"
        val c = contentResolver.query(SongContentProvider.CONTENT_URI, projection, null, null, orderby)
        if(c != null && c.count != 0) {
            Log.d(TAG, c.count.toString() + " Zeilen bei restoring first")
            c.moveToFirst()
            val dur = c.fetchString(SongDB.COLUMN_DURATION).toLongOrNull() ?: 0L
            val songnr = c.fetchString(SongDB.COLUMN_NUMTRACKS).toLongOrNull() ?: 0

            val music = MusicMetadata(
                    c.fetchString(SongDB.COLUMN_ID),
                    c.fetchString(SongDB.COLUMN_ALBUM),
                    c.fetchString(SongDB.COLUMN_ARTIST),
                    c.fetchString(SongDB.COLUMN_TITLE),
                    c.fetchString(SongDB.COLUMN_COVER_URI),
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
        val projection = getProjection()
        val c = contentResolver.query(SongContentProvider.CONTENT_URI, projection, null, null, null)
        if(c != null && c.moveToFirst()) {
            Log.d(TAG, c.count.toString() + " Zeilen bei restoring")
            c.moveToNext()
            while (!c.isAfterLast) {
                val item = MusicMetadata(
                        c.fetchString(SongDB.COLUMN_ID),
                        c.fetchString(SongDB.COLUMN_ALBUM),
                        c.fetchString(SongDB.COLUMN_ARTIST),
                        c.fetchString(SongDB.COLUMN_TITLE),
                        c.fetchString(SongDB.COLUMN_COVER_URI)
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
    
    private fun Cursor.fetchString(column: String): String =
            this.getString(this.getColumnIndex(column))
        
    

}