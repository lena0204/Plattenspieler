package com.lk.musicservicelibrary.system

import android.media.*
import android.os.Build
import com.lk.musicservicelibrary.utils.EnumAudioFucos
import com.lk.musicservicelibrary.utils.audioFocusChanged

/**
 * Erstellt von Lena am 02.09.18.
 */
object AudioFocusRequester: AudioManager.OnAudioFocusChangeListener {

    var audioFocusStatus = EnumAudioFucos.AUDIO_LOSS

    val audioAttr: AudioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA).build()

    private lateinit var changedHandler: audioFocusChanged
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    fun setup(_changedHandler: audioFocusChanged, _audioManager: AudioManager){
        changedHandler = _changedHandler
        audioManager = _audioManager
    }

    fun requestAudioFocus(): Boolean {
        val result = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(audioAttr)
                    .setOnAudioFocusChangeListener(this)
                    .build()
            audioManager.requestAudioFocus(audioFocusRequest as AudioFocusRequest)
        } else {
            audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    fun releaseAudioFocus(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null){
            audioManager.abandonAudioFocusRequest(audioFocusRequest as AudioFocusRequest)
        } else {
            audioManager.abandonAudioFocus(this)
        }
    }

    override fun onAudioFocusChange(newAudioFocusType: Int) {
        val oldFocus = audioFocusStatus
        when(newAudioFocusType){
            AudioManager.AUDIOFOCUS_LOSS -> {
                audioFocusStatus = EnumAudioFucos.AUDIO_LOSS
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                audioFocusStatus = EnumAudioFucos.AUDIO_PAUSE_PLAYING
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                audioFocusStatus = EnumAudioFucos.AUDIO_DUCK
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                audioFocusStatus = EnumAudioFucos.AUDIO_FOCUS
            }
        }
        changedHandler(oldFocus, audioFocusStatus)
    }


}

