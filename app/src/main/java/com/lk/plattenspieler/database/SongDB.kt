package com.lk.plattenspieler.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

/**
 * Created by Lena on 19.08.17.
 * Repräsentiert die Datenbank mit den entsprechenden Spalten
 */
class SongDB(context: Context): SQLiteOpenHelper(context, "songs.db", null, 1) {

    private val DATABASE_NAME = "songs.db"
    private val DATABASE_VERSION = 2

    companion object Spalten {
        val TABLE_SONGS = "songs"
        val COLUMN_ID = "_id"
        val COLUMN_TITLE = "description"
        val COLUMN_ALBUM = "album"
        val COLUMN_ARTIST = "artist"
        val COLUMN_COVER_URI = "cover_uri"
        val COLUMN_NUMTRACKS = "num_tracks"
        val COLUMN_DURATION = "duration"
        val COLUMN_FILE = "file_path"
    }

    // Datenbank und Tabelle erstellen
    val DATABASE_CREATE = "create table " + TABLE_SONGS +
            " (" + COLUMN_ID + " text primary key, " +
            COLUMN_TITLE + " text not null, " +
            COLUMN_ALBUM + " text not null, " +
            COLUMN_ARTIST + " text not null, " +
            COLUMN_COVER_URI + " text not null, " +
            COLUMN_NUMTRACKS + " text not null, " +
            COLUMN_DURATION + " text not null, " +
            COLUMN_FILE + " text not null " +
            ");"

    override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
        sqLiteDatabase.execSQL(DATABASE_CREATE)
    }

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, iOld: Int, iNew: Int) {
        Log.w(SongDB::class.java.name, "Upgrading database from version " + iOld
                + " to " + iNew + " which will destroy all old data.")
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS $TABLE_SONGS")
        onCreate(sqLiteDatabase)
    }

}