package com.lk.musicservicelibrary.utils

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.lk.musicservicelibrary.main.MusicService

/**
 * Erstellt von Lena am 2019-11-19.
 * To simplify access to proporties saved in shared preferences like shuffle and
 * preventing to permantly give them as parameters
 */
object SharedPrefsWrapper {

    fun writeShuffle(context: Context, shuffle: Boolean) {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPrefs.edit {
            putBoolean(MusicService.SHUFFLE_KEY, shuffle)
        }
    }

    fun readShuffle(context: Context): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return sharedPreferences.getBoolean(MusicService.SHUFFLE_KEY, false)
    }

}