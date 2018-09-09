package com.lk.music_service_library.background

import android.annotation.TargetApi
import android.content.Context
import android.media.*
import android.os.Build
import android.util.Log
import com.lk.music_service_library.observables.PlaybackActions
import com.lk.music_service_library.observables.PlaybackDataObservable as DataObserv
import com.lk.music_service_library.utils.*
import java.util.*

/**
 * Erstellt von Lena am 13.05.18.
 * Verwaltet den Musikplayer zusammen mit dem Audiofokus
 */
class MusicPlayback(private val service: MusicService): Observer {

    // IDEA_ Audiofokushandling in eigene Klasse inkl. Listener auslagern

    private val TAG = "MusicPlayback"
    private val audioManager = service.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val audioFocusCallback = AudioFocusCallback()
    private val audioAttr = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA).build()

    private var musicPlayer: MediaPlayer? = null

    @TargetApi(26)
    private var audioFocusRequest: AudioFocusRequest? = null
    private var audioFucosStatus = EnumAudioFucos.AUDIO_LOSS

    init{
        DataObserv.addObserver(this)
    }

    override fun update(o: Observable?, arg: Any?) {
        if(arg != null && arg is PlaybackActions){
            when(arg){
                PlaybackActions.ACTION_TRY_PLAYING -> tryPlaying()
                PlaybackActions.ACTION_CAN_PLAY -> play()
                PlaybackActions.ACTION_PAUSE -> pause()
                PlaybackActions.ACTION_STOP -> stop()
            }
        }
    }

    private fun tryPlaying(){
        if(canPlay()){
            DataObserv.canPlay(service.getMetadataForId(DataObserv.musicId))
        }
    }

    private fun canPlay(): Boolean {
        stopPlayerIfPlaying()
        if(areTitleAndAudiofocusAvailable()){
            audioFucosStatus = EnumAudioFucos.AUDIO_FOCUS
            return true
        }
        return false
    }

    private fun stopPlayerIfPlaying(){
        if(musicPlayer != null){
            if(musicPlayer!!.isPlaying)
                musicPlayer!!.stop()
        }
    }

    private fun areTitleAndAudiofocusAvailable(): Boolean = (DataObserv.musicId != "" &&
            checkAudioFocus() == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)

    private fun checkAudioFocus(): Int{
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(audioAttr)
                    .setOnAudioFocusChangeListener(audioFocusCallback)
                    .build()
            audioManager.requestAudioFocus(audioFocusRequest)
        } else {
            audioManager.requestAudioFocus(audioFocusCallback, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
    }

    private fun play(){
        musicPlayer = MediaPlayer()
        musicPlayer!!.setOnPreparedListener { _ -> startPlaying() }
        musicPlayer!!.setOnErrorListener { _, what, extra ->
            Log.e(TAG, "MusicPlayerError: $what; $extra")
            false
        }
        musicPlayer!!.setOnCompletionListener { _ -> DataObserv.next() }
        musicPlayer!!.setAudioAttributes(audioAttr)
        musicPlayer!!.setDataSource(DataObserv.musicFilePath)
        musicPlayer!!.prepareAsync()
    }

    private fun startPlaying(){
        if (DataObserv.positionMs > 0) {
            musicPlayer!!.seekTo(DataObserv.positionMs.toInt())
        }
        musicPlayer!!.start()
    }

    private fun pause() {
        Log.i(TAG, "handleOnPause")
        musicPlayer!!.pause()
    }

    private fun stop(){
        Log.d(TAG, "handleOnStop")
        releaseAudioFocus()
        releaseMusicPlayer()
    }

    private fun releaseAudioFocus(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null){
            audioManager.abandonAudioFocusRequest(audioFocusRequest as AudioFocusRequest)
        } else {
            audioManager.abandonAudioFocus(audioFocusCallback)
        }
        audioFucosStatus = EnumAudioFucos.AUDIO_LOSS
    }

    private fun releaseMusicPlayer(){
        musicPlayer?.stop()
        musicPlayer?.reset()
        musicPlayer?.release()
        musicPlayer = null
    }

    inner class AudioFocusCallback: AudioManager.OnAudioFocusChangeListener{

        override fun onAudioFocusChange(focusChange: Int) {
            when(focusChange){
                AudioManager.AUDIOFOCUS_LOSS -> {
                    DataObserv.stop()
                    audioFucosStatus = EnumAudioFucos.AUDIO_LOSS
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    if(musicPlayer!!.isPlaying){
                        DataObserv.pause(musicPlayer!!.currentPosition.toLong())
                        audioFucosStatus = EnumAudioFucos.AUDIO_PAUSE_PLAYING
                    }
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    musicPlayer!!.setVolume(0.3f, 0.3f)
                    audioFucosStatus = EnumAudioFucos.AUDIO_DUCK
                }
                AudioManager.AUDIOFOCUS_GAIN -> {
                    if(audioFucosStatus == EnumAudioFucos.AUDIO_PAUSE_PLAYING){
                        DataObserv.tryPlaying()
                    } else {
                        musicPlayer!!.setVolume(0.8f, 0.8f)
                        // generell leiser spielen
                    }
                }
            }
        }
    }

}