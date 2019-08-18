package com.lk.musicservicelibrary.mocks

import android.util.Log
import com.lk.musicservicelibrary.playback.MusicPlayer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Erstellt von Lena am 2019-08-18.
 */
class MockMusicPlayer: MusicPlayer {

    private val TAG = "MockMusicPlayer"

    private var currentPosition = 0

    override fun preparePlayer(mediaFile: String) {
        Log.i(TAG, "I'm preparing for mediaFile: $mediaFile")
        stop()
        play(0)
    }

    override fun play(position: Int) {
        Log.i(TAG, "I'm playing, start at position $position")
        currentPosition += 50
    }

    override fun pause() {
        Log.i(TAG, "I pause")
    }

    override fun stop() {
        Log.i(TAG, "I stop")
    }

    override fun getCurrentPosition(): Int {
        return currentPosition
    }

    override fun resetPosition() {
        currentPosition = 0
    }

    fun setPositionTo(newValue: Int) {
        currentPosition = newValue
    }

}