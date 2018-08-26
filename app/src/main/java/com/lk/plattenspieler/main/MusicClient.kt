package com.lk.plattenspieler.main

import android.content.*
import android.media.browse.MediaBrowser
import android.media.session.*
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import com.lk.music_service_library.background.MusicService
import com.lk.music_service_library.database.*
import com.lk.music_service_library.models.*
import com.lk.music_service_library.observables.*
import com.lk.music_service_library.utils.QueueCreation
import com.lk.plattenspieler.R
import com.lk.plattenspieler.fragments.LyricsAddingDialog
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
        val state = musicController.playbackState.state
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
        val args = Bundle()
        args.putInt("I", 1)
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
        val args = Bundle()
        args.putParcelable("L", queue)
        musicController.sendCommand("addQueue", args, null)
    }

    private fun sendShuffleIfOn(){
        val shuffleOn = sharedPreferences.getBoolean(MainActivityNew.PREF_SHUFFLE,false)
        if(shuffleOn) {
            musicController.sendCommand("shuffle", null, null)
        }
    }

    fun play(){
        if(musicController.playbackState.state == PlaybackState.STATE_PLAYING)
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
        val queue = QueueCreation.createQueueFromTitle(titleid, MedialistsObservable.getTitleList())
        sendQueueToController(queue)
        activity.showBar()
    }

    fun shuffleTitles(){
        val medialist = MedialistsObservable.getTitleList()
        val titleid = playFirstTitleRandomly()
        val queue = QueueCreation.createRandomQueue(medialist, titleid)
        sendQueueToController(queue)
        musicController.sendCommand("shuffle", null, null)
        activity.showBar()
    }

    private fun playFirstTitleRandomly(): String{
        val i = Random().nextInt(MedialistsObservable.getTitleList().countItems())
        val titleid = MedialistsObservable.getTitleList().getItemAt(i).id
        musicController.transportControls.playFromMediaId(titleid, null)
        return titleid
    }

    fun clear(){
        saveState()
        mbrowser.disconnect()
    }

    private fun saveState(){
        if(musicController.playbackState.state != PlaybackState.STATE_PLAYING){
            saveQueue()
        } else {
            sharedPreferences.edit().putBoolean(MainActivityNew.PREF_PLAYING, false).apply()
        }
        sharedPreferences.edit()
                .putBoolean(MainActivityNew.PREF_SHUFFLE, PlaybackDataObservable.shuffleOn).apply()
    }

    private fun saveQueue(){
        if(!PlaybackDataObservable.getQueueLimitedTo30().isEmpty()) {
            SongDBAccess.savePlayingQueue(activity.contentResolver,
                    PlaybackDataObservable.musicQueue, PlaybackDataObservable.metadata)
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
        if(musicController.playbackState.state != PlaybackState.STATE_STOPPED){
            musicController.transportControls.stop()
        }
        activity.contentResolver.delete(SongContentProvider.CONTENT_URI, null, null)
        sharedPreferences.edit().putBoolean(MainActivityNew.PREF_PLAYING, false).apply()
    }

    private fun shuffleAll(){
        // TODO shuffleAll() implementieren
        /*Log.d(TAG, "shuffleAll")
        musicController.sendCommand("addAll", null, null)
        activity.showBar()*/
    }

    private fun addLyrics() {
        val dialog = LyricsAddingDialog()
        dialog.show(activity.fragmentManager, "LyricsAddingDialog")
    }

    inner class BrowserConnectionCallback: MediaBrowser.ConnectionCallback(){

        override fun onConnected() {
            val token = mbrowser.sessionToken
            activity.mediaController = MediaController(activity, token)
            musicController = activity.mediaController
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
                activity.mediaController = null
            }
        }
    }
}