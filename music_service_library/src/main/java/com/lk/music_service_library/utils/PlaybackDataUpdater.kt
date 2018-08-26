package com.lk.music_service_library.utils

import android.media.session.PlaybackState
import android.os.Bundle
import android.util.Log
import com.lk.music_service_library.models.MusicMetadata
import com.lk.music_service_library.observables.PlaybackDataObservable

/**
 * Erstellt von Lena am 15.08.18.
 */
object PlaybackDataUpdater {

    private const val TAG = "PlaybackDataUpdater"
    private var stateBuilder = PlaybackState.Builder()

    fun updateMetadata(newMetadata: MusicMetadata): MusicMetadata{
        newMetadata.nr_of_songs_left = getRemainingSongNumber()
        return newMetadata
    }

    // Long, weil MediaMetadata einen Long erfordert
    private fun getRemainingSongNumber(): Long {
        var remainingSongs = 0L
        if(!PlaybackDataObservable.musicQueue.isEmpty()){
            remainingSongs = PlaybackDataObservable.musicQueue.countItems().toLong()
        }
        return remainingSongs
    }

    fun updatePlaybackstate(state: Int): PlaybackState{
        stateBuilder = PlaybackState.Builder()
        Log.d(TAG, state.toString())
        setPlaybackActions(state)
        setShuffleBuilderExtra()
        stateBuilder.setState(state, PlaybackDataObservable.positionMs, 1.0f)
        return stateBuilder.build()
    }

    private fun setPlaybackActions(state: Int){
        when(state) {
            PlaybackState.STATE_PLAYING -> setPlayingActions()
            PlaybackState.STATE_PAUSED -> setPausedActions()
            PlaybackState.STATE_STOPPED -> setStoppedActions()
        }
    }

    private fun setPlayingActions(){
        stateBuilder.setActions(PlaybackState.ACTION_PAUSE
                or PlaybackState.ACTION_STOP
                or PlaybackState.ACTION_SKIP_TO_NEXT
                or PlaybackState.ACTION_SKIP_TO_PREVIOUS)
    }

    private fun setPausedActions(){
        stateBuilder.setActions(PlaybackState.ACTION_PLAY
                or PlaybackState.ACTION_PLAY_FROM_MEDIA_ID
                or PlaybackState.ACTION_SKIP_TO_NEXT
                or PlaybackState.ACTION_SKIP_TO_PREVIOUS
                or PlaybackState.ACTION_STOP)
    }

    private fun setStoppedActions(){
        stateBuilder.setActions(PlaybackState.ACTION_PLAY
                or PlaybackState.ACTION_PLAY_FROM_MEDIA_ID)
    }

    private fun setShuffleBuilderExtra() {
        val extras = Bundle()
        Log.v(TAG, "UpdatePlaybackstate: shuffleOn is ${PlaybackDataObservable.shuffleOn}")
        extras.putBoolean("shuffle", PlaybackDataObservable.shuffleOn)
        stateBuilder.setExtras(extras)
    }
}