package com.lk.musicservicelibrary.playback

import android.os.Bundle
import com.lk.musicservicelibrary.main.MusicService
import com.lk.musicservicelibrary.utils.PlaybackStateCreator

/**
 * Erstellt von Lena am 05/04/2019.
 */
class StoppedState (private val playback: PlaybackCallback): PlayerState(playback) {

    private val TAG = "StoppedState"

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