package com.lk.musicservicelibrary.playback.state

import com.lk.musicservicelibrary.playback.PlaybackCallback

/**
 * Erstellt von Lena am 05/04/2019.
 */
class PlayingState(private val playback: PlaybackCallback): BasicState(playback) {

    private val TAG = "PlayingState"
    override var type: States = States.PLAYING

    override fun play() {
        throw UnsupportedOperationException()
    }

    override fun pause() {
        playback.getPlayer().pause()
        val position = playback.getPlayer().getCurrentPosition()

        updateState(States.PAUSED, position.toLong())
        playback.setPlayerState(PausedState(playback))
    }

    override fun skipToNext() {
        val skippedToNext = skipToNextOrStop()
        if(skippedToNext) {
            playback.setPlayerState(PlayingState(playback))
        }
    }

    override fun skipToPrevious() {
        skipToPreviousIfPossible()
        playback.setPlayerState(PlayingState(playback))
    }

}