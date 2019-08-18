package com.lk.musicservicelibrary.helpers

import com.lk.musicservicelibrary.models.MusicMetadata
import com.lk.musicservicelibrary.playback.PlaybackCallback
import com.lk.musicservicelibrary.playback.state.States
import org.junit.Assert

/**
 * Erstellt von Lena am 2019-08-18.
 */
class AssertPlayback(private val playback: PlaybackCallback) {

    private val TAG = "AssertPlayback"

    fun stateIs(state: States) {
        Assert.assertEquals(state, playback.getPlayerState().type)
    }

    fun positionIs(position: Int) {
        Assert.assertEquals(position, (playback.getPlayer().getCurrentPosition()))
    }

    fun mediaIdIs(mediaId: String) {
        val currentPlaying = playback.getPlayingList().value?.getItemAtCurrentPlaying() ?: MusicMetadata()
        Assert.assertEquals(mediaId, currentPlaying.id)
    }

}