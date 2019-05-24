package com.lk.musicservicelibrary.playback.state

import com.lk.musicservicelibrary.playback.PlaybackCallback
import com.lk.musicservicelibrary.utils.PlaybackStateBuilder

/**
 * Erstellt von Lena am 05/04/2019.
 */
class PlayingState(private val playback: PlaybackCallback): BasicState(playback) {

    private val TAG = "PlayingState"

    override fun play() {
        throw UnsupportedOperationException()
    }

    override fun pause() {
        playback.getPlayer().pause()
        val position = playback.getPlayer().getCurrentPosition()
        val shuffle = playback.getShuffleFromPlaybackState()
        playback.setPlaybackState(
            PlaybackStateBuilder.createStateForPaused(
                position.toLong(),
                shuffle
            )
        )
        playback.setPlayerState(PausedState(playback))
    }

    override fun skipToNext() {
        val skippedToNext = skipToNextOrStop()
        if(skippedToNext) {
            playback.setPlayerState(PlayingState(playback))
        } else {
            playback.setPlayerState(StoppedState(playback))
        }
    }

    override fun skipToPrevious() {
        skipToPreviousIfPossible()
        playback.setPlayerState(PlayingState(playback))
    }

}