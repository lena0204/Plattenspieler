package com.lk.plattenspieler.models

import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import java.util.*

/**
 * Erstellt von Lena am 12.05.18.
 */
object PlaybackObservable: Observable() {

    private var queue = MusicList()
    private var metadata = MusicMetadata()
    private var state = MusicPlaybackState()
    private val TAG = "PlaybackObservable"

    fun setQueue(_queue: MusicList){
        // queue auf 30 items begrenzen
        if(_queue.countItems() > 30) {
            var i = 0
            while (i < 30){
                queue.addItem(_queue.getItemAt(i))
                i++
            }
        } else {
            queue = _queue
        }
        Log.v(TAG, "setqueue: Größe: " + queue.countItems())
        setChanged()
        notifyObservers(queue)
    }
    fun getQueue(): MusicList{
        Log.v(TAG, "getqueue: Größe: " + queue.countItems())
        return queue
    }

    fun setMetadata(_meta: MusicMetadata){
        metadata = _meta
        Log.v(TAG, "setmeta: Titel: " + metadata.title)
        setChanged()
        notifyObservers(metadata)
    }
    fun getMetadata(): MusicMetadata{
        Log.v(TAG, "getmeta: Titel: " + metadata.title)
        return metadata
    }

    fun setState(_state: MusicPlaybackState){
        state = _state
        Log.v(TAG, "setstate: state (2 -> pause, 3 -> play, 10 -> next, 9 -> previous, 1 -> stop): " + state.state)
        Log.v(TAG, "setstate: shuffle on " + state.shuffleOn)
        setChanged()
        notifyObservers(state)
    }
    fun getState(): MusicPlaybackState{
        Log.v(TAG, "getstate: state (2 -> pause, 3 -> play, 10 -> next, 9 -> previous, 1 -> stop): " + state.state)
        Log.v(TAG, "getstate: shuffle on " + state.shuffleOn)
        return state
    }

}