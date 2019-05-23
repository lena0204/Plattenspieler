package com.lk.musicservicelibrary.playback

import android.os.Bundle
import com.lk.musicservicelibrary.main.MusicService
import com.lk.musicservicelibrary.utils.PlaybackStateCreator

/**
 * Erstellt von Lena am 05/04/2019.
 */
class PausedState (private val playback: PlaybackCallback): PlayerState(playback) {

    private val TAG = "PausedState"

    override fun playFromId(mediaId: String?, extras: Bundle?) {
        extras?.classLoader = this.javaClass.classLoader
        val shuffle = extras?.getBoolean(MusicService.SHUFFLE_KEY) ?: false
        val playingList = createPlayingList(mediaId, shuffle)
        playCurrentPlayingItem(playingList)
        playback.setPlayingList(playingList)
        playback.setPlaybackState(
            PlaybackStateCreator.createStateForPlaying(
                0L,
                shuffle
            )
        )
        playback.setPlayerState(PlayingState(playback))
    }

    override fun play() {
        val firstMetadata = playback.getPlayingList().value!!.getItemAtCurrentPlaying()
        if(firstMetadata != null) {
            playback.getPlayer().play()
            val shuffle = playback.getShuffleFromPlaybackState()
            playback.setPlaybackState(
                PlaybackStateCreator.createStateForPlaying(
                    0L,
                    shuffle
                )
            )
            playback.setPlayerState(PlayingState(playback))
        }
    }

    override fun pause() {
        throw UnsupportedOperationException()
    }

    override fun stop() {
        stopPlayback()
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