package com.lk.musicservicelibrary.playback

/**
 * Erstellt von Lena am 06/04/2019.
 */
interface MusicPlayer {

    fun preparePlayer(mediaFile: String)
    fun play(position: Int)
    fun pause()
    fun stop()

    fun getCurrentPosition(): Int
    fun resetPosition()

    interface PlaybackFinished {
        fun playbackFinished()
    }

}