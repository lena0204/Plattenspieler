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
import com.lk.musicservicelibrary.R
import com.lk.musicservicelibrary.models.*
import com.lk.musicservicelibrary.system.*

/**
 * Erstellt von Lena am 02.09.18.
 */
class MusicService : MediaBrowserService() {

    private val TAG = MusicService::class.java.simpleName
    private val NOTIFICATION_ID = 9880

    private lateinit var musicFileRepo: MusicFileRepository
    private lateinit var session: MediaSession
    private lateinit var actionsCallback: MusicActionsCallback

    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: MusicNotificationBuilder
    private lateinit var nbReceiver: NotificationActionReceiver

    private var serviceStarted = false

    companion object {
        const val ACTION_MEDIA_PLAY = "com.lk.pl-ACTION_MEDIA_PLAY"
        const val ACTION_MEDIA_PAUSE = "com.lk.pl-ACTION_MEDIA_PAUSE"
        const val ACTION_MEDIA_NEXT = "com.lk.pl-ACTION_MEDIA_NEXT"
        const val SHUFFLE_KEY = "shuffle"
        var PLAYBACK_STATE = PlaybackState.STATE_STOPPED

    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        initializeComponents()
        registerBroadcastReceiver()
        prepareSession()
        actionsCallback.createInitialData()
    }

    private fun initializeComponents() {
        musicFileRepo = MusicFileRepository(this.applicationContext)
        val metadataRepo = MetadataRepository(musicFileRepo)
        actionsCallback = MusicActionsCallback(this, metadataRepo)
        notificationManager = this.getSystemService<NotificationManager>() as NotificationManager
        notificationBuilder = MusicNotificationBuilder(this)
        nbReceiver = NotificationActionReceiver(actionsCallback)
        val am = this.getSystemService<AudioManager>() as AudioManager
        AudioFocusRequester.setup(actionsCallback.audioFocusChanged, am)
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
        session.setCallback(actionsCallback)
        session.setQueueTitle(getString(R.string.queue_title))
        sessionToken = session.sessionToken
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?)
            : MediaBrowserService.BrowserRoot {
        if (this.packageName == clientPackageName) {
            return MediaBrowserService.BrowserRoot(MusicFileRepository.ROOT_ID, null)
        }
        return MediaBrowserService.BrowserRoot("", null)
    }

    override fun onLoadChildren(parentId: String,
                         result: MediaBrowserService.Result<MutableList<MediaBrowser.MediaItem>>) {
        when {
            parentId == MusicFileRepository.ROOT_ID ->
                result.sendResult(musicFileRepo.getAlbums().getMediaItemList())
            parentId.contains("ALBUM-") ->
                result.sendResult(getTitles(parentId))
            else -> Log.e(TAG, "No known parent ID")
        }
    }

    private fun getTitles(albumId: String): MutableList<MediaBrowser.MediaItem> {
        val id = albumId.replace("ALBUM-", "")
        return musicFileRepo.getTitlesForAlbumID(albumid = id)
                .getMediaItemList()
    }

    fun startServiceIfNecessary() {
        if (!serviceStarted) {
            this.startService(android.content.Intent(applicationContext,
                    com.lk.musicservicelibrary.main.MusicService::class.java))
            serviceStarted = true
        }
        if (!session.isActive)
            session.isActive = true
    }

    fun stopService() {
        Log.d(TAG, "handleOnStop in service")
        this.stopSelf()
        serviceStarted = false
        this.stopForeground(true)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        val bool = super.onUnbind(intent)
        if (session.controller.playbackState?.state == PlaybackState.STATE_PAUSED) {
            actionsCallback.onStop()
        }
        return bool
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        this.unregisterReceiver(nbReceiver)
        session.release()
        session.isActive = false
    }

    val onDataChanged = fun(playbackData: PlaybackData) {
        val (metadata, playbackState, queue) = playbackData
        metadata.cover = MusicMetadata.decodeAlbumcover(metadata.cover_uri, resources)
        session.setMetadata(metadata.getMediaMetadata())
        session.setPlaybackState(playbackState)
        session.setQueue(queue.getQueueItemList())
        PLAYBACK_STATE = playbackState.state
        sendBroadcastForLightningLauncher(metadata)
        val shuffleOn = playbackState.extras?.getBoolean("S") ?: false
        launchNotification(metadata, shuffleOn)
    }

    private fun launchNotification(metadata: MusicMetadata, shuffleOn: Boolean) {
        when (PLAYBACK_STATE) {
            PlaybackState.STATE_PLAYING, PlaybackState.STATE_PAUSED -> {
                notificationManager.notify(NOTIFICATION_ID,
                        notificationBuilder.showNotification(PLAYBACK_STATE, metadata, shuffleOn))
            }
            PlaybackState.STATE_STOPPED -> {
                notificationManager.cancel(NOTIFICATION_ID)
                stopService()
            }
        }
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
}