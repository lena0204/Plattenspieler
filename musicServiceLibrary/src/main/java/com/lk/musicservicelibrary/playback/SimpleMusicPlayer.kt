package com.lk.musicservicelibrary.playback

import android.media.MediaPlayer
import android.util.Log
import com.lk.musicservicelibrary.system.AudioFocusRequester

/**
 * Erstellt von Lena am 06/04/2019.
 */
class SimpleMusicPlayer: MusicPlayer {

    private val TAG = "SimpleMusicPlayer"

    private var musicPlayer = MediaPlayer()
    private var startPosition = 0

    override fun getCurrentPosition(): Int = musicPlayer.currentPosition

    override fun preparePlayer(mediaFile: String) {
        stop()
        createMusicPlayer()
        playMediaFile(mediaFile)
    }

    private fun createMusicPlayer(){
        // TODO music player states abfangen, stoppes is called beforehand...
        musicPlayer = MediaPlayer()
        musicPlayer.setOnPreparedListener {
            Log.v(TAG, "prepared player: start to play")
            musicPlayer.seekTo(startPosition)
            musicPlayer.start()
        }
        musicPlayer.setOnErrorListener { _, what, extra ->
            Log.e(TAG, "MusicPlayerError: $what; $extra")
            false
        }
        musicPlayer.setOnCompletionListener { // TODO Zugriff auf skipToNext fehlt
            }
        musicPlayer.setAudioAttributes(AudioFocusRequester.audioAttr)
        Log.v(TAG, "Musicplayer created")
    }

    private fun playMediaFile(mediaFile: String){
        Log.v(TAG, "playFile from $mediaFile")
        musicPlayer.setDataSource(mediaFile)
        musicPlayer.prepareAsync()
    }

    override fun play(position: Int) {
        startPosition = position
    }

    override fun pause() {
        musicPlayer.pause()
    }

    override fun stop() {
        musicPlayer.stop()
        musicPlayer.reset()
        musicPlayer.release()
    }

}