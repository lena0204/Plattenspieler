package com.lk.music_service_library.background

import android.app.Notification
import android.app.NotificationManager
import android.media.browse.MediaBrowser
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.service.media.MediaBrowserService
import android.util.Log
import android.content.*
import android.media.*
import android.os.*
import com.lk.music_service_library.R
import com.lk.music_service_library.models.MusicList
import com.lk.music_service_library.models.MusicMetadata
import com.lk.music_service_library.observables.*
import java.util.*

/**
 * Created by Lena on 08.06.17.
 * Verwaltet die Callbacks (MediaBrowser und MediaSession) zum Client, Zugriff auf MusicProvider,
 * Playback und Notification (Background)
 */
class MusicService: MediaBrowserService(), Observer  {

    private val TAG = "com.lk.pl-MusicService"

    private val notificationID = 88
    private lateinit var notificationManager: NotificationManager
    private val musicNotification = MusicNotification(this)
    private val nbReceiver = NotificationBroadcastReceiver()

    private lateinit var session: MediaSession
    private lateinit var playback: MusicPlayback
    private lateinit var musicProvider: MusicProvider

    private var serviceStarted = false

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
        initializeComponents()
    }

    private fun prepareSession(){
        session = MediaSession(applicationContext, TAG)
        registerBroadcastReceiver()
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            session.setFlags(MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
        }
        session.setCallback(MusicSessionCallback())
        sessionToken = session.sessionToken
    }

    private fun registerBroadcastReceiver(){
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
        session.setPlaybackState(playbackState.build())
        session.setMetadata(MediaMetadata.Builder().build())
        session.setQueueTitle(getString(R.string.queue_title))
    }

    private fun initializeComponents(){
        musicProvider = MusicProvider(this.applicationContext)
        notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        playback = MusicPlayback(this)
        PlaybackDataObservable.addObserver(this)
    }


    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): MediaBrowserService.BrowserRoot? {
        if(this.packageName == clientPackageName){
            return MediaBrowserService.BrowserRoot(MusicProvider.ROOT_ID, null)
        }
        return MediaBrowserService.BrowserRoot("", null)
    }

    override fun onLoadChildren(parentId: String, result: MediaBrowserService.Result<MutableList<MediaBrowser.MediaItem>>) {
        when {
            parentId == MusicProvider.ROOT_ID -> result.sendResult(musicProvider.getAlbums().getMediaItemList())
            parentId.contains("ALBUM-") -> result.sendResult(getTitles(parentId))
			else -> Log.e(TAG, "No known parent ID")
		}
    }

    private fun getTitles(albumId: String): MutableList<MediaBrowser.MediaItem>{
        val id = albumId.replace("ALBUM-", "")
        return musicProvider.getTitlesForAlbumID(albumid = id)
                .getMediaItemList()
    }


    override fun update(o: Observable?, arg: Any?) {
        if(arg != null && arg is PlaybackActions){
            when(arg){
                PlaybackActions.ACTION_UPDATE_METADATA -> setNewMetadata()
                PlaybackActions.ACTION_UPDATE_PLAYBACKSTATE -> setNewPlaybackState()
                PlaybackActions.ACTION_UPDATE_QUEUE ->  setNewQueue()
                PlaybackActions.ACTION_CAN_PLAY ->  launchService()
                PlaybackActions.ACTION_PAUSE -> this.stopForeground(false)
                PlaybackActions.ACTION_STOP -> stopService()
            }
        }
    }

    private fun setNewMetadata(){
        val metadata = PlaybackDataObservable.metadata
        sendBroadcastForLightningLauncher(metadata)
        session.setMetadata(metadata.getMediaMetadata())
    }

    private fun sendBroadcastForLightningLauncher(metadata: MusicMetadata){
        val extras = Bundle()
        val track = Bundle()
        track.putString("title", metadata.title)
        track.putString("album", metadata.album)
        track.putString("artist",metadata.artist)
        extras.putBundle("track", track)
        extras.putString("aaPath", metadata.cover_uri)
        this.sendBroadcast(Intent("com.lk.plattenspieler.metachanged").putExtras(extras))
    }

    private fun setNewPlaybackState(){
        val state = PlaybackDataObservable.getState()
        session.setPlaybackState(state)
        launchNewNotification(state.state)
    }

    private fun launchNewNotification(state: Int){
        when(state){
            PlaybackState.STATE_PLAYING,
            PlaybackState.STATE_PAUSED ->
                notificationManager.notify(notificationID, prepareCurrentNotification(state))
            PlaybackState.STATE_STOPPED -> notificationManager.cancel(notificationID)
        }
    }

    private fun prepareCurrentNotification(state: Int): Notification{
        val metadata = PlaybackDataObservable.metadata
        val shuffleOn = PlaybackDataObservable.shuffleOn
        return musicNotification.showNotification(state, metadata, shuffleOn).build()
    }

    private fun setNewQueue(){
        val queue = PlaybackDataObservable.musicQueue
        session.setQueue(queue.getQueueItemList())
    }

    fun getMetadataForId(id: String): MusicMetadata{
        val metadata = musicProvider.getMediaMetadata(id)
        metadata.cover = MusicMetadata.decodeAlbumcover(metadata.cover_uri, resources)
        return metadata
    }

    private fun launchService(){
        startServiceIfNecessary()
        this.startForeground(notificationID, prepareCurrentNotification(PlaybackState.STATE_PLAYING))
    }

    private fun startServiceIfNecessary(){
        if(!serviceStarted){
            this.startService(android.content.Intent(applicationContext,
                    com.lk.music_service_library.background.MusicService::class.java))
            serviceStarted = true
        }
        if(!session.isActive)
            session.isActive = true
    }

    private fun stopService(){
        Log.d(TAG, "handleOnStop Service")
        this.stopSelf()
        serviceStarted = false
        this.stopForeground(true)

    }

    override fun onUnbind(intent: Intent?): Boolean {
        val bool = super.onUnbind(intent)
        if(session.controller.playbackState.state == PlaybackState.STATE_PAUSED) {
            PlaybackDataObservable.stop()
        }
        Log.v(TAG, "Service started: " + this.serviceStarted)
        return bool
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        this.unregisterReceiver(nbReceiver)
        session.release()
        session.isActive = false
    }

    inner class MusicSessionCallback: MediaSession.Callback(){
        override fun onPlay() {
            PlaybackDataObservable.tryPlaying()
        }

        override fun onPlayFromMediaId(mediaId: String, extras: Bundle?) {
            Log.d(TAG, "onPlayfromid")
            if(shallPrepare(extras)){
                // Callback onPrepareFromId ist erst ab API 24 (aktuell 22) möglich
                PlaybackDataObservable.prepareFromId(mediaId, getMetadataForId(mediaId))
            } else {
                PlaybackDataObservable.playFromId(mediaId)
            }
        }

        private fun shallPrepare(extras: Bundle?): Boolean = extras != null && extras.containsKey("I")

        override fun onPause() {
            PlaybackDataObservable.pause(session.controller.playbackState.position)
        }
        override fun onStop() {
            PlaybackDataObservable.stop()
        }
        override fun onSkipToNext() {
            PlaybackDataObservable.next()
        }
        override fun onSkipToPrevious() {
            PlaybackDataObservable.previous(session.controller.playbackState.position)
        }

        override fun onCommand(command: String, args: Bundle?, resultReceiver: ResultReceiver?) {
            when(command){
                "addQueue" -> addQueueToService(args)
                "shuffle" -> PlaybackDataObservable.setShuffleOn()
                "addAll" -> {}
            }
        }

        private fun addQueueToService(args: Bundle?){
            if(args != null) {
                args.classLoader = this.javaClass.classLoader
                val list = args.getParcelable<MusicList>("L")
                PlaybackDataObservable.setQueue(list)
            }
        }
    }

    inner class NotificationBroadcastReceiver: BroadcastReceiver(){

        override fun onReceive(context: Context?, intent: Intent?) {
            if(intent != null){
                Log.d(TAG, "onReceive: " + intent.action + "; " + session.controller.playbackState.state)
                when(intent.action){
                    ACTION_MEDIA_PLAY -> handlePlayIntent()
                    ACTION_MEDIA_PAUSE -> handlePauseIntent()
                    ACTION_MEDIA_NEXT -> handleNextIntent()
                }
            }
        }

        private fun handlePlayIntent(){
            if(PlaybackState.STATE_PAUSED == PlaybackDataObservable.getState().state){
                PlaybackDataObservable.tryPlaying()
                // logCurrentState("PLAY")
            }
        }

        private fun handlePauseIntent(){
            if(PlaybackState.STATE_PLAYING == PlaybackDataObservable.getState().state){
                PlaybackDataObservable.pause(session.controller.playbackState.position)
                // logCurrentState("PAUSE")
            }
        }

        private fun handleNextIntent(){
            if(PlaybackState.STATE_PLAYING == PlaybackDataObservable.getState().state ||
                    PlaybackState.STATE_PAUSED == PlaybackDataObservable.getState().state) {
                PlaybackDataObservable.next()
                // logCurrentState("NEXT")
            }
        }

        private fun logCurrentState(intentAction: String){
            // zur Fehlererkennung
            Log.d(TAG, intentAction + ": Observablestate: " + PlaybackDataObservable.getState().state +
                    " vs. Controllerstate:" + session.controller.playbackState.state)
        }
    }
}