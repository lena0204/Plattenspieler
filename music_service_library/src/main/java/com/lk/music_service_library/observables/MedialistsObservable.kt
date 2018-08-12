package com.lk.music_service_library.observables

import android.util.Log
import com.lk.music_service_library.models.MusicList
import java.util.*

/**
 * Erstellt von Lena am 12.05.18.
 * Verwaltet die Liste von Alben und die Liste von Titeln
 */
object MedialistsObservable: Observable() {

    private var medialist = MusicList()
    private var albumlist = MusicList()
    private var TAG = "MediaListObservable"

    fun setTitleList(_list: MusicList){
        medialist = _list
        Log.d(TAG, "set: Größe: " + medialist.countItems())
        setChanged()
        notifyObservers(medialist)
    }
    fun setAlbumList(_list: MusicList){
        albumlist = _list
        setChanged()
        notifyObservers(albumlist)
    }
    fun getTitleList(): MusicList {
        Log.d(TAG, "get: Größe: " + medialist.countItems())
        return medialist
    }
    fun getAlbumList(): MusicList {
        Log.d(TAG, "get: Größe: " + albumlist.countItems())
        return albumlist
    }

}