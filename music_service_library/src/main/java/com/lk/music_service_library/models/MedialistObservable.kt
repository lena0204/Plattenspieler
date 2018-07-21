package com.lk.music_service_library.models

import android.util.Log
import java.util.*

/**
 * Erstellt von Lena am 12.05.18.
 * Verwaltet die Liste von Alben und die Liste von Titeln
 */
object MedialistObservable: Observable() {

    private var medialist = MusicList()
    private var albumlist = MusicList()
    private var TAG = "MediaListObservable"

    fun setMediaList(_list: MusicList){
        medialist = _list
        Log.d(TAG, "set: Größe: " + medialist.countItems())
        setChanged()
        notifyObservers(medialist)
    }
    fun setAlbums(_list: MusicList){
        albumlist = _list
        setChanged()
        notifyObservers(albumlist)
    }
    fun getMediaList(): MusicList{
        Log.d(TAG, "get: Größe: " + medialist.countItems())
        return medialist
    }
    fun getAlbums(): MusicList{
        Log.d(TAG, "get: Größe: " + albumlist.countItems())
        return albumlist
    }

}