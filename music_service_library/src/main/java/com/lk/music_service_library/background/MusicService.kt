package com.lk.music_service_library.background

import android.media.browse.MediaBrowser
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.ResultReceiver
import android.service.media.MediaBrowserService
import android.util.Log
import android.content.*
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
        // Session aufsetzen (mit Provider, Token, Callback und Flags setzen)
        msession = MediaSession(applicationContext, TAG)
        registerBroadcast()
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            msession.setFlags(MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
        }
        msession.setCallback(MusicSessionCallback())
        sessionToken = msession.sessionToken
        // Playback und Metadata initialisieren (leer)
        val playbackState = PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY
                        or PlaybackState.ACTION_PLAY_FROM_MEDIA_ID)
        msession.setPlaybackState(playbackState.build())
        msession.setMetadata(MediaMetadata.Builder().build())
        // Queuetitel setzen
        msession.setQueueTitle(getString(R.string.queue_title))
        playback = MusicPlayback(this, MusicNotification(this))
    }

    override fun onUnbind(intent: Intent?): Boolean {
        val bool = super.onUnbind(intent)
        if(msession.controller.playbackState.state == PlaybackState.STATE_PAUSED) {
            playback.handleOnStop()     // speichern und aufräumen
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

    // -------------- Hierachie der Musiktitel --------------
    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): MediaBrowserService.BrowserRoot? {
        if(this.packageName == clientPackageName){  // nur eigenes package
            return MediaBrowserService.BrowserRoot(MusicProvider.ROOT_ID, null)
        }
        return MediaBrowserService.BrowserRoot("", null)
    }
    override fun onLoadChildren(parentId: String, result: MediaBrowserService.Result<MutableList<MediaBrowser.MediaItem>>) {
        // eigene Hierachie aufbauen mit Browsable und Playable MediaItems
        when{
            parentId == MusicProvider.ROOT_ID -> result.sendResult(playback.sendRootChildren())
            parentId.contains("ALBUM-") -> result.sendResult(playback.sendAlbumChildren(parentId))
			else -> Log.e(TAG, "No known parent ID")
		}
    }

    // Für die Aktionen in der Benachrichtigung
    private fun registerBroadcast(){
        val ifilter = IntentFilter()
        ifilter.addAction(MusicService.ACTION_MEDIA_PLAY)
        ifilter.addAction(MusicService.ACTION_MEDIA_PAUSE)
        ifilter.addAction(MusicService.ACTION_MEDIA_NEXT)
        this.registerReceiver(nbReceiver, ifilter)
    }

    // ----------- Methoden, die MusicPlayback aufruft----------
    fun sendMetadata(data: MusicMetadata){
        msession.setMetadata(data.getMediaMetadata())
    }
    fun sendPlaybackstate(state: PlaybackState){
        msession.setPlaybackState(state)
    }
    fun sendQueue(queue: MusicList){
        msession.setQueue(queue.getQueueItemList())
    }
    fun setupSession(){
        if(!serviceStarted){
            this.startService(android.content.Intent(applicationContext, com.lk.music_service_library.background.MusicService::class.java))
            serviceStarted = true
        }
        if(!msession.isActive) { msession.isActive = true }
    }
    fun handleOnStopService(){
        Log.d(TAG, "handleOnStop Service")
        this.stopSelf()
        serviceStarted = false
        this.stopForeground(true)

    }

    // ----------------- Callbacks und Notification ---------------
    inner class MusicSessionCallback: MediaSession.Callback(){
        override fun onPlay() {
            playback.handleOnPlay()
        }
        override fun onPlayFromMediaId(mediaId: String, extras: Bundle?) {
            playback.shuffleOn = false
            Log.d(TAG, "onPlayfromid")
            if(extras != null && extras.containsKey("I")){
                playback.handleOnPrepareFromId(mediaId) // prepareFromId ist erst ab API 24 möglich
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

        override fun onCommand(command: String, args: Bundle?, cb: ResultReceiver?) {
            when(command){
                "addQueue" -> {
                    if(args != null) {
                        args.classLoader = this.javaClass.classLoader
                        val list = args.getParcelable<MusicList>("L")
                        msession.setQueue(list.getQueueItemList())
                        playback.setQueue(list)
                    }
                }
                "addAll" -> playback.addAllSongsToPlayingQueue()
                "shuffle" -> {
                    playback.shuffleOn = true
                    Log.v(TAG, "shuffle is on")
                }
            }
        }
    }

    inner class NotificationBroadcastReceiver: BroadcastReceiver(){

        override fun onReceive(context: Context?, intent: Intent?) {
            if(intent != null){
                Log.d(TAG, "onReceive: " + intent.action + "; " + playingstate)
                when(intent.action){
                    ACTION_MEDIA_PLAY -> {
                        if(playingstate == EnumPlaybackState.STATE_PAUSE){
                            playback.handleOnPlay()
                            playingstate = EnumPlaybackState.STATE_PLAY
                            //Log.d(TAG, "PLAY: playingstate: " + playingstate + ", Playbackstate:" + msession.controller.playbackState.state)
                        }
                    }
                    ACTION_MEDIA_PAUSE -> {
                        if(playingstate == EnumPlaybackState.STATE_PLAY){
                            playback.handleOnPause()
                            playingstate = EnumPlaybackState.STATE_PAUSE
                            //Log.d(TAG, "PAUSE: playingstate: " + playingstate + ", Playbackstate:" + msession.controller.playbackState.state)

                        }
                    }
                    ACTION_MEDIA_NEXT -> {
                        if(playingstate == EnumPlaybackState.STATE_PLAY || playingstate == EnumPlaybackState.STATE_PAUSE) {
                            playback.handleOnNext()
                            playingstate = EnumPlaybackState.STATE_NEXT
                            //Log.d(TAG, "NEXT: playingstate: " + playingstate + ", Playbackstate:" + msession.controller.playbackState.state)
                        }
                    }
                    else -> Log.d(TAG, "Neuer Intent mit Action:${intent.action}")
                }
            }
        }
    }
}