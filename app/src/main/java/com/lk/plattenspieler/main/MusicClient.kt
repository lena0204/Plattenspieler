package com.lk.plattenspieler.main

import android.content.*
import android.media.MediaMetadata
import android.media.browse.MediaBrowser
import android.media.session.*
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import com.lk.music_service_library.background.MusicService
import com.lk.music_service_library.database.*
import com.lk.music_service_library.models.*
import com.lk.music_service_library.utils.QueueCreation
import com.lk.plattenspieler.R
import com.lk.plattenspieler.fragments.LyricsAddingDialog
import com.lk.plattenspieler.utils.EnumTheme
import com.lk.plattenspieler.utils.ThemeChanger
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.UI
import java.util.*

/**
 * Erstellt von Lena am 12.05.18.
 * Verwaltet Aktionen vom Menü und den Zugriff inkl. Callbacks auf den MediaBrowserService (MusicService),
 * Gibt Updates an die Observables weiter zum verteilen
 */
class MusicClient(val activity: MainActivityNew) {

    private val TAG = "MusicClient"
    private val connectionCallback = BrowserConnectionCallback()
    val subscriptionCallback = MusicSubscriptionCallback()
    val controllerCallback = MusicControllerCallback()

    private var restoringQueue = false
    private var shuffleOn = false
    private var sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)

    private lateinit var mbrowser: MediaBrowser
    private lateinit var musicController: MediaController
    
    fun completeSetup(restoringQueue: Boolean){
        this.restoringQueue = restoringQueue
        Log.v(TAG, "restore: $restoringQueue")
        // Setup MediaBrowser
        val c = ComponentName(activity.applicationContext, MusicService::class.java)
        mbrowser = MediaBrowser(activity, c, connectionCallback, null)
        mbrowser.connect()
    }
    fun clear(){
        saveQueue()
        musicController.unregisterCallback(controllerCallback)
        mbrowser.disconnect()
    }

    fun play(){
        Log.v(TAG, "play()")
        if(musicController.playbackState.state == PlaybackState.STATE_PLAYING){
            musicController.transportControls.pause()
        } else {
            musicController.transportControls.play()
        }
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
        Log.v(TAG, "playFromTitle:$titleid.")
        musicController.transportControls.playFromMediaId(titleid, null)
        // Queue erstellen und weiterleiten
        val queue = QueueCreation.createQueueFromTitle(titleid, MedialistObservable.getMediaList())
        sendQueueToController(queue)
        shuffleOn = false
        PlaybackObservable.setState(MusicPlaybackState((shuffleOn)))
        activity.showBar()
    }
    fun shuffleTitles(){
        val medialist = MedialistObservable.getMediaList()
        // ersten zufälligen heraussuchen
        val i = Random().nextInt(medialist.countItems())
        val titleid = medialist.getItemAt(i).id
        Log.v(TAG, "shuffleTitles")
        musicController.transportControls.playFromMediaId(titleid, null)
        // Queue erstellen und weitergeben, sowie shuffle
        val queue = QueueCreation.createQueueRandom(medialist, titleid)
        sendQueueToController(queue)
        musicController.sendCommand("shuffle", null, null)
        shuffleOn = true
        PlaybackObservable.setState(MusicPlaybackState(shuffleOn))
        activity.showBar()
    }
    
    private fun saveQueue(){
        if(musicController.playbackState.state != PlaybackState.STATE_PLAYING){
            if(!PlaybackObservable.getQueue().isEmpty()) {
                SongDBAccess.savePlayingQueue(activity.contentResolver,
                        PlaybackObservable.getQueueAll(), PlaybackObservable.getMetadata())
            }
            sharedPreferences.edit().putBoolean(MainActivityNew.PREF_PLAYING, true).apply()
        } else {
            Log.v(TAG, "saveQueue, false")
            sharedPreferences.edit().putBoolean(MainActivityNew.PREF_PLAYING, false).apply()
        }
        sharedPreferences.edit().putBoolean(MainActivityNew.PREF_SHUFFLE, shuffleOn).apply()
    }
    fun restoreQueue(){
        // Playlist wiederherstellen und UI Kontrolle herstellen
        // shuffle auslesen
        val shuffleOn = sharedPreferences
                .getBoolean(MainActivityNew.PREF_SHUFFLE,false)
        PlaybackObservable.setState(MusicPlaybackState(shuffleOn, PlaybackState.STATE_PAUSED))
        val state = musicController.playbackState.state
        if(state != PlaybackState.STATE_PLAYING &&
                state != PlaybackState.STATE_PAUSED){
            if( restoringQueue){
                val music = SongDBAccess.restoreFirstQueueItem(activity.contentResolver)
                if(music == null){
                    Log.d(TAG, "Cursor ist null oder leer")
                    sharedPreferences.edit().putBoolean(MainActivityNew.PREF_PLAYING, false).apply()
                } else {
                    /*val music = queue.removeItemAt(0)*/
                    //val music = queue.getItemAt(0)
                    PlaybackObservable.setMetadata(music)
                    val args = Bundle()
                    args.putInt("I", 1)
                    musicController.transportControls.playFromMediaId(music.id, args)
                    activity.showBar()
                }
                val queue = SongDBAccess.restorePlayingQueue(activity.contentResolver)
                if(queue != null){
                    sendQueueToController(queue)
                    Log.d(TAG, "Hat queue geladen")
                }
            }
        } else {
            // Session spielt Musik ab, Daten holen und weiterleiten
            PlaybackObservable.setQueue(MusicList.createListFromQueue(musicController.queue))
            PlaybackObservable.setMetadata(MusicMetadata.createFromMediaMetadata(musicController.metadata))
            if(state == PlaybackState.STATE_PLAYING)
                PlaybackObservable.setState(MusicPlaybackState(shuffleOn, PlaybackState.STATE_PLAYING))
            else
                PlaybackObservable.setState(MusicPlaybackState(shuffleOn, PlaybackState.STATE_PAUSED))
            activity.showBar()
        }
        if(shuffleOn) {
            musicController.sendCommand("shuffle", null, null)
        }
    }

    private fun sendQueueToController(queue: MusicList){
        PlaybackObservable.setQueue(queue)
        val args = Bundle()
        args.putParcelable("L", queue)
        musicController.sendCommand("addQueue", args, null)
    }
    
    fun menu(itemId: Int){
        when (itemId){
            R.id.menu_change_design -> changeDesign()
            R.id.menu_remove_playing -> stopAndRemovePlaying()
            R.id.menu_dark_light -> changeLightDark()
            R.id.menu_shuffle_all -> shuffleAll()
            R.id.menu_show_lyrics -> changeLyricsState()
            R.id.menu_theme_lineage -> applyTheme(EnumTheme.THEME_LINEAGE)
            R.id.menu_add_lyrics -> addLyrics()
        /*R.id.menu_delete_database -> {
            // DEBUGGING: Alte Dateien löschen und die aktuelle Wiedergabe in die Datenbank schreiben
            val number = contentResolver.delete(SongContentProvider.CONTENT_URI, null, null)
            Log.d(TAG, "Anzahl der gelöschten Zeilen: " + number)
        }*/
        }
    }
    private fun changeDesign(){
        var design = ThemeChanger.readThemeFromPreferences(sharedPreferences)
        Log.d(TAG, "Farbe: $design")
        design = when(design){
            EnumTheme.THEME_LIGHT -> EnumTheme.THEME_LIGHT_T
            EnumTheme.THEME_DARK -> EnumTheme.THEME_DARK_T
            EnumTheme.THEME_LIGHT_T, EnumTheme.THEME_LINEAGE -> EnumTheme.THEME_LIGHT
            EnumTheme.THEME_DARK_T -> EnumTheme.THEME_DARK
        }
        applyTheme(design)
    }
    private fun changeLightDark(){
        var design = ThemeChanger.readThemeFromPreferences(sharedPreferences)
        Log.d(TAG, "Hell/Dunkel: $design")
        design = when(design){
            EnumTheme.THEME_LIGHT -> EnumTheme.THEME_DARK
            EnumTheme.THEME_DARK, EnumTheme.THEME_LINEAGE -> EnumTheme.THEME_LIGHT
            EnumTheme.THEME_LIGHT_T -> EnumTheme.THEME_DARK_T
            EnumTheme.THEME_DARK_T -> EnumTheme.THEME_LIGHT_T
        }
        applyTheme(design)
    }
    fun applyTheme(design: EnumTheme){
        ThemeChanger.writeThemeToPreferences(sharedPreferences, design)
        if(design == EnumTheme.THEME_LINEAGE){
            // Permission abfragen
            if(activity.requestDesignReadPermission()){
                saveQueue()
                activity.recreate()
            }
        } else {
            saveQueue()
            activity.recreate()
        }
    }
    private fun stopAndRemovePlaying(){
        // Wiedergabe stoppen, falls nötig; löscht automatisch die Queue und den Player etc
        if(musicController.playbackState.state != PlaybackState.STATE_STOPPED){
            musicController.transportControls.stop()
        }
        // Datenbank löschen
        activity.contentResolver.delete(SongContentProvider.CONTENT_URI, null, null)
        sharedPreferences.edit().putBoolean(MainActivityNew.PREF_PLAYING, false).apply()
    }
    fun shuffleAll(){
        Log.d(TAG, "shuffleAll")
        musicController.sendCommand("addAll", null, null)
        shuffleOn = true
        PlaybackObservable.setState(MusicPlaybackState(shuffleOn))
        activity.showBar()
    }
    private fun changeLyricsState(){
        val pref = sharedPreferences.getInt(MainActivityNew.PREF_LYRICS,0)
        if(pref == 0){
            sharedPreferences.edit().putInt(MainActivityNew.PREF_LYRICS, 1).apply()
        } else {
            sharedPreferences.edit().putInt(MainActivityNew.PREF_LYRICS, 0).apply()
        }
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
            musicController.registerCallback(controllerCallback)
            // Daten abfragen
            mbrowser.subscribe(mbrowser.root, subscriptionCallback)
            Log.v(TAG, "call restoreQueue()")
            launch(UI) {
                restoreQueue()
            }
            Log.v(TAG, "weitermachen")
        }

        override fun onConnectionFailed() {
            Log.e(TAG, "Connection to service failed")
        }

        override fun onConnectionSuspended() {
            Log.v(TAG, "onConnectionSuspended")
            if(activity.mediaController != null){
                activity.mediaController.unregisterCallback(controllerCallback)
                activity.mediaController = null
            }
        }
    }
    inner class MusicSubscriptionCallback: MediaBrowser.SubscriptionCallback(){

        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowser.MediaItem>) {
            val medialist = MusicList()
            Log.v(TAG, "onCildrenLoaded")
            if(parentId == mbrowser.root){
                medialist.addFlag(MediaBrowser.MediaItem.FLAG_BROWSABLE)
                // Basisabfrage auf die Alben
                for(i in children){
                    medialist.addItem(MusicMetadata.createFromMediaDescription(i.description))
                }
                MedialistObservable.setAlbums(medialist)
            } else if(parentId.contains("ALBUM-")){
                // ein Album wurde abgefragt
                medialist.addFlag(MediaBrowser.MediaItem.FLAG_PLAYABLE)
                for(i in children){
                    medialist.addItem(MusicMetadata.createFromMediaDescription(i.description))
                }
                MedialistObservable.setMediaList(medialist)
            }
            mbrowser.unsubscribe(parentId)
        }


    }
    inner class MusicControllerCallback: MediaController.Callback(){

        override fun onQueueChanged(queue: MutableList<MediaSession.QueueItem>) {
            super.onQueueChanged(queue)
            Log.v(TAG, "onQueueChanged: Länge: " + queue.count())
            PlaybackObservable.setQueue(MusicList.createListFromQueue(queue))
        }

        override fun onMetadataChanged(metadata: MediaMetadata) {
            super.onMetadataChanged(metadata)
            Log.v(TAG, "OnMetadataChanged")
            PlaybackObservable.setMetadata(MusicMetadata.createFromMediaMetadata(metadata))
        }
        override fun onPlaybackStateChanged(state: PlaybackState) {
            super.onPlaybackStateChanged(state)
            Log.v(TAG, "onPlaybackstateChanged")
            // update Bar
            if(state.extras != null){
                shuffleOn = state.extras.getBoolean("shuffle")
                PlaybackObservable.setState(MusicPlaybackState(shuffleOn, state.state))
            } else {
                PlaybackObservable.setState(MusicPlaybackState(state = state.state))
            }
            if(state.state == PlaybackState.STATE_STOPPED){
                PlaybackObservable.setMetadata(MusicMetadata())
                PlaybackObservable.setQueue(MusicList())
                activity.hideBar()
            }
        }
    }
}