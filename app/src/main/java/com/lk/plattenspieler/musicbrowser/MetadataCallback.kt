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

    override fun onMetadataChanged(metadata: MediaMetadata?) {
        if(metadata != null){
            viewModel.setMetadata(MusicMetadata.createFromMediaMetadata(metadata))
        }
    }

    override fun onPlaybackStateChanged(state: PlaybackState?) {
        if(state != null) {
            viewModel.setPlaybackState(state)
        }
    }

    override fun onQueueChanged(queue: MutableList<MediaSession.QueueItem>?) {
        if(queue != null) {
            viewModel.setQueue(MusicList.createListFromQueue(queue))
        }
    }

}