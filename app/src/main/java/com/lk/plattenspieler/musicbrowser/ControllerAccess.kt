package com.lk.plattenspieler.musicbrowser

import android.content.ComponentName
import android.media.browse.MediaBrowser
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import androidx.core.content.edit
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.lk.musicservicelibrary.database.SongContentProvider
import com.lk.musicservicelibrary.main.MusicService
import com.lk.musicservicelibrary.models.MusicList
import com.lk.musicservicelibrary.models.MusicMetadata
import com.lk.musicservicelibrary.utils.Commands
import com.lk.plattenspieler.fragments.LyricsAddingDialog
import com.lk.plattenspieler.main.*
import com.lk.plattenspieler.observables.*
import com.lk.plattenspieler.utils.*
import kotlinx.coroutines.*
import java.util.*

/**
 * Erstellt von Lena am 09.09.18.
 */
class ControllerAccess(private val activityNew: MainActivityNew): Observer<Any> {

    private val playbackViewModel = ViewModelProviders.of(activityNew).get(PlaybackViewModel::class.java)
    private val mediaViewModel = ViewModelProviders.of(activityNew).get(MediaViewModel::class.java)

    private val TAG = "MusicClient"
    private val context = activityNew.applicationContext
    private val connectionCallback = BrowserConnectionCallback()
    private val controllerCallback = MetadataCallback(playbackViewModel)
    lateinit var subscriptionCallback: MusicSubscriptionCallback
    private val permissionRequester = PermissionRequester(activityNew)

    private var sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private lateinit var mbrowser: MediaBrowser
    private lateinit var musicController: MediaController
    // PROBLEM_ Absturz: not initializied wenn ein Design geändert wurde

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
            Log.d(TAG, "ControllerAction: ${update.action}")
            when (update.action) {
                EnumActions.IS_PLAYING -> updateAlreadyPlaying()
                EnumActions.PLAY_FROM_ID -> playFromMediaId(update)
                // EnumActions.PREPARE_FROM_ID -> prepareFromId(update)
                EnumActions.PLAY_PAUSE -> playPause()
                EnumActions.NEXT -> musicController.transportControls.skipToNext()
                EnumActions.PREVIOUS -> musicController.transportControls.skipToPrevious()
                EnumActions.STOP -> stop()
                // EnumActions.QUEUE -> queue(update)
                EnumActions.QUEUE_RESTORED -> queueRestored(update)
                EnumActions.SHUFFLE -> playFromMediaId(update)
                EnumActions.SHUFFLE_ALL -> shuffleAll()
                EnumActions.SHOW_ALBUM -> showAlbumTitles(update.titleId)
            }
        }
    }

    private fun updateAlreadyPlaying(){
        activityNew.showBar()
        playbackViewModel.setQueue(MusicList.createListFromQueue(musicController.queue!!))
        playbackViewModel.setMetadata(MusicMetadata.createFromMediaMetadata(musicController.metadata!!))
        playbackViewModel.setPlaybackState(musicController.playbackState!!)
    }

    private fun prepareFromId(action: ControllerAction) {
        musicController.transportControls.playFromMediaId(action.titleId, action.args)
        activityNew.showBar()
    }

    private fun playFromMediaId(action: ControllerAction) {
        musicController.transportControls.playFromMediaId(action.titleId, action.args)
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
        context.contentResolver.delete(SongContentProvider.CONTENT_URI, null, null)
        sharedPreferences.edit { putBoolean(MainActivityNew.PREF_PLAYING, false) }
    }

    private fun queueRestored(action: ControllerAction){
        musicController.sendCommand(Commands.ADD_QUEUE, action.args, null)
    }

/*
    private fun queue(action: ControllerAction) {
        musicController.sendCommand("addQueue", action.args, null)
    }

    private fun shuffle(action: ControllerAction){
        playFirstTitleRandomly(action)
        randomQueue(action)
        activityNew.showBar()
    }

    private fun randomQueue(action: ControllerAction) {
        musicController.sendCommand("addRandomQueue", action.args, null)
    }

    private fun playFirstTitleRandomly(action: ControllerAction) {
        val list = action.args.getParcelable<MusicList>("L")
        if(list != null) {
            val i = Random().nextInt(list.size())
            val titleid = mediaViewModel.titleList.value!!.getItemAt(i).id
            val args = bundleOf(MusicService.SHUFFLE_KEY to true)
            musicController.transportControls.playFromMediaId(titleid, args)
        }
    } */

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
        if(musicController.playbackState?.state != PlaybackState.STATE_PLAYING){
            playbackViewModel.saveQueue()
            sharedPreferences.edit { putBoolean(MainActivityNew.PREF_PLAYING, true) }
        } else {
            sharedPreferences.edit { putBoolean(MainActivityNew.PREF_PLAYING, false) }
        }
        sharedPreferences.edit {
            putBoolean(MainActivityNew.PREF_SHUFFLE, playbackViewModel.getShuffleFromPlaybackState()) }
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

    // TODO recreate führt zum Absturz aufgrund fehlender Initialisierung (s. PROBLEM)
    private fun recreateActivity(){
        saveState()
        // activityNew.recreate()
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
            GlobalScope.launch(Dispatchers.Default){
                playbackViewModel.restoreSavedState(musicController.playbackState?.state)
            }
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