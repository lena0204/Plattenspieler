package com.lk.musicservicelibrary.database

import androidx.lifecycle.LiveData
import androidx.room.*

/**
 * Erstellt von Lena am 02/04/2019.
 */
@Dao
interface PlaylistDAO {

    @Insert
    fun insertPlayingItem(playingItem: PlayingItemEntity)

    @Query("DELETE FROM playlist")
    fun deleteAll()

    @Query("SELECT * FROM playlist LIMIT 1")
    fun getFirstPlayingItem(): PlayingItemEntity

    @Query("SELECT * FROM playlist")
    fun getPlayingItems(): MutableList<PlayingItemEntity>

    @Query("SELECT COUNT(*) FROM playlist")
    fun hasItems(): LiveData<Int>

}