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
import com.lk.plattenspieler.fragments.LyricsAddingDialog
import com.lk.plattenspieler.main.*
import com.lk.plattenspieler.observables.*
import com.lk.plattenspieler.utils.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
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

    init {
        playbackViewModel.controllerAction.observe(activityNew, this)
    }

    fun completeSetup(){
        val c = ComponentName(context, MusicService::class.java)
        mbrowser = MediaBrowser(context, c, connectionCallback, null)
        subscriptionCallback = MusicSubscriptionCallback(mbrowser, mediaViewModel)
        mbrowser.connect()
    }

    override fun onChanged(update: Any?) {
        if(update is ControllerAction) {
            Log.d(TAG, "ControllerAction: ${update.action}")
            when (update.action) {
                EnumActions.PLAYS -> plays()
                EnumActions.PLAY_FROM_ID -> playFromMediaId(update)
                EnumActions.PREPARE_FROM_ID -> prepareFromId(update)
                EnumActions.PLAY_PAUSE -> playPause()
                EnumActions.NEXT -> musicController.transportControls.skipToNext()
                EnumActions.PREVIOUS -> musicController.transportControls.skipToPrevious()
                EnumActions.STOP -> stop()
                EnumActions.QUEUE -> queue(update)
                EnumActions.QUEUE_RESTORED -> queueRestored(update)
                EnumActions.SHUFFLE -> shuffle(update)
                EnumActions.SHUFFLE_ALL -> shuffleAll()
                EnumActions.SHOW_ALBUM -> showAlbumTitles(update.titleId)
            }
        }
    }

    private fun plays(){
        activityNew.showBar()
        playbackViewModel.queue.value = MusicList.createListFromQueue(musicController.queue!!)
        playbackViewModel.metadata.value = MusicMetadata.createFromMediaMetadata(musicController.metadata!!)
        playbackViewModel.playbackState.value = musicController.playbackState
    }

    private fun prepareFromId(action: ControllerAction) {
        musicController.transportControls.playFromMediaId(action.titleId, action.args)
        activityNew.showBar()
    }

    private fun playFromMediaId(action: ControllerAction) {
        musicController.transportControls.playFromMediaId(action.titleId, action.args)
        queue(action)
        activityNew.showBar()
    }

    private fun playPause(){
        if(playbackViewModel.playbackState.value?.state == PlaybackState.STATE_PLAYING)
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

    private fun queue(action: ControllerAction) {
        musicController.sendCommand("addQueue", action.args, null)
    }

    private fun queueRestored(action: ControllerAction){
        musicController.sendCommand("addRestoredQueue", action.args, null)
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
            val i = Random().nextInt(list.countItems())
            val titleid = mediaViewModel.titleList.value!!.getItemAt(i).id
            val args = bundleOf(MusicService.SHUFFLE_KEY to true)
            musicController.transportControls.playFromMediaId(titleid, args)
        }
    }

    private fun shuffleAll() {
        musicController.sendCommand("addAll", null, null)
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
        } else
            recreateActivity()
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
            launch(UI) {
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