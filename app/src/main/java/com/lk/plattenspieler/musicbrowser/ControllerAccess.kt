package com.lk.plattenspieler.musicbrowser

import android.content.ComponentName
import android.media.browse.MediaBrowser
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Build
import androidx.preference.PreferenceManager
import android.util.Log
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.lk.musicservicelibrary.main.MusicService
import com.lk.musicservicelibrary.models.MusicList
import com.lk.musicservicelibrary.models.MusicMetadata
import com.lk.musicservicelibrary.utils.Commands
import com.lk.plattenspieler.fragments.LyricsAddingDialog
import com.lk.plattenspieler.main.*
import com.lk.plattenspieler.observables.*
import com.lk.plattenspieler.utils.*
import kotlinx.coroutines.*

/**
 * Erstellt von Lena am 09.09.18.
 */
class ControllerAccess(private val activityNew: MainActivityNew): Observer<Any> {

    private val playbackViewModel = ViewModelProviders.of(activityNew).get(PlaybackViewModel::class.java)
    private val mediaViewModel = ViewModelProviders.of(activityNew).get(MediaViewModel::class.java)

    private val TAG = "ControllerAccess"
    private val context = activityNew.applicationContext
    private val connectionCallback = BrowserConnectionCallback()
    private val controllerCallback = MetadataCallback(playbackViewModel)
    lateinit var subscriptionCallback: MusicSubscriptionCallback
    private val permissionRequester = PermissionRequester(activityNew)

    private var sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private lateinit var mbrowser: MediaBrowser
    private lateinit var musicController: MediaController

    init {
        playbackViewModel.setObserverToAction(activityNew, this)
    }

    fun completeSetup(){
        val component = ComponentName(context, MusicService::class.java)
        mbrowser = MediaBrowser(context, component, connectionCallback, null)
        subscriptionCallback = MusicSubscriptionCallback(mbrowser, mediaViewModel)
        mbrowser.connect()
    }

    override fun onChanged(update: Any?) {
        if(update is ControllerAction) {
            when (update.action) {
                EnumActions.PLAY_FROM_ID -> playFromMediaId(update)
                EnumActions.PLAY_PAUSE -> playPause()
                EnumActions.NEXT -> musicController.transportControls.skipToNext()
                EnumActions.PREVIOUS -> musicController.transportControls.skipToPrevious()
                EnumActions.STOP -> stop()
                EnumActions.SHUFFLE -> playFromMediaId(update)
                EnumActions.SHUFFLE_ALL -> shuffleAll()
                EnumActions.SHOW_ALBUM -> showAlbumTitles(update.titleId)
                EnumActions.DONE -> { }
            }
            if(update.action != EnumActions.DONE) {
                playbackViewModel.callAction(ControllerAction(EnumActions.DONE))
            }
        }
    }

    private fun playFromMediaId(action: ControllerAction) {
        musicController.transportControls.playFromMediaId(action.titleId, null)
        activityNew.showBar()
    }

    private fun playPause(){
        if(playbackViewModel.getPlaybackState().state == PlaybackState.STATE_PLAYING)
            musicController.transportControls.pause()
        else
            musicController.transportControls.play()
    }

    private fun stop() {
        if(musicController.playbackState?.state != PlaybackState.STATE_STOPPED){
            musicController.transportControls.stop()
        }
        activityNew.hideBar()
    }

    private fun setupQueue() {
        playbackViewModel.setShowBar(true)
        if(isPlayingOrPaused(musicController.playbackState?.state)) {
            updateAlreadyPlaying()
        } else {
            GlobalScope.launch(Dispatchers.Default){
                musicController.sendCommand(Commands.RESTORE_QUEUE, null, null)
            }
        }
    }

    private fun isPlayingOrPaused(controllerState: Int?): Boolean =
        (controllerState == PlaybackState.STATE_PLAYING || controllerState == PlaybackState.STATE_PAUSED)

    private fun updateAlreadyPlaying(){
        playbackViewModel.setQueue(MusicList.createListFromQueue(musicController.queue!!))
        playbackViewModel.setMetadata(MusicMetadata.createFromMediaMetadata(musicController.metadata!!))
        playbackViewModel.setPlaybackState(musicController.playbackState!!)
    }

    private fun shuffleAll() {
        musicController.sendCommand(Commands.ADD_ALL, null, null)
        activityNew.showBar()
    }

    private fun showAlbumTitles(albumid: String) {
        mbrowser.subscribe("ALBUM-$albumid", subscriptionCallback)
    }

    fun clear(){
        saveState()
        mbrowser.disconnect()
    }

    private fun saveState(){
        Log.d(TAG, "Save state: ${musicController.playbackState?.state} und ${PlaybackState.STATE_PLAYING}")
        if(musicController.playbackState?.state != PlaybackState.STATE_PLAYING){
            playbackViewModel.saveQueue()
        } else {
            Log.d(TAG, "Still playing")
        }
    }

    fun applyTheme(design: EnumTheme){
        ThemeChanger.writeThemeToPreferences(sharedPreferences, design)
        if(design == EnumTheme.THEME_LINEAGE){
            if(MainActivityNew.isVersionGreaterThan(Build.VERSION_CODES.M)
                    && permissionRequester.requestDesignReadPermission()){
                recreateActivity()
            }
        } else {
            recreateActivity()
        }
    }

    private fun recreateActivity(){
        saveState()
        activityNew.recreate()
    }

    fun addLyrics() {
        val dialog = LyricsAddingDialog()
        dialog.show(activityNew.supportFragmentManager, "LyricsAddingDialog")
    }

    inner class BrowserConnectionCallback: MediaBrowser.ConnectionCallback(){

        override fun onConnected() {
            val token = mbrowser.sessionToken
            activityNew.mediaController = MediaController(context, token)
            musicController = activityNew.mediaController
            musicController.registerCallback(controllerCallback)
            mbrowser.subscribe(mbrowser.root, subscriptionCallback)
            setupQueue()
        }

        override fun onConnectionFailed() {
            Log.e(TAG, "Connection to service failed")
        }

        override fun onConnectionSuspended() {
            Log.w(TAG, "onConnectionSuspended")
            if(activityNew.mediaController != null){
                activityNew.mediaController.unregisterCallback(controllerCallback)
                activityNew.mediaController = null
            }
        }
    }
}