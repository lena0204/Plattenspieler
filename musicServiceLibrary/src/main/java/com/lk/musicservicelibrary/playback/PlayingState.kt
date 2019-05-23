package com.lk.musicservicelibrary.playback

import android.os.Bundle
import com.lk.musicservicelibrary.main.MusicService
import com.lk.musicservicelibrary.utils.PlaybackStateCreator

/**
 * Erstellt von Lena am 05/04/2019.
 */
class PlayingState(private val playback: PlaybackCallback): PlayerState(playback) {

    private val TAG = "PlayingState"

    override fun playFromId(mediaId: String?, extras: Bundle?) {
        extras?.classLoader = this.javaClass.classLoader
        val shuffle = extras?.getBoolean(MusicService.SHUFFLE_KEY) ?: false
        val playingList = createPlayingList(mediaId, shuffle)
        playCurrentPlayingItem(playingList)
        playback.setPlayerState(PlayingState(playback))
    }

    override fun play() {
        throw UnsupportedOperationException()
    }

    override fun pause() {
        playback.getPlayer().pause()
        val position = playback.getPlayer().getCurrentPosition()
        val shuffle = playback.getShuffleFromPlaybackState()
        playback.setPlaybackState(
            PlaybackStateCreator.createStateForPaused(
                position.toLong(),
                shuffle
            )
        )
        playback.setPlayerState(PausedState(playback))
    }

    override fun stop() {
        stopPlayback()
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