package com.lk.musicservicelibrary.playback

import android.content.res.AssetFileDescriptor

/**
 * Erstellt von Lena am 06/04/2019.
 */
interface MusicPlayer {

    fun preparePlayer()
    fun playMedia(mediaFile: String, startPlaying: Boolean)
    fun playMedia(mediaFileDescriptor: AssetFileDescriptor, startPlaying: Boolean)
    fun play(position: Int)
    fun pause()
    fun stop()

    fun getCurrentPosition(): Int
    fun resetPosition()

    interface PlaybackFinished {
        fun playbackFinished()
    }

}