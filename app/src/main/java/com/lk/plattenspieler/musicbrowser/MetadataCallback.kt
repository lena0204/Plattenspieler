package com.lk.plattenspieler.musicbrowser

import android.media.MediaMetadata
import android.media.session.*
import android.util.Log
import com.lk.musicservicelibrary.models.MusicList
import com.lk.musicservicelibrary.models.MusicMetadata
import com.lk.plattenspieler.observables.PlaybackViewModel

/**
 * Erstellt von Lena am 07.09.18.
 */
class MetadataCallback(private val viewModel: PlaybackViewModel): MediaController.Callback() {

    override fun onMetadataChanged(metadata: MediaMetadata) {
        viewModel.metadata.value = MusicMetadata.createFromMediaMetadata(metadata)
    }

    override fun onPlaybackStateChanged(state: PlaybackState) {
        Log.v(this::class.java.simpleName, "New state: ${state.state}")
        viewModel.playbackState.value = state
    }

    override fun onQueueChanged(queue: MutableList<MediaSession.QueueItem>) {
        viewModel.queue.value  = MusicList.createListFromQueue(queue)
    }

}