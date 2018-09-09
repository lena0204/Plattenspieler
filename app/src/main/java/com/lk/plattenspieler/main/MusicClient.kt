package com.lk.plattenspieler.main

import android.content.*
import android.media.browse.MediaBrowser
import android.media.session.*
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import androidx.core.os.bundleOf
import com.lk.musicservicelibrary.database.SongContentProvider
import com.lk.musicservicelibrary.database.SongDBAccess
import com.lk.musicservicelibrary.main.MusicService
import com.lk.musicservicelibrary.models.MusicList
import com.lk.plattenspieler.R
import com.lk.plattenspieler.fragments.LyricsAddingDialog
import com.lk.plattenspieler.observables.PlaybackObservable
import com.lk.plattenspieler.observables.MedialistsObservable
import com.lk.plattenspieler.utils.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.UI
import java.util.*

/**
 * Erstellt von Lena am 12.05.18.
 * Verwaltet Aktionen vom Menü und den Zugriff inkl. Callbacks auf den MediaBrowserService (MusicService),
 */
class MusicClient(val activity: MainActivityNew) {

    private val TAG = "MusicClient"
    private val connectionCallback = BrowserConnectionCallback()
    private val controllerCallback = MetadataCallback()
    lateinit var subscriptionCallback: MusicSubscriptionCallback
    private val permissionRequester = PermissionRequester(activity)

    private var wasQueueSaved = false
    private var sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)

    private lateinit var mbrowser: MediaBrowser
    private lateinit var musicController: MediaController
    
    fun completeSetup(restoringQueue: Boolean){
        this.wasQueueSaved = restoringQueue
        val c = ComponentName(activity.applicationContext, MusicService::class.java)
        mbrowser = MediaBrowser(activity, c, connectionCallback, null)
        subscriptionCallback = MusicSubscriptionCallback(mbrowser)
        mbrowser.connect()
    }

    fun restoreSavedState(){
        val state = musicController.playbackState?.state
        if(state != PlaybackState.STATE_PLAYING && state != PlaybackState.STATE_PAUSED){
            if(wasQueueSaved)
                restoreQueue()
        } else {
            activity.showBar()
        }
        sendShuffleIfOn()
    }

    private fun restoreQueue(){
        val music = SongDBAccess.restoreFirstQueueItem(activity.contentResolver)
        if(music == null){
            sharedPreferences.edit().putBoolean(MainActivityNew.PREF_PLAYING, false).apply()
        } else {
            sendFirstItemToController(music.id)
            sendQueueIfAvailable()
        }
    }

    private fun sendFirstItemToController(titleId: String){
        val args = bundleOf("I" to 1,
                    MusicService.SHUFFLE_KEY to sendShuffleIfOn())
        musicController.transportControls.playFromMediaId(titleId, args)
        activity.showBar()
    }

    private fun sendQueueIfAvailable(){
        val queue = SongDBAccess.restorePlayingQueue(activity.contentResolver)
        if(queue != null){
            sendQueueToController(queue)
        }
    }

    private fun sendQueueToController(queue: MusicList){
        val args = bundleOf("L" to queue)
        musicController.sendCommand("addQueue", args, null)
    }

    private fun sendRandomQueueToController(queue: MusicList){
        val args = bundleOf("L" to queue)
        musicController.sendCommand("addRandomQueue", args, null)
    }

    private fun sendShuffleIfOn(): Boolean
         = sharedPreferences.getBoolean(MainActivityNew.PREF_SHUFFLE,false)

    fun play(){
        if(musicController.playbackState?.state == PlaybackState.STATE_PLAYING)
            musicController.transportControls.pause()
        else
            musicController.transportControls.play()
    }

    fun next(){
        musicController.transportControls.skipToNext()
    }

    fun previous(){
        musicController.transportControls.skipToPrevious()
    }

    fun showAlbumTitles(albumid: String) {
        mbrowser.subscribe("ALBUM-$albumid", subscriptionCallback)
    }

    fun playFromTitle(titleid: String){
        musicController.transportControls.playFromMediaId(titleid, null)
        sendQueueToController(MedialistsObservable.getTitleList())
        activity.showBar()
    }

    fun shuffleTitles(){
        playFirstTitleRandomly()
        sendRandomQueueToController(MedialistsObservable.getTitleList())
        activity.showBar()
    }

    private fun playFirstTitleRandomly() {
        val i = Random().nextInt(MedialistsObservable.getTitleList().countItems())
        val titleid = MedialistsObservable.getTitleList().getItemAt(i).id
        val args = bundleOf(MusicService.SHUFFLE_KEY to true)
        musicController.transportControls.playFromMediaId(titleid, args)
    }

    fun clear(){
        saveState()
        mbrowser.disconnect()
    }

    private fun saveState(){
        if(musicController.playbackState?.state != PlaybackState.STATE_PLAYING){
            saveQueue()
        } else {
            sharedPreferences.edit().putBoolean(MainActivityNew.PREF_PLAYING, false).apply()
        }
        sharedPreferences.edit()
                .putBoolean(MainActivityNew.PREF_SHUFFLE, PlaybackObservable.getShuffleOn()).apply()
    }

    private fun saveQueue(){
        if(!PlaybackObservable.getQueueLimited30().isEmpty()) {
            SongDBAccess.savePlayingQueue(activity.contentResolver,
                    PlaybackObservable.getQueue(), PlaybackObservable.getMetadata())
        }
        sharedPreferences.edit().putBoolean(MainActivityNew.PREF_PLAYING, true).apply()
    }

    fun menu(itemId: Int){
        when (itemId){
            R.id.menu_remove_playing -> stopAndRemovePlaying()
            R.id.menu_shuffle_all -> shuffleAll()
            R.id.menu_add_lyrics -> addLyrics()
        }
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
        activity.recreate()
    }

    private fun stopAndRemovePlaying(){
        if(musicController.playbackState?.state != PlaybackState.STATE_STOPPED){
            musicController.transportControls.stop()
        }
        activity.contentResolver.delete(SongContentProvider.CONTENT_URI, null, null)
        sharedPreferences.edit().putBoolean(MainActivityNew.PREF_PLAYING, false).apply()
    }

    private fun shuffleAll(){
        musicController.sendCommand("addAll", null, null)
        activity.showBar()
    }

    private fun addLyrics() {
        val dialog = LyricsAddingDialog()
        dialog.show(activity.supportFragmentManager, "LyricsAddingDialog")
    }

    inner class BrowserConnectionCallback: MediaBrowser.ConnectionCallback(){

        override fun onConnected() {
            val token = mbrowser.sessionToken
            activity.mediaController = MediaController(activity, token)
            musicController = activity.mediaController
            musicController.registerCallback(controllerCallback)
            mbrowser.subscribe(mbrowser.root, subscriptionCallback)
            launch(UI) {
                restoreSavedState()
            }
        }

        override fun onConnectionFailed() {
            Log.e(TAG, "Connection to service failed")
        }

        override fun onConnectionSuspended() {
            Log.w(TAG, "onConnectionSuspended")
            if(activity.mediaController != null){
                activity.mediaController.unregisterCallback(controllerCallback)
                activity.mediaController = null
                
            }
        }
    }
}