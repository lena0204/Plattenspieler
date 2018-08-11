package com.lk.music_service_library.background

import android.media.browse.MediaBrowser
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.ResultReceiver
import android.service.media.MediaBrowserService
import android.util.Log
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.*
import android.os.Build
import com.lk.music_service_library.R
import com.lk.music_service_library.models.MusicList
import com.lk.music_service_library.models.MusicMetadata
import com.lk.music_service_library.utils.EnumPlaybackState

/**
 * Created by Lena on 08.06.17.
 * Verwaltet die Callbacks (MediaBrowser und MediaSession) zum Client, Zugriff auf den MusicProvider
 * und auf das Playback, Entgegennahme der Broadcastnachrichten
 */
class MusicService: MediaBrowserService()  {

    private val TAG = "com.lk.pl-MusicService"

    private lateinit var msession: MediaSession
    private lateinit var playback: MusicPlayback

    private val nbReceiver = NotificationBroadcastReceiver()
    private var serviceStarted = false
    var playingstate = EnumPlaybackState.STATE_STOP

    companion object {
        const val ACTION_MEDIA_PLAY = "com.lk.pl-ACTION_MEDIA_PLAY"
        const val ACTION_MEDIA_PAUSE = "com.lk.pl-ACTION_MEDIA_PAUSE"
        const val ACTION_MEDIA_NEXT = "com.lk.pl-ACTION_MEDIA_NEXT"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        prepareSession()
        initializeMusicDataForSession()
        playback = MusicPlayback(this, MusicNotification(this))
    }

    private fun prepareSession(){
        msession = MediaSession(applicationContext, TAG)
        registerBroadcast()
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            msession.setFlags(MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
        }
        msession.setCallback(MusicSessionCallback())
        sessionToken = msession.sessionToken
    }

    private fun registerBroadcast(){
        val ifilter = IntentFilter()
        ifilter.addAction(MusicService.ACTION_MEDIA_PLAY)
        ifilter.addAction(MusicService.ACTION_MEDIA_PAUSE)
        ifilter.addAction(MusicService.ACTION_MEDIA_NEXT)
        this.registerReceiver(nbReceiver, ifilter)
    }

    private fun initializeMusicDataForSession(){
        val playbackState = PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY
                        or PlaybackState.ACTION_PLAY_FROM_MEDIA_ID)
        msession.setPlaybackState(playbackState.build())
        msession.setMetadata(MediaMetadata.Builder().build())
        msession.setQueueTitle(getString(R.string.queue_title))
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): MediaBrowserService.BrowserRoot? {
        if(this.packageName == clientPackageName){
            return MediaBrowserService.BrowserRoot(MusicProvider.ROOT_ID, null)
        }
        return MediaBrowserService.BrowserRoot("", null)
    }

    override fun onLoadChildren(parentId: String, result: MediaBrowserService.Result<MutableList<MediaBrowser.MediaItem>>) {
        when {
            parentId == MusicProvider.ROOT_ID -> result.sendResult(playback.sendRootChildren())
            parentId.contains("ALBUM-") -> result.sendResult(playback.sendAlbumChildren(parentId))
			else -> Log.e(TAG, "No known parent ID")
		}
    }

    fun startServiceIfNecessary(){
        if(!serviceStarted){
            this.startService(android.content.Intent(applicationContext,
                    com.lk.music_service_library.background.MusicService::class.java))
            serviceStarted = true
        }
        if(!msession.isActive) { msession.isActive = true }
    }

    fun setMetadataToSession(data: MusicMetadata){
        // WICHTIG_ Cover auf dem Lockscreen erfordert, dass ein Bitmap in den Metadaten vorhanden ist!!
        data.cover = decodeAlbumcover(data.cover_uri)
        msession.setMetadata(data.getMediaMetadata())
    }

    fun decodeAlbumcover(path: String): Bitmap{
        var albumart: Bitmap?
        albumart = BitmapFactory.decodeFile(path)
        if (albumart == null) {
            albumart = BitmapFactory.decodeResource(resources, R.mipmap.ic_no_cover)
        }
        return albumart
    }

    fun setPlaybackStateToSession(state: PlaybackState){
        msession.setPlaybackState(state)
    }

    fun setQueueToSession(queue: MusicList){
        msession.setQueue(queue.getQueueItemList())
    }

    fun stopService(){
        Log.d(TAG, "handleOnStop Service")
        this.stopSelf()
        serviceStarted = false
        this.stopForeground(true)

    }

    override fun onUnbind(intent: Intent?): Boolean {
        val bool = super.onUnbind(intent)
        if(msession.controller.playbackState.state == PlaybackState.STATE_PAUSED) {
            playback.handleOnStop()
        }
        Log.v(TAG, "Service started: " + this.serviceStarted)
        return bool
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        this.unregisterReceiver(nbReceiver)
        msession.release()
        msession.isActive = false
    }

    inner class MusicSessionCallback: MediaSession.Callback(){
        override fun onPlay() {
            playback.handleOnPlay()
        }

        override fun onPlayFromMediaId(mediaId: String, extras: Bundle?) {
            playback.shuffleOn = false
            Log.d(TAG, "onPlayfromid")
            if(extras != null && extras.containsKey("I")){
                // Callback onPrepareFromId ist erst ab API 24 möglich
                playback.handleOnPrepareFromId(mediaId)
            } else {
                playback.handleOnPlayFromId(mediaId)
            }
        }

        override fun onPause() {
            playback.handleOnPause()
        }
        override fun onStop() {
            playback.handleOnStop()
        }
        override fun onSkipToNext() {
            playback.handleOnNext()
        }
        override fun onSkipToPrevious() {
            playback.handleOnPrevious(msession.controller.playbackState.position)
        }

        override fun onMediaButtonEvent(mediaButtonIntent: Intent?): Boolean{
            Log.i(TAG, "onMediaButtonEvent" + mediaButtonIntent?.action
					+ ";" + mediaButtonIntent?.data + ";" + mediaButtonIntent?.`package`)
            return super.onMediaButtonEvent(mediaButtonIntent)
        }

        override fun onCommand(command: String, args: Bundle?, resultReceiver: ResultReceiver?) {
            when(command){
                "addQueue" -> addQueueToService(args)
                "addAll" -> playback.addAllSongsToPlayingQueue()
                "shuffle" -> playback.shuffleOn = true
            }
        }

        private fun addQueueToService(args: Bundle?){
            if(args != null) {
                args.classLoader = this.javaClass.classLoader
                val list = args.getParcelable<MusicList>("L")
                msession.setQueue(list.getQueueItemList())
                playback.setQueue(list)
            }
        }
    }

    inner class NotificationBroadcastReceiver: BroadcastReceiver(){

        override fun onReceive(context: Context?, intent: Intent?) {
            if(intent != null){
                Log.d(TAG, "onReceive: " + intent.action + "; " + playingstate)
                when(intent.action){
                    ACTION_MEDIA_PLAY -> handlePlayIntent()
                    ACTION_MEDIA_PAUSE -> handlePauseIntent()
                    ACTION_MEDIA_NEXT -> handleNextIntent()
                    else -> Log.d(TAG, "Neuer Intent mit Action:${intent.action}")
                }
            }
        }

        private fun handlePlayIntent(){
            if(playingstate == EnumPlaybackState.STATE_PAUSE){
                playback.handleOnPlay()
                playingstate = EnumPlaybackState.STATE_PLAY
                // Log.d(TAG, "PLAY: playingstate: " + playingstate + ", Playbackstate:" + msession.controller.playbackState.state)
            }
        }

        private fun handlePauseIntent(){
            if(playingstate == EnumPlaybackState.STATE_PLAY){
                playback.handleOnPause()
                playingstate = EnumPlaybackState.STATE_PAUSE
                //Log.d(TAG, "PAUSE: playingstate: " + playingstate + ", Playbackstate:" + msession.controller.playbackState.state)

            }
        }

        private fun handleNextIntent(){
            if(playingstate == EnumPlaybackState.STATE_PLAY || playingstate == EnumPlaybackState.STATE_PAUSE) {
                playback.handleOnNext()
                playingstate = EnumPlaybackState.STATE_NEXT
                //Log.d(TAG, "NEXT: playingstate: " + playingstate + ", Playbackstate:" + msession.controller.playbackState.state)
            }
        }
    }
}