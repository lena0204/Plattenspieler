package com.lk.musicservicelibrary.database

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.text.TextUtils
import java.util.*

/**
 * Created by Lena on 19.08.17.
 * ContentProvider (einfügen, löschen, updaten) für die interne Datenbank
 */
class SongContentProvider: ContentProvider() {

    companion object {
        private const val AUTHORITY = "com.lk.plattenspieler.contentprovider"
        private const val BASE_PATH = "songs"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$BASE_PATH")
        const val SONGS = 10
        const val SONG_ID = 20
    }

    private lateinit var database: SongDB

    private val sURIMatcher = UriMatcher(UriMatcher.NO_MATCH)

    override fun onCreate(): Boolean {
        sURIMatcher.addURI(AUTHORITY, BASE_PATH, SONGS)
        sURIMatcher.addURI(AUTHORITY, "$BASE_PATH/#", SONG_ID)
        database = SongDB(context!!)  // TESTING_ Lösung für context? nötig, aktuell context!! aber nicht ideal
        return true
    }

    override fun getType(uri: Uri?): String = ""

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        var queryBuilder = SQLiteQueryBuilder()
        checkColumns(projection)
        queryBuilder.tables = SongDB.TABLE_SONGS
        queryBuilder = addWhereClauseIfNeeded(uri, queryBuilder)
        val db = database.writableDatabase
        val cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder)
        cursor.setNotificationUri(context!!.contentResolver, uri)
        return cursor
    }

    private fun addWhereClauseIfNeeded(uri: Uri, queryBuilder: SQLiteQueryBuilder): SQLiteQueryBuilder{
        val uriType = sURIMatcher.match(uri)
        when (uriType) {
            SONGS -> {}
            SONG_ID -> queryBuilder.appendWhere(SongDB.COLUMN_ID + "=" + uri.lastPathSegment)
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
        return queryBuilder
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        val uriType = sURIMatcher.match(uri)
        val sqlDB = database.writableDatabase
        val id: Long
        when (uriType) {
            SONGS -> id = sqlDB.insert(SongDB.TABLE_SONGS, null, values)
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
        context!!.contentResolver.notifyChange(uri, null)
        return Uri.parse("$BASE_PATH/$id")
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        val uriType = sURIMatcher.match(uri)
        val sqlDB = database.writableDatabase
        val rowsUpdated = when (uriType) {
            SONGS -> sqlDB.update(SongDB.TABLE_SONGS, values, selection, selectionArgs)
            SONG_ID -> {
                val id = uri.lastPathSegment
                if (TextUtils.isEmpty(selection)) {
                    sqlDB.update(SongDB.TABLE_SONGS, values, SongDB.COLUMN_ID + "=" + id, null)
                } else {
                    sqlDB.update(SongDB.TABLE_SONGS, values, SongDB.COLUMN_ID + "=" + id
                            + "and" + selection, selectionArgs)
                }
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
        context!!.contentResolver.notifyChange(uri, null)
        return rowsUpdated
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val uriType = sURIMatcher.match(uri)
        val sqlDB = database.writableDatabase
        val rowsDeleted = when (uriType) {
            SONGS -> sqlDB.delete(SongDB.TABLE_SONGS, selection, selectionArgs)
            SONG_ID -> {
                val id = uri.lastPathSegment
                if (TextUtils.isEmpty(selection)) {
                    sqlDB.delete(SongDB.TABLE_SONGS, SongDB.COLUMN_ID + "=" + id, null)
                } else {
                    sqlDB.delete(SongDB.TABLE_SONGS, SongDB.COLUMN_ID + "=" + id
                            + "and" + selection, selectionArgs)
                }
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
        context!!.contentResolver.notifyChange(uri, null)
        return rowsDeleted
    }

    private fun checkColumns(projection: Array<out String>?) {
        val available = arrayOf(SongDB.COLUMN_ID, SongDB.COLUMN_TITLE, SongDB.COLUMN_ARTIST,
                SongDB.COLUMN_ALBUM, SongDB.COLUMN_COVER_URI, SongDB.COLUMN_DURATION, SongDB.COLUMN_NUMTRACKS)
        if (projection != null) {
            val requestedColumns = HashSet(Arrays.asList(*projection))
            val availableColumns = HashSet(Arrays.asList(*available))
            if (!availableColumns.containsAll(requestedColumns)) {
                throw IllegalArgumentException("Unknown columns in projection")
            }
        }
    }


}