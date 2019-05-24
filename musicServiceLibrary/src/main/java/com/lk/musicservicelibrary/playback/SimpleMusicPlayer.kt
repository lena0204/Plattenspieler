package com.lk.musicservicelibrary.playback

import android.media.MediaPlayer
import android.util.Log
import com.lk.musicservicelibrary.system.AudioFocusRequester

/**
 * Erstellt von Lena am 06/04/2019.
 */
class SimpleMusicPlayer(private val listener: MusicPlayer.PlaybackFinished): MusicPlayer {

    private val TAG = "SimpleMusicPlayer"

    private var musicPlayer = MediaPlayer()
    private var startPosition = 0
    private var created = false

    override fun getCurrentPosition(): Int = musicPlayer.currentPosition

    override fun preparePlayer(mediaFile: String) {
        stop()
        createMusicPlayer()
        playMediaFile(mediaFile)
    }

    private fun createMusicPlayer(){
        musicPlayer = MediaPlayer()
        musicPlayer.setOnPreparedListener {
            musicPlayer.seekTo(startPosition)
            musicPlayer.start()
        }
        musicPlayer.setOnErrorListener { _, what, extra ->
            Log.e(TAG, "MusicPlayerError: $what; $extra")
            false
        }
        musicPlayer.setOnCompletionListener {
            listener.playbackFinished()
        }
        musicPlayer.setAudioAttributes(AudioFocusRequester.audioAttr)
        Log.v(TAG, "Musicplayer created")
        created = true
    }

    private fun playMediaFile(mediaFile: String){
        Log.v(TAG, "playFile from $mediaFile")
        musicPlayer.setDataSource(mediaFile)
        musicPlayer.prepareAsync()
    }

    override fun play(position: Int) {
        startPosition = position
        musicPlayer.seekTo(position)
        musicPlayer.start()
    }

    override fun pause() {
        musicPlayer.pause()
    }

    override fun stop() {
        if (created) {
            musicPlayer.stop()
            musicPlayer.reset()
            musicPlayer.release()
            created = false
        }
    }

}