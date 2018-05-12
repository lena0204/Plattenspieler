package com.lk.plattenspieler.database

import android.content.ContentResolver
import android.content.ContentValues
import android.media.session.MediaController
import android.os.Bundle
import android.util.Log
import com.lk.plattenspieler.models.MusicList
import com.lk.plattenspieler.models.MusicMetadata

/**
 * Erstellt von Lena am 11.05.18.
 */
object SongDBAccess{

    private var TAG = "SongDBAccess"

    fun savePlayingQueue(contentResolver: ContentResolver, playingQueue: MusicList, metadata: MusicMetadata){
        Log.d(TAG, "savePlayingQueue(): Evtl Warteschlange abspeichern")
        // wenn die Wartschlange etwas enthält, muss es auch aktuelle Metadaten geben und nur wenn
        // nicht abgespielt wird -> sonst Sicherung über die Session
        contentResolver.delete(SongContentProvider.CONTENT_URI, null, null)
        if(playingQueue.countItems() > 0){
            // aktuelle Metadaten sichern
            var values = ContentValues()
            // IDEA_ Wiedergabestelle speichern (Position und passend wiederherstellen)
            values.put(SongDB.COLUMN_ID, metadata.id)
            values.put(SongDB.COLUMN_TITLE, metadata.title)
            values.put(SongDB.COLUMN_ARTIST, metadata.artist)
            values.put(SongDB.COLUMN_ALBUM, metadata.album)
            values.put(SongDB.COLUMN_COVER_URI, metadata.cover_uri)
            values.put(SongDB.COLUMN_DURATION, metadata.duration.toString())
            values.put(SongDB.COLUMN_NUMTRACKS, metadata.songnr.toString())
            values.put(SongDB.COLUMN_FILE, metadata.path)
            contentResolver.insert(SongContentProvider.CONTENT_URI, values)
            // Warteschlange sichern
            Log.d(TAG, "Saving: Länge der Schlange: " + playingQueue.countItems())
            //  TESTING_ -- Manchmal Error: Unique constraint failed (_id ist nicht eindeutig(Primärschlüssel));
            //  wenn er versucht eine Queue erneut einzufügen, obwohl die zur aktuellen wiedergabe gehört
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

    fun restorePlayingQueue(contentResolver: ContentResolver, mc: MediaController): MusicList?{
        val playingQueue = MusicList()
        Log.d(TAG, "Restoring")
        val projection = getProjection()
        val c = contentResolver.query(SongContentProvider.CONTENT_URI, projection, null, null, null)
        if(c != null && c.count != 0) {
            Log.d(TAG, c.count.toString() + " Zeilen bei restoring")
            c.moveToFirst()
            // ersten Datensatz in die aktuelle Wiedergabe schreiben und an den Service weitergeben
            val music = MusicMetadata(
                    c.getString(c.getColumnIndex(SongDB.COLUMN_ID)),
                    c.getString(c.getColumnIndex(SongDB.COLUMN_ALBUM)),
                    c.getString(c.getColumnIndex(SongDB.COLUMN_ARTIST)),
                    c.getString(c.getColumnIndex(SongDB.COLUMN_TITLE)),
                    c.getString(c.getColumnIndex(SongDB.COLUMN_COVER_URI)),
                    duration = c.getString(c.getColumnIndex(SongDB.COLUMN_DURATION)).toLong(),
                    songnr = c.getString(c.getColumnIndex(SongDB.COLUMN_NUMTRACKS)).toLong()
            )
            mc.transportControls.playFromMediaId(music.id, Bundle())
            playingQueue.addItem(music)
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
                val args = Bundle()
                args.putParcelable("S", item)
                mc.sendCommand("add", args, null)
                c.moveToNext()
            }
            c.close()
            return playingQueue
        }
        c.close()
        return null
    }

    private fun getProjection(): Array<String>{
            val p = Array(7, { _ -> "" })
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