package com.lk.musicservicelibrary.main

import android.app.NotificationManager
import android.content.*
import android.media.AudioManager
import android.media.browse.MediaBrowser
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.service.media.MediaBrowserService
import android.util.Log
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import com.lk.musicservicelibrary.R
import com.lk.musicservicelibrary.models.*
import com.lk.musicservicelibrary.playback.PlaybackCallback
import com.lk.musicservicelibrary.system.*

/**
 * Erstellt von Lena am 02.09.18.
 * MediaBrowserService; zusammen mit PlaybackController (ControllerCallback)
 * mögliche Controller-Aktionen: playFromId, play, pause, next, previous, stop, Command: addAll (als shuffle)
 */
class MusicService : MediaBrowserService(), Observer<Any> {

    private val TAG = MusicService::class.java.simpleName
    private val NOTIFICATION_ID = 9880

    private lateinit var musicDataRepo: MusicDataRepository
    private lateinit var session: MediaSession
    private lateinit var sessionCallback: PlaybackCallback

    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: MusicNotificationBuilder
    private lateinit var nbReceiver: NotificationActionReceiver

    private var serviceStarted = false

    companion object {
        const val ACTION_MEDIA_PLAY = "com.lk.pl-ACTION_MEDIA_PLAY"
        const val ACTION_MEDIA_PAUSE = "com.lk.pl-ACTION_MEDIA_PAUSE"
        const val ACTION_MEDIA_NEXT = "com.lk.pl-ACTION_MEDIA_NEXT"
        const val SHUFFLE_KEY = "shuffle"
        const val QUEUE_KEY = "queue"
        var PLAYBACK_STATE = PlaybackState.STATE_STOPPED
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        initializeComponents()
        registerBroadcastReceiver()
        prepareSession()
        registerPlaybackObserver()
    }

    private fun initializeComponents() {
        musicDataRepo = LocalMusicFileRepository(this.applicationContext)
        sessionCallback = PlaybackCallback(musicDataRepo)
        notificationManager = this.getSystemService<NotificationManager>() as NotificationManager
        notificationBuilder = MusicNotificationBuilder(this)
        nbReceiver = NotificationActionReceiver(sessionCallback)
        val am = this.getSystemService<AudioManager>() as AudioManager
        // AudioFocusRequester.setup(sessionCallback.audioFocusChanged, am)
    }

    private fun registerBroadcastReceiver() {
        val ifilter = IntentFilter()
        ifilter.addAction(MusicService.ACTION_MEDIA_PLAY)
        ifilter.addAction(MusicService.ACTION_MEDIA_PAUSE)
        ifilter.addAction(MusicService.ACTION_MEDIA_NEXT)
        this.registerReceiver(nbReceiver, ifilter)
    }

    private fun prepareSession() {
        session = MediaSession(applicationContext, TAG)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            session.setFlags(MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
        }
        session.setCallback(sessionCallback)
        session.setQueueTitle(getString(R.string.queue_title))
        sessionToken = session.sessionToken
    }

    private fun registerPlaybackObserver() {
        sessionCallback.getPlaybackState().observeForever(this)
        sessionCallback.getPlayingList().observeForever(this)
    }

    private fun startServiceIfNecessary() {
        if (!serviceStarted) {
            this.startService(android.content.Intent(applicationContext,
                com.lk.musicservicelibrary.main.MusicService::class.java))
            Log.d(TAG, "started service")
            serviceStarted = true
        }
        if (!session.isActive)
            session.isActive = true
    }


    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?)
            : MediaBrowserService.BrowserRoot {
        if (this.packageName == clientPackageName) {
            return MediaBrowserService.BrowserRoot(MusicDataRepository.ROOT_ID, null)
        }
        return MediaBrowserService.BrowserRoot("", null)
    }

    override fun onLoadChildren(parentId: String,
                         result: MediaBrowserService.Result<MutableList<MediaBrowser.MediaItem>>) {
        when {
            parentId == MusicDataRepository.ROOT_ID -> {
                result.sendResult(musicDataRepo.queryAlbums().getMediaItemList())
            }
            parentId.contains("ALBUM-") -> {
                result.sendResult(getTitles(parentId))
            }
            else -> Log.e(TAG, "No known parent ID")
        }
    }

    private fun getTitles(albumId: String): MutableList<MediaBrowser.MediaItem> {
        val id = albumId.replace("ALBUM-", "")
        val playingList = musicDataRepo.queryTitlesByAlbumID(id)
        sessionCallback.setQueriedMediaList(playingList)
        return playingList.getMediaItemList()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        val bool = super.onUnbind(intent)
        Log.v(TAG, "onUnbind")
        val state = session.controller.playbackState?.state
        if (state == PlaybackState.STATE_PAUSED) {
            Log.d(TAG, "Playback paused ($state), so stop")
            sessionCallback.onStop()
        }
        return bool
    }

    private fun stopService() {
        Log.d(TAG, "stopService in service")
        this.stopSelf()
        serviceStarted = false
        this.stopForeground(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        sessionCallback.onStop()
        this.unregisterReceiver(nbReceiver)
        unregisterPlaybackObserver()
        session.release()
        session.isActive = false
    }

    private fun unregisterPlaybackObserver(){
        sessionCallback.getPlayingList().removeObserver(this)
        sessionCallback.getPlaybackState().removeObserver(this)
    }

    // - - - Changes handler - - -

    override fun onChanged(update: Any?) {
        if(update is MusicList) {
            updatePlayingList(update)
        } else if (update is PlaybackState) {
            updatePlaybackState(update)
        }
    }

    private fun updatePlayingList(playingList: MusicList) {
        val playingTitle = playingList.getItemAtCurrentPlaying()
        val shortQueue = getRealQueue(playingList)
        if(playingTitle != null ){
            playingTitle.nr_of_songs_left = shortQueue.count().toLong()
            session.setMetadata(playingTitle.getMediaMetadata())
            sendBroadcastForLightningLauncher(playingTitle)
        }
        session.setQueue(shortQueue.getQueueItemList())
    }

    private fun sendBroadcastForLightningLauncher(metadata: MusicMetadata) {
        val track = bundleOf(
            "title" to metadata.title,
            "album" to metadata.album,
            "artist" to metadata.artist)
        val extras = bundleOf(
            "track" to track,
            "aaPath" to metadata.cover_uri)
        this.sendBroadcast(Intent("com.lk.plattenspieler.metachanged").putExtras(extras))
    }

    private fun updatePlaybackState(state: PlaybackState) {
        PLAYBACK_STATE = state.state
        session.setPlaybackState(state)
        updateNotification(state)

        if(PLAYBACK_STATE == PlaybackState.STATE_PAUSED){
            this.stopForeground(false)
        }
    }

    private fun getRealQueue(playingList: MusicList) : MusicList {
        val shortedQueue = MusicList()
        val firstAfterPlaying = playingList.getCurrentPlaying() + 1
        val lastItem = playingList.count() - 1
        for (i in firstAfterPlaying until lastItem) {
            shortedQueue.addItem(playingList.getItemAt(i))
        }
        return shortedQueue
    }

    private fun updateNotification(state: PlaybackState) {
        val shuffleOn = state.extras?.getBoolean(SHUFFLE_KEY) ?: false
        val playing = PLAYBACK_STATE ==  PlaybackState.STATE_PLAYING
        val metadata = if(session.controller.metadata != null) {
            MusicMetadata.createFromMediaMetadata(session.controller.metadata!!)
        } else {
            MusicMetadata()
        }
        launchNotification(metadata, shuffleOn, playing)
    }

    private fun launchNotification(metadata: MusicMetadata, shuffleOn: Boolean, startInForeground: Boolean) {
        when (PLAYBACK_STATE) {
            PlaybackState.STATE_PLAYING, PlaybackState.STATE_PAUSED -> {
                val noti = notificationBuilder.showNotification(PLAYBACK_STATE, metadata, shuffleOn)
                if(startInForeground){
                    startServiceIfNecessary()
                    this.startForeground(NOTIFICATION_ID, noti)
                } else {
                    notificationManager.notify(NOTIFICATION_ID, noti)
                }
            }
            PlaybackState.STATE_STOPPED -> {
                notificationManager.cancel(NOTIFICATION_ID)
                stopService()
            }
        }
    }
}