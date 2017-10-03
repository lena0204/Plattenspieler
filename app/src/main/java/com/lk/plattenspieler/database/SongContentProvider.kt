package com.lk.plattenspieler.database

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.text.TextUtils
import java.util.*

/**
 * Created by Lena on 19.08.17.
 */
class SongContentProvider: ContentProvider() {

    companion object {
        private val AUTHORITY = "com.lk.plattenspieler.contentprovider"
        private val BASE_PATH = "songs"
        val CONTENT_URI = Uri.parse("content://$AUTHORITY/$BASE_PATH")
        val CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/song"
        val SONGS = 10
        val SONG_ID = 20
        val ALL_URIS = Uri.parse("content://$AUTHORITY/$BASE_PATH/$SONGS")
    }

    // Zugriff auf die Datenbank
    lateinit private var database: SongDB

    // Werden für Uri-Matcher gebraucht
    private val sURIMatcher = UriMatcher(UriMatcher.NO_MATCH)

    override fun onCreate(): Boolean {
        sURIMatcher.addURI(AUTHORITY, BASE_PATH, SONGS)
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", SONG_ID)
        database = SongDB(context)
        return false
    }
    override fun getType(uri: Uri?): String {
        return ""
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        val queryBuilder = SQLiteQueryBuilder()
        checkColumns(projection)
        // Tabelle auswählen
        queryBuilder.tables = SongDB.TABLE_SONGS
        // alle ansprechen oder nur eine bestimmte Reihe
        val uriType = sURIMatcher.match(uri)
        when (uriType) {
            SONGS -> {}
            SONG_ID -> queryBuilder.appendWhere(SongDB.COLUMN_ID + "=" + uri.lastPathSegment)
            else -> throw IllegalArgumentException("Unknown URI: " + uri)
        }
        val db = database.writableDatabase
        val cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder)
        // absichern, dass mögliche Listener aufmerksam gemacht werden
        cursor.setNotificationUri(context!!.contentResolver, uri)
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        val uriType = sURIMatcher.match(uri)
        val sqlDB = database.writableDatabase
        val id: Long
        // neue Daten eintragen falls nötig und die URI bekannt
        when (uriType) {
            SONGS -> id = sqlDB.insert(SongDB.TABLE_SONGS, null, values)
            else -> throw IllegalArgumentException("Unknown URI: " + uri)
        }
        context!!.contentResolver.notifyChange(uri, null)
        return Uri.parse(BASE_PATH + "/" + id)
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        val uriType = sURIMatcher.match(uri)
        val sqlDB = database.writableDatabase
        val rowsUpdated: Int
        //  Daten updaten falls gewollt und die URI bekannt
        when (uriType) {
            SONGS -> rowsUpdated = sqlDB.update(SongDB.TABLE_SONGS, values, selection, selectionArgs)
            SONG_ID -> {
                val id = uri.lastPathSegment
                if (TextUtils.isEmpty(selection)) { // falls nichts für selection übergeben wird
                    rowsUpdated = sqlDB.update(SongDB.TABLE_SONGS, values, SongDB.COLUMN_ID + "=" + id, null)
                } else {
                    rowsUpdated = sqlDB.update(SongDB.TABLE_SONGS, values, SongDB.COLUMN_ID + "=" + id
                            + "and" + selection, selectionArgs)
                }
            }
            else -> throw IllegalArgumentException("Unknown URI: " + uri)
        }
        context!!.contentResolver.notifyChange(uri, null)
        return rowsUpdated
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val uriType = sURIMatcher.match(uri)
        val sqlDB = database.writableDatabase
        val rowsDeleted: Int
        //  Daten löschen falls gewollt und die URI bekannt
        when (uriType) {
            SONGS -> rowsDeleted = sqlDB.delete(SongDB.TABLE_SONGS, selection, selectionArgs)
            SONG_ID -> {
                val id = uri.lastPathSegment
                if (TextUtils.isEmpty(selection)) { // falls nichts für selection übergeben wird
                    rowsDeleted = sqlDB.delete(SongDB.TABLE_SONGS, SongDB.COLUMN_ID + "=" + id, null)
                } else {
                    rowsDeleted = sqlDB.delete(SongDB.TABLE_SONGS, SongDB.COLUMN_ID + "=" + id
                            + "and" + selection, selectionArgs)
                }
            }
            else -> throw IllegalArgumentException("Unknown URI: " + uri)
        }
        context!!.contentResolver.notifyChange(uri, null)
        return rowsDeleted
    }

    private fun checkColumns(projection: Array<out String>?) {
        val available = arrayOf<String>(SongDB.COLUMN_ID, SongDB.COLUMN_TITLE, SongDB.COLUMN_ARTIST,
                SongDB.COLUMN_ALBUM, SongDB.COLUMN_COVER_URI, SongDB.COLUMN_DURATION, SongDB.COLUMN_NUMTRACKS)
        if (projection != null) {
            val requestedColumns = HashSet(Arrays.asList(*projection))
            val availableColumns = HashSet(Arrays.asList(*available))
            // abfragen, ob alle angefragten Spalten auch vorhanden sind
            if (!availableColumns.containsAll(requestedColumns)) {
                throw IllegalArgumentException("Unknown columns in projection")
            }
        }
    } // Methode checkColumns


}