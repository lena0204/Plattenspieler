package com.lk.plattenspieler.observables

import android.media.session.PlaybackState
import android.util.Log
import com.lk.musicservicelibrary.main.MusicService
import com.lk.musicservicelibrary.models.MusicList
import com.lk.musicservicelibrary.models.MusicMetadata
import java.util.*

/**
 * Erstellt von Lena am 07.09.18.
 */
object PlaybackObservable: Observable() {

    private val TAG = this::class.java.simpleName

    private var playbackState = PlaybackState.Builder().build()
    private var metadata = MusicMetadata()
    private var queue = MusicList()

    fun setQueue(_queue: MusicList){
        queue = _queue
        setChanged()
        notifyObservers(queue)
    }

    fun getQueue(): MusicList{
        Log.v(TAG, "getqueue: Größe: " + queue.countItems())
        return queue
    }

    fun getQueueLimited30(): MusicList {
        var queue30 = MusicList()
        if(queue.countItems() > 30) {
            for(i in 0..29){
                queue30.addItem(queue.getItemAt(i))
            }
        } else {
            queue30 = queue
        }
        Log.v(TAG, "setqueue: Größe: " + queue30.countItems())
        return queue30
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

    fun setState(_state: PlaybackState){
        playbackState = _state
        logState("setState")
        setChanged()
        notifyObservers(playbackState)
    }

    fun getState(): PlaybackState{
        logState("getState")
        return playbackState
    }

    private fun logState(type: String) {
        Log.v(TAG, "$type: state (2 -> pause, 3 -> play, 10 -> next, 9 -> previous, 1 -> stop): "
                + playbackState.state)
        Log.v(TAG, "$type: shuffle on " + getShuffleOn())
    }

    fun getShuffleOn(): Boolean =
            playbackState.extras?.getBoolean(MusicService.SHUFFLE_KEY) ?: false
}