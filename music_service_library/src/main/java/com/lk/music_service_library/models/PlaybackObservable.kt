package com.lk.music_service_library.models

import android.util.Log
import java.util.*

/**
 * Erstellt von Lena am 12.05.18.
 * Verwaltet alle Daten, die an das Playback gekoppelt sind (Metadaten, Wiedergabeliste, Status)
 */
object PlaybackObservable: Observable() {

    private var currentQueue = MusicList()
    private var currentMetadata = MusicMetadata()
    private var currentState = MusicPlaybackState()
    private const val TAG = "PlaybackObservable"

    fun setQueue(_queue: MusicList){
        currentQueue = _queue
        setChanged()
        notifyObservers(getQueueLimitedTo30())
    }

    fun getQueueLimitedTo30(): MusicList{
        var shorterQueue = MusicList()
        if(currentQueue.countItems() > 30) {
            for(i in 0..30){
                shorterQueue.addItem(currentQueue.getItemAt(i))
            }
        } else {
            shorterQueue = currentQueue
        }
        Log.v(TAG, "getqueue: Größe: " + shorterQueue.countItems())
        return shorterQueue
    }

    fun getQueueAll(): MusicList{
        Log.v(TAG, "getqueue: Größe: " + currentQueue.countItems())
        return currentQueue
    }

    fun setMetadata(meta: MusicMetadata){
        currentMetadata = meta
        Log.v(TAG, "setmeta: Titel: " + currentMetadata.title)
        setChanged()
        notifyObservers(currentMetadata)
    }

    fun getMetadata(): MusicMetadata{
        Log.v(TAG, "getmeta: Titel: " + currentMetadata.title)
        return currentMetadata
    }

    fun setState(state: MusicPlaybackState){
        currentState = state
        logState()
        setChanged()
        notifyObservers(currentState)
    }
    fun getState(): MusicPlaybackState{
        logState()
        return currentState
    }

    private fun logState(){
        Log.v(TAG, "setstate: currentState (2 -> pause, 3 -> play, " +
                "10 -> next, 9 -> previous, 1 -> stop): " + currentState.state)
        Log.v(TAG, "setstate: shuffle on " + currentState.shuffleOn)
    }

}