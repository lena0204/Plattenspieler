package com.lk.musicservicelibrary.playback

import android.annotation.TargetApi
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.os.ParcelFileDescriptor
import android.util.Log
import com.lk.musicservicelibrary.system.AudioFocusRequester
import java.io.FileDescriptor

/**
 * Erstellt von Lena am 06/04/2019.
 */
class SimpleMusicPlayer(private val listener: MusicPlayer.PlaybackFinished): MusicPlayer {

    private val TAG = "SimpleMusicPlayer"

    private var musicPlayer = MediaPlayer()
    private var startPosition = 0
    private var created = false

    override fun getCurrentPosition(): Int = musicPlayer.currentPosition

    override fun resetPosition() {
        musicPlayer.seekTo(0)
    }

    override fun preparePlayer() {
        stop()
        createMusicPlayer()
    }

    override fun playMedia(mediaFile: String, startPlaying: Boolean) {
        playMediaFile(mediaFile, startPlaying)
    }

    override fun playMedia(mediaFileDescriptor: AssetFileDescriptor, startPlaying: Boolean) {
        playMediaFile(mediaFileDescriptor, startPlaying)
    }

    private fun createMusicPlayer(){
        musicPlayer = MediaPlayer()
        musicPlayer.setOnErrorListener { _, what, extra ->
            Log.e(TAG, "MusicPlayerError: $what; $extra")
            false
        }
        musicPlayer.setOnCompletionListener {
            listener.playbackFinished()
        }
        musicPlayer.setAudioAttributes(AudioFocusRequester.audioAttr)
        musicPlayer.setVolume(0.7f, 0.7f)
        Log.v(TAG, "Musicplayer created")
        created = true
    }

    private fun playMediaFile(mediaFile: String, startPlaying: Boolean){
        Log.v(TAG, "playFile from $mediaFile")
        musicPlayer.setDataSource(mediaFile)

        musicPlayer.setOnPreparedListener {
            musicPlayer.seekTo(startPosition)
            if(startPlaying) {
                musicPlayer.start()
            }
        }
        musicPlayer.prepareAsync()
    }


    @TargetApi(24)
    private fun playMediaFile(mediaFileDescriptor: AssetFileDescriptor, startPlaying: Boolean){
        Log.v(TAG, "playFile from $mediaFileDescriptor")
        musicPlayer.setDataSource(mediaFileDescriptor)

        musicPlayer.setOnPreparedListener {
            musicPlayer.seekTo(startPosition)
            if(startPlaying) {
                musicPlayer.start()
            }
        }
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
            startPosition = 0
            musicPlayer.stop()
            musicPlayer.reset()
            musicPlayer.release()
            created = false
        }
    }

}