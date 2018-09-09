package com.lk.musicservicelibrary.models

import android.media.session.PlaybackState

/**
 * Erstellt von Lena am 02.09.18.
 */
data class PlaybackData(
        var metadata: MusicMetadata = MusicMetadata(),
        var playbackState: PlaybackState = PlaybackState.Builder().build(),
        var queue: MusicList = MusicList()
)