package com.lk.musicservicelibrary.playback.state

import com.lk.musicservicelibrary.playback.PlaybackCallback

/**
 * Erstellt von Lena am 05/04/2019.
 */
class StoppedState (playback: PlaybackCallback): BasicState(playback) {

    private val TAG = "StoppedState"
    override var type: States = States.STOPPED

    override fun play() {
        throw UnsupportedOperationException("Can't play in stopped state")
    }

    override fun pause() {
        throw UnsupportedOperationException("Can't pause in stopped state")
    }

    override fun stop() {
        throw UnsupportedOperationException("Can't stop, already in stopped state")
    }

    override fun skipToNext() {
        throw UnsupportedOperationException("Can't skip to next in stopped state")
    }

    override fun skipToPrevious() {
        throw UnsupportedOperationException("Can't skip to previous in stopped state")
    }

}