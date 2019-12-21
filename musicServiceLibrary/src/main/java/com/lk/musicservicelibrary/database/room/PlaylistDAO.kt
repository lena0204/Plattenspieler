package com.lk.musicservicelibrary.database.room

import androidx.lifecycle.LiveData
import androidx.room.*

/**
 * Erstellt von Lena am 02/04/2019.
 */
@Dao
interface PlaylistDAO {

    @Insert
    suspend fun insert(track: TrackEntity): Long

    @Query("DELETE FROM playlist")
    suspend fun deleteAll()

    @Query("SELECT * FROM playlist")
    fun selectAll(): LiveData<List<TrackEntity>>

}