package com.lk.musicservicelibrary.playback.state

import com.lk.musicservicelibrary.playback.PlaybackCallback
import com.lk.musicservicelibrary.utils.PlaybackStateBuilder

/**
 * Erstellt von Lena am 05/04/2019.
 */
class PausedState (private val playback: PlaybackCallback): BasicState(playback) {

    private val TAG = "PausedState"

    override fun play() {
        val firstMetadata = playback.getPlayingList().value!!.getItemAtCurrentPlaying()
        if(firstMetadata != null) {
            playback.getPlayer().play()
            val shuffle = playback.getShuffleFromPlaybackState()
            playback.setPlaybackState(
                PlaybackStateBuilder.createStateForPlaying(0L, shuffle)
            )
            playback.setPlayerState(PlayingState(playback))
        }
    }

    override fun pause() {
        throw UnsupportedOperationException()
    }

    override fun skipToNext() {
        val skippedToNext = skipToNextOrStop()
        if(skippedToNext) {
            playback.setPlayerState(PausedState(playback))
        } else {
            playback.setPlayerState(StoppedState(playback))
        }
    }

    override fun skipToPrevious() {
        skipToPreviousIfPossible()
        playback.setPlayerState(PausedState(playback))
    }

}