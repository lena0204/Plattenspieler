package com.lk.musicservicelibrary.system

import android.content.*
import android.media.session.PlaybackState
import android.util.Log
import com.lk.musicservicelibrary.main.MusicActionsCallback
import com.lk.musicservicelibrary.main.MusicService
import com.lk.musicservicelibrary.main.MusicService.Companion.ACTION_MEDIA_NEXT
import com.lk.musicservicelibrary.main.MusicService.Companion.ACTION_MEDIA_PAUSE
import com.lk.musicservicelibrary.main.MusicService.Companion.ACTION_MEDIA_PLAY

/**
 * Erstellt von Lena am 05.09.18.
 */
class NotificationActionReceiver(private val actionsCallback: MusicActionsCallback)
            : BroadcastReceiver() {

    private val TAG = this::class.java.simpleName

    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent != null){
            Log.d(TAG, "onReceive: " + intent.action + "; " + MusicService.PLAYBACK_STATE)
            when(intent.action){
                ACTION_MEDIA_PLAY -> handlePlayIntent()
                ACTION_MEDIA_PAUSE -> handlePauseIntent()
                ACTION_MEDIA_NEXT -> handleNextIntent()
            }
        }
    }

    private fun handlePlayIntent(){
        if(PlaybackState.STATE_PAUSED == MusicService.PLAYBACK_STATE){
            actionsCallback.onPlay()
        }
    }

    private fun handlePauseIntent(){
        if(PlaybackState.STATE_PLAYING == MusicService.PLAYBACK_STATE){
            actionsCallback.onPause()
        }
    }

    private fun handleNextIntent(){
        if(PlaybackState.STATE_PLAYING == MusicService.PLAYBACK_STATE ||
                PlaybackState.STATE_PAUSED == MusicService.PLAYBACK_STATE) {
            actionsCallback.onSkipToNext()
        }
    }

    /*private fun logCurrentState(intentAction: String){
        // zur Fehlererkennung
        Log.d(TAG, intentAction + ": Servicestate: " + MusicService.PLAYBACK_STATE)
    }*/
}