package com.lk.plattenspieler.main

import android.media.MediaMetadata
import android.media.session.*
import com.lk.musicservicelibrary.models.MusicList
import com.lk.musicservicelibrary.models.MusicMetadata
import com.lk.plattenspieler.observables.PlaybackObservable

/**
 * Erstellt von Lena am 07.09.18.
 */
class MetadataCallback: MediaController.Callback() {

    override fun onMetadataChanged(metadata: MediaMetadata) {
        PlaybackObservable.setMetadata(MusicMetadata.createFromMediaMetadata(metadata))
    }

    override fun onPlaybackStateChanged(state: PlaybackState) {
        PlaybackObservable.setState(state)
    }

    override fun onQueueChanged(queue: MutableList<MediaSession.QueueItem>) {
        PlaybackObservable.setQueue(MusicList.createListFromQueue(queue))
    }

}