package com.lk.musicservicelibrary.utils

import android.media.session.PlaybackState
import android.util.Log
import androidx.core.os.bundleOf
import com.lk.musicservicelibrary.main.MusicService
import com.lk.musicservicelibrary.playback.state.States

/**
 * Erstellt von Lena am 06/04/2019.
 */
object PlaybackStateFactory {

    private val TAG = "PlaybackStateFactory"

    fun createState(state: States, position: Long = 0L, shuffle: Boolean = false): PlaybackState{
        return when(state) {
            States.PLAYING -> createStateForPlaying(position, shuffle)
            States.PAUSED -> createStateForPaused(position, shuffle)
            States.STOPPED -> createStateForStopped()
        }
    }

    private fun createStateForPlaying(position: Long, shuffle: Boolean): PlaybackState {
        val builder = PlaybackState.Builder()
        builder.setActions(PlaybackState.ACTION_PAUSE
                or PlaybackState.ACTION_PLAY_FROM_MEDIA_ID
                or PlaybackState.ACTION_STOP
                or PlaybackState.ACTION_SKIP_TO_NEXT
                or PlaybackState.ACTION_SKIP_TO_PREVIOUS)
        builder.setState(PlaybackState.STATE_PLAYING, position, 1.0f)
        val extras = bundleOf(MusicService.SHUFFLE_KEY to shuffle)
        builder.setExtras(extras)
        return builder.build()
    }

    private fun createStateForPaused(position: Long, shuffle: Boolean): PlaybackState {
        val builder = PlaybackState.Builder()
        builder.setActions(PlaybackState.ACTION_PLAY
                or PlaybackState.ACTION_PLAY_FROM_MEDIA_ID
                or PlaybackState.ACTION_SKIP_TO_NEXT
                or PlaybackState.ACTION_SKIP_TO_PREVIOUS
                or PlaybackState.ACTION_STOP)
        builder.setState(PlaybackState.STATE_PAUSED, position, 1.0f)
        val extras = bundleOf(MusicService.SHUFFLE_KEY to shuffle)
        builder.setExtras(extras)
        return builder.build()
    }

    private fun createStateForStopped(): PlaybackState {
        val builder = PlaybackState.Builder()
        Log.v(TAG, "Stopped state: ${PlaybackState.STATE_STOPPED}")
        builder.setActions(PlaybackState.ACTION_PLAY_FROM_MEDIA_ID)
        builder.setState(PlaybackState.STATE_STOPPED, 0L, 1.0f)
        return builder.build()
    }

}