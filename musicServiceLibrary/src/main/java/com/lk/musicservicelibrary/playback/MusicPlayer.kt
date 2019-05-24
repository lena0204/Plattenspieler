package com.lk.musicservicelibrary.playback

/**
 * Erstellt von Lena am 06/04/2019.
 */
interface MusicPlayer {

    fun preparePlayer(mediaFile: String)
    fun play(position: Int = 0)
    fun pause()
    fun stop()

    fun getCurrentPosition(): Int

    interface PlaybackFinished {
        fun playbackFinished()
    }

}