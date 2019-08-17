package com.lk.musicservicelibrary.playback.state

import com.lk.musicservicelibrary.playback.PlaybackCallback
import com.lk.musicservicelibrary.utils.PlaybackStateFactory

/**
 * Erstellt von Lena am 05/04/2019.
 */
class PausedState (private val playback: PlaybackCallback): BasicState(playback) {

    private val TAG = "PausedState"
    override var type: States = States.PAUSED

    override fun play() {
        val firstMetadata = playback.getPlayingList().value!!.getItemAtCurrentPlaying()
        if(firstMetadata != null) {
            val position = playback.getPlayer().getCurrentPosition()
            playback.getPlayer().play(position)
            playback.setPlayerState(PlayingState(playback))
            updateState(States.PLAYING, position.toLong())
        }
    }

    override fun pause() {
        throw UnsupportedOperationException()
    }

    override fun skipToNext() {
        val skippedToNext = skipToNextOrStop()
        if(skippedToNext) {
            playback.setPlayerState(PausedState(playback))
        }
    }

    override fun skipToPrevious() {
        skipToPreviousIfPossible()
        playback.setPlayerState(PausedState(playback))
    }

}