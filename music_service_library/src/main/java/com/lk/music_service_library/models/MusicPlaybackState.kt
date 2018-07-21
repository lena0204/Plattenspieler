package com.lk.music_service_library.models

import android.media.session.PlaybackState

/**
 * Erstellt von Lena am 12.05.18.
 */
data class MusicPlaybackState(
        var shuffleOn: Boolean = false,
        var state: Int = PlaybackState.STATE_STOPPED
)