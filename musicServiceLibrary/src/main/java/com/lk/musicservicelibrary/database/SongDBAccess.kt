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
class SongDBAccess (private var contentResolver: ContentResolver): PlaylistRepository {

    // IDEA_ daraus eine normale Klasse machen, contentResolver als Konstruktorparameter, eh Änderung wenn Room
    private var TAG = "SongDBAccess"

    override fun savePlayingQueue(playingQueue: MusicList, playingMetadata: MusicMetadata){
        Log.d(TAG, "savePlayingQueue(): Evtl. Warteschlange abspeichern")
        contentResolver.delete(SongContentProvider.CONTENT_URI, null, null)
        if(playingQueue.size() > 0){
            saveCurrentMetadata(playingMetadata)
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
        Log.d(TAG, "Saving: Länge der Schlange: " + playingQueue.size())
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

    override fun restoreFirstItem(): MusicMetadata? {
        val projection = getProjection()
        val orderBy = SongDB.COLUMN_ID + " LIMIT 1"
        val cursor = contentResolver.query(
            SongContentProvider.CONTENT_URI, projection, null, null, orderBy)

        if(cursor != null && cursor.count != 0) {
            cursor.moveToFirst()
            val music = createMetadataFromCursor(cursor)
            cursor.close()
            return music
        }
        cursor?.close()
        return null
    }

    private fun createMetadataFromCursor(cursor: Cursor): MusicMetadata {
        val duration = cursor.fetchString(SongDB.COLUMN_DURATION).toLongOrNull() ?: 0L
        val songsLeft = cursor.fetchString(SongDB.COLUMN_NUMTRACKS).toLongOrNull() ?: 0

        return MusicMetadata(
            cursor.fetchString(SongDB.COLUMN_ID),
            cursor.fetchString(SongDB.COLUMN_ALBUM),
            cursor.fetchString(SongDB.COLUMN_ARTIST),
            cursor.fetchString(SongDB.COLUMN_TITLE),
            cursor.fetchString(SongDB.COLUMN_COVER_URI),
            duration = duration,
            nr_of_songs_left = songsLeft
        )
    }

    override fun restorePlayingQueue(): MusicList? {
        val playingQueue = MusicList()
        val projection = getProjection()
        val cursor = contentResolver.query(
            SongContentProvider.CONTENT_URI, projection, null, null, null)
        if(cursor != null && cursor.moveToFirst()) {
            Log.d(TAG, cursor.count.toString() + " Zeilen bei restoring")

            cursor.moveToNext()
            while (!cursor.isAfterLast) {
                val item = createMetadataFromCursor(cursor)
                playingQueue.addItem(item)
                cursor.moveToNext()
            }
            cursor.close()
            return playingQueue
        }
        cursor?.close()
        return null
    }

    private fun getProjection(): Array<String>{
            val projection = Array(7) { "" }
            projection[0] = SongDB.COLUMN_ID
            projection[1] = SongDB.COLUMN_TITLE
            projection[2] = SongDB.COLUMN_ARTIST
            projection[3] = SongDB.COLUMN_ALBUM
            projection[4] = SongDB.COLUMN_COVER_URI
            projection[5] = SongDB.COLUMN_DURATION
            projection[6] = SongDB.COLUMN_NUMTRACKS
            return projection
    }
    
    private fun Cursor.fetchString(column: String): String =
            this.getString(this.getColumnIndex(column))

}