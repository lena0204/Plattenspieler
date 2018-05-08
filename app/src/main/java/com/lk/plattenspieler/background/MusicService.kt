package com.lk.plattenspieler.background

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.graphics.BitmapFactory
import android.media.browse.MediaBrowser
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.ResultReceiver
import android.provider.MediaStore
import android.service.media.MediaBrowserService
import android.util.Log
import com.lk.plattenspieler.R
import com.lk.plattenspieler.main.MainActivity
import android.app.NotificationChannel
import android.content.*
import android.media.*
import android.os.Build
import android.support.annotation.RequiresApi
import com.lk.plattenspieler.utils.MediaStack
import java.util.*

/**
 * Created by Lena on 08.06.17.
 */
class MusicService: MediaBrowserService()  {

    private val TAG = "com.lk.pl-MusicService"
    //private val brn = BroadcastReceiverNoisy()
    private val amCallback = AudioFocusCallback()
    private val ID: Int = 88
    private val STATE_STOP = 1
    private val STATE_PLAY = 2
    private val STATE_PAUSE = 3
    private val STATE_NEXT = 4
    private val AUDIO_FOCUS = 0
    private val AUDIO_LOSS = 1
    private val AUDIO_DUCK = 2
    private val AUDIO_PAUSE_PLAYING = 3
    private val CHANNEL_ID = "plattenspieler_playback"
    private val audioAttr = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA).build()
    @TargetApi(26)
    private val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttr)
            .setOnAudioFocusChangeListener(amCallback)
            .build()

    private lateinit var playbackState: PlaybackState.Builder
    private lateinit var metadataState: MediaMetadata.Builder
    private lateinit var msession: MediaSession
    private lateinit var c: android.content.Context
    private lateinit var musicProvider: MusicProvider
    private lateinit var am: AudioManager
    private lateinit var nm: NotificationManager

    private var playingQueue = mutableListOf<MediaSession.QueueItem>()
    private var mediaStack = MediaStack()
    private var playingID: Long = 0
    private var currentMediaMetaData: MediaMetadata? = null
    private var currentMediaFile: String? = null
    private var currentMediaId: String? = null
    private var musicPlayer: MediaPlayer? = null
    // Statusvariablen, um verschiedene Sachen abzufragen
    private var serviceStarted = false
    private var positionMs = -1
    private var audioFocus = AUDIO_LOSS
    private var shuffleOn = false
    private var playingstate = STATE_STOP

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        c = applicationContext
        // Audiofokus und Benachrichtigung initialisieren
        am = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        nm = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Session aufsetzen (mit Provider, Token, Callback und Flags setzen)
        musicProvider = MusicProvider(c)
        msession = MediaSession(c, TAG)
        /*if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            msession.setFlags(MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
        }*/
        msession.setCallback(MusicSessionCallback())
        sessionToken = msession.sessionToken
        // Playback und Metadata initialisieren
        playbackState = PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY)
                .setActions(PlaybackState.ACTION_PLAY_PAUSE)
        metadataState = MediaMetadata.Builder()
        msession.setPlaybackState(playbackState.build())
        msession.setMetadata(metadataState.build())
        // Queuetitel setzen
        msession.setQueueTitle(getString(R.string.queue_title))
    }
    override fun onUnbind(intent: Intent?): Boolean {
        val bool = super.onUnbind(intent)
		/*var state = ""
		when(msession.controller.playbackState.state){
			1 -> state = "stopped"
			2 -> state = "paused"
			3 -> state = "playing"
		}
        Log.d(TAG, "Playbackstate: $state")*/
        if(msession.controller.playbackState.state == PlaybackState.STATE_PAUSED) {
            handleOnStop()
        }
        Log.d(TAG, "Service started: " + this.serviceStarted)
        return bool
    }

    // -------------- Hierachie der Musiktitel --------------
    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): MediaBrowserService.BrowserRoot? {
        // nur mein eigenes Paket zulassen zum Abfragen
        if(this.packageName == clientPackageName){
            return MediaBrowserService.BrowserRoot(musicProvider.ROOT_ID, null)
        }
        return MediaBrowserService.BrowserRoot("", null)
    }
    override fun onLoadChildren(parentId: String, result: MediaBrowserService.Result<MutableList<MediaBrowser.MediaItem>>) {
        // eigene Hierachie aufbauen mit Browsable und Playable MediaItems
        when{
            parentId == musicProvider.ROOT_ID -> sendRootChildren(result)
            parentId.contains("ALBUM-") -> sendAlbumChildren(result, parentId)
			else -> android.util.Log.e(TAG, "No known parent ID")       // Fehler
		}
    }
    private fun sendAlbumChildren(result: MediaBrowserService.Result<MutableList<MediaBrowser.MediaItem>>, albumid: String){
        // alle Titel eines Albums (SELECT und WHERE festlegen)
        val projection = Array(3, init = {_ -> ""})
        projection[0] = MediaStore.Audio.Media._ID
        projection[1] = MediaStore.Audio.Media.TITLE
        projection[2] = MediaStore.Audio.Media.ARTIST
        var selection = albumid.replace("ALBUM-", "")
        selection = android.provider.MediaStore.Audio.Media.ALBUM_ID + "='" + selection + "'"
        // Datenbank abfragen
        val cursor = contentResolver.query(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,selection,null,null)
        val list = MutableList(cursor.count,
                { i -> MediaBrowser.MediaItem(MediaDescription.Builder().setMediaId(i.toString()).build(), MediaBrowser.MediaItem.FLAG_PLAYABLE) } )
        if(cursor.moveToFirst()){
            // Pfad zum Albumcover abfragen
            var cover = ""
            projection[0] = MediaStore.Audio.Albums._ID
            projection[1] = MediaStore.Audio.Albums.ALBUM_ART
            projection[2] = MediaStore.Audio.Albums.ALBUM
            val select = MediaStore.Audio.Albums._ID + "='" + albumid.replace("ALBUM-", "") + "'"
            val c = contentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, projection, select, null, null)
            if(c.count == 1){
                c.moveToFirst()
                cover = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ART))
            }
            c.close()
            // Liste aufstellen (Weiterleitung an den Provider)
            musicProvider.getTitles(cursor, list, cover)
        }
        cursor.close()
        result.sendResult(list)
    }
    private fun sendRootChildren(result: MediaBrowserService.Result<MutableList<MediaBrowser.MediaItem>>){
        // Alben abfragen (SELECT, ORDERBY definieren)
        val orderby = MediaStore.Audio.Albums.ALBUM + " ASC"
        val projection = Array(5, init = {_ -> ""})
        projection[0] = MediaStore.Audio.Albums._ID
        projection[1] = MediaStore.Audio.Albums.ALBUM
        projection[2] = MediaStore.Audio.Albums.ALBUM_ART
        projection[3] = MediaStore.Audio.Albums.ARTIST
        projection[4] = MediaStore.Audio.Albums.NUMBER_OF_SONGS
        // Datenbankabfrage und Weitergabe an den Provider
        val cursor = contentResolver.query(android.provider.MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, null,null,null,orderby)
        val list = MutableList(cursor.count,
                { i -> MediaBrowser.MediaItem(MediaDescription.Builder().setMediaId(i.toString()).build(), MediaBrowser.MediaItem.FLAG_BROWSABLE) } )
        if(cursor.moveToFirst()){
            musicProvider.getAlbums(cursor, list)
        }
        cursor.close()
        result.sendResult(list)
    }

    // --------------- Methoden zur Verwaltung des Musikplayers ------------
    fun handleOnPlay(){
        if(musicPlayer != null){
            if(musicPlayer!!.isPlaying) musicPlayer!!.stop()
        }
        // nicht spielen, wenn keine Musik ausgewählt ist
        if(currentMediaFile.isNullOrEmpty()){
            return
        }
        // request AudioFocus, nur spielen falls gestattet -> AUDIOFOCUS_GAIN -> dauerhaft
        val result = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            am.requestAudioFocus(audioFocusRequest)
        } else {
            am.requestAudioFocus(amCallback, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        if(result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
            // Service starten und Session aktiv setzen
            audioFocus = AUDIO_FOCUS
            if(!serviceStarted){
                this.startService(android.content.Intent(c, com.lk.plattenspieler.background.MusicService::class.java))
                serviceStarted = true
            }
            if(!msession.isActive){
                msession.isActive = true
            }
            // METADATEN setzen
            updateMetadata()
            musicPlayer = MediaPlayer()
            musicPlayer!!.setOnPreparedListener{ _ ->
                // spielen starten
                if(positionMs != -1){
                    musicPlayer!!.seekTo(positionMs)
                }
                musicPlayer!!.start()
                updatePlaybackstate(PlaybackState.STATE_PLAYING)
                playingstate = STATE_PLAY
                // starte im Vordergrund mit Benachrichtigung
                this.startForeground(ID, MediaNotification(this).showNotification(PlaybackState.STATE_PLAYING).build())
            }
            musicPlayer!!.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MusicPlayerError: $what; $extra")
                false
            }
            musicPlayer!!.setOnCompletionListener { _ ->
                // neuen Titel abspielen falls vorhanden; von der Schlange holen und Werte zurücksetzen
                if(!playingQueue.isEmpty()){
                    mediaStack.pushMedia(currentMediaMetaData)
                    Log.d(TAG, "onPlay(), Größe: " + playingQueue.size)
                    val queueItem = playingQueue[0]
                    playingQueue.removeAt(0)
                    msession.setQueue(playingQueue)
                    positionMs = -1
                    currentMediaId = queueItem.description.mediaId
                    currentMediaFile = musicProvider.getFileFromMediaId(queueItem.description.mediaId)
                    handleOnPlay()
                } else {
                    handleOnStop()
                }
            }
            musicPlayer!!.setAudioAttributes(audioAttr)
            musicPlayer!!.setDataSource(currentMediaFile)
            musicPlayer!!.prepareAsync()
        }
    }
    fun handleOnPlayFromId(pId: String?){
        Log.d(TAG, "handleOnPlayFromId")
        playingQueue.clear()
        shuffleOn = false
        // Standardmäßig ausschalten wenn neu abgespielt wird, falls shuffle wird das später gesetzt
        msession.setQueue(playingQueue)
        positionMs = -1
        currentMediaId = pId
        currentMediaFile = musicProvider.getFileFromMediaId(currentMediaId)
        handleOnPlay()
    }
    fun handleOnPrepareFromId(pId: String?){
        // Nach dem Neustart der App und dem Wiederherstellen der Playliste
        Log.d(TAG, "handleOnPrepareFromId")
        positionMs = -1
        currentMediaId = pId
        currentMediaFile = musicProvider.getFileFromMediaId(currentMediaId)
        updateMetadata()
    }
    fun handleOnPause() {
        playingstate = STATE_PAUSE
        musicPlayer!!.pause()
        updatePlaybackstate(PlaybackState.STATE_PAUSED)
        this.stopForeground(false)
    }
    fun handleOnPrevious(){
        val previous = mediaStack.popMedia()
        if(previous != null){
            if(msession.controller.playbackState.position >= 15000){
                positionMs = 0      // zum Anfang des Liedes skippen, wenn schon mehr als 15s gespielt wurde
            } else {
                Log.d(TAG, "Vorgänger ist vorhanden mit Titel: " + previous.getString(MediaMetadata.METADATA_KEY_TITLE))
                playingQueue.add(0, MediaSession.QueueItem(currentMediaMetaData?.description, playingID++))
                currentMediaId = previous.getString(MediaMetadata.METADATA_KEY_MEDIA_ID)
                currentMediaFile = previous.getString(MediaMetadata.METADATA_KEY_WRITER)
                currentMediaMetaData = previous
                msession.setQueue(playingQueue)
            }
        } else {
            positionMs = -1     // erstes Lied in der Queue
            Log.d(TAG, "Vorgänger ist NULL, nichts passiert.")
        }
        handleOnPlay()
    }
    fun handleOnNext(){
        if(!playingQueue.isEmpty()){
            // von der Schlange holen und Werte zurücksetzen
            mediaStack.pushMedia(currentMediaMetaData)
            Log.d(TAG, "onNext(), Größe: " + playingQueue.size)
            val queueItem = playingQueue[0]
            playingQueue.removeAt(0)
            positionMs = 0
            currentMediaId = queueItem.description.mediaId
            currentMediaFile = musicProvider.getFileFromMediaId(currentMediaId)
            msession.setQueue(playingQueue)
            handleOnPlay()
        } else {
            handleOnStop()
        }
    }
    fun handleOnStop(){
        Log.d(TAG, "handleOnStop")
        // update Metadata, state und die Playliste
        playingstate = STATE_STOP
        playingQueue.clear()
        mediaStack.popAll()
        playingID = 0
        shuffleOn = false
        currentMediaFile = null
        currentMediaId = null
        msession.setQueue(playingQueue)
        updatePlaybackstate(PlaybackState.STATE_STOPPED)
        updateMetadata()
        // AudioFocus freigeben
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            am.abandonAudioFocusRequest(audioFocusRequest)
        } else {
            am.abandonAudioFocus(amCallback)
        }
        audioFocus = AUDIO_LOSS
        // Session und Service beenden
        msession.release()
        this.stopSelf()
        serviceStarted = false
        // Player stoppen
        musicPlayer?.stop()
        musicPlayer?.reset()
        musicPlayer?.release()
        musicPlayer = null
        // endgültig beenden
        msession.isActive = false
        this.stopForeground(true)
    }

	fun addAllSongsToPlayingQueue(){
		Log.d(TAG, "addAllSongsToPlayingQueue")
		val playingID = musicProvider.getFirstTitle()
		handleOnPlayFromId(playingID)
		// alle anderen Songs zufällig hinzufügen
		Log.d(TAG, "Queue erstellen ohne $playingID")
		val listSongs = musicProvider.getAllTitle(playingID)
		val random = Random()
		var i: Int
		while(!listSongs.isEmpty()){
			i = random.nextInt(listSongs.size)
			playingQueue.add(listSongs[i])
			listSongs.removeAt(i)
		}
		Log.d(TAG, "Queue fertig mit Länge ${playingQueue.size}")
		msession.setQueue(playingQueue)
		updateMetadata()
		nm.notify(ID, MediaNotification(this).showNotification(msession.controller.playbackState.state).build())
        shuffleOn = true
	}

    // ------------------ Update der Abspieldaten ----------------
    fun updateMetadata(){
        var number = "0"
        if(!playingQueue.isEmpty()){
            number = playingQueue.size.toString()
        }
        // Daten aktualisieren und setzen
        currentMediaMetaData = musicProvider.getMediaDescription(currentMediaId, number)
        if(currentMediaMetaData == null){
            Log.e(TAG, "Metadaten sind null")
        }
        // Broadcast rausschicken (für Lightning Launcher)
        val extras = Bundle()
        val track = Bundle()
        track.putString("title", currentMediaMetaData?.getString(MediaMetadata.METADATA_KEY_TITLE))
        track.putString("album", currentMediaMetaData?.getString(MediaMetadata.METADATA_KEY_ALBUM))
        track.putString("artist",currentMediaMetaData?.getString(MediaMetadata.METADATA_KEY_ARTIST))
        extras.putBundle("track", track)
        extras.putString("aaPath", currentMediaMetaData?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI))
        sendBroadcast(Intent("com.lk.plattenspieler.metachanged").putExtras(extras))
        // IDEA_ Broadcast sticky ?
        msession.setMetadata(currentMediaMetaData)
    }
    private fun updatePlaybackstate(state: Int){
        if(state == PlaybackState.STATE_PLAYING){
            // anfangen oder weiterspielen
            playbackState.setActions(PlaybackState.ACTION_PAUSE
                    or PlaybackState.ACTION_STOP
                    or PlaybackState.ACTION_SKIP_TO_NEXT)
            val position = musicPlayer!!.currentPosition.toLong()
            positionMs = position.toInt()
            playbackState.setState(PlaybackState.STATE_PLAYING, position, 1.0f)
            nm.notify(ID, MediaNotification(this).showNotification(state).build())
        } else if(state == PlaybackState.STATE_PAUSED ||
                state == PlaybackState.STATE_SKIPPING_TO_NEXT){
            playbackState.setActions(PlaybackState.ACTION_PLAY
                    or PlaybackState.ACTION_PLAY_FROM_MEDIA_ID
                    or PlaybackState.ACTION_SKIP_TO_NEXT)
            val position = musicPlayer!!.currentPosition.toLong()
            positionMs = position.toInt()
            playbackState.setState(PlaybackState.STATE_PAUSED, position, 1.0f)
            nm.notify(ID, MediaNotification(this).showNotification(state).build())
        } else if(state == PlaybackState.STATE_STOPPED){
            playbackState.setActions(PlaybackState.ACTION_PLAY
                    or PlaybackState.ACTION_PLAY_FROM_MEDIA_ID)
            positionMs = 0
            playbackState.setState(PlaybackState.STATE_STOPPED, 0, 1.0f)
            nm.cancel(ID)
        }
        val extras = Bundle()
        extras.putBoolean("shuffle", shuffleOn)
        playbackState.setExtras(extras)
        msession.setPlaybackState(playbackState.build())
    }

    // ----------------- Callbacks und Notification ---------------
    inner class MusicSessionCallback: MediaSession.Callback(){
        override fun onPlay() {
            handleOnPlay()
        }
        override fun onPlayFromMediaId(mediaId: String, extras: Bundle) {
            shuffleOn = false
            if(extras.containsKey("I")){
                handleOnPlayFromId(mediaId)
            } else {
                handleOnPrepareFromId(mediaId)
            }
        }
        override fun onPause() {
            handleOnPause()
        }
        override fun onStop() {
            handleOnStop()
        }
        override fun onSkipToNext() {
            handleOnNext()
        }

        override fun onSkipToPrevious() {
            handleOnPrevious()
        }

        override fun onMediaButtonEvent(mediaButtonIntent: Intent?): Boolean{
            Log.i(TAG, "onMediaButtonEvent" + mediaButtonIntent?.action
					+ ";" + mediaButtonIntent?.data + ";" + mediaButtonIntent?.`package`)
            return super.onMediaButtonEvent(mediaButtonIntent)
        }

        override fun onCommand(command: String, args: Bundle?, cb: ResultReceiver?) {
            //super.onCommand(command, args, cb)
            when(command){
                "add" -> {
                    val md = args?.get("S") as MediaDescription
                    playingQueue.add(MediaSession.QueueItem(md, playingID++))
                    msession.setQueue(playingQueue)
                    // Zum sicherstellen, dass die richtige Anzahl an Liedern angezeigt wird
                    updateMetadata()
                }
                "addAll" -> addAllSongsToPlayingQueue()
                "shuffle" -> shuffleOn = true
                "remove" -> {
                    var i = 0
                    // Description aus der Liste entfernen und aktualisieren
                    while(i < playingQueue.size){
                        if(playingQueue[i].description == args?.get("S") as MediaDescription){
                            playingQueue.removeAt(i)
                            i = playingQueue.size
                            msession.setQueue(playingQueue)
                        } else {
                            i++
                        }
                    }
                    updateMetadata()
                }
            }
        }
    }
    inner class AudioFocusCallback: AudioManager.OnAudioFocusChangeListener{
        override fun onAudioFocusChange(focusChange: Int) {
            // Fälle abfragen und entsprechend pausieren oder leiser stellen
            when(focusChange){
                AudioManager.AUDIOFOCUS_LOSS -> {
                    handleOnStop()
                    audioFocus = AUDIO_LOSS
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    if(musicPlayer!!.isPlaying){
                        handleOnPause()
                        audioFocus = AUDIO_PAUSE_PLAYING
                    }
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    musicPlayer!!.setVolume(0.3f, 0.3f)
                    audioFocus = AUDIO_DUCK
                }
                AudioManager.AUDIOFOCUS_GAIN -> {
                    if(audioFocus == AUDIO_PAUSE_PLAYING){
                        handleOnPlay()
                    } else {
                        musicPlayer!!.setVolume(0.8f, 0.8f)     // generell leiser spielen
                    }
                }
            }
        }
    }

    inner class MediaNotification(private val service: MusicService): BroadcastReceiver(){

        private val ACTION_MEDIA_PLAY = "com.lk.pl-ACTION_MEDIA_PLAY"
        private val ACTION_MEDIA_PAUSE = "com.lk.pl-ACTION_MEDIA_PAUSE"
        private val ACTION_MEDIA_NEXT = "com.lk.pl-ACTION_MEDIA_NEXT"

        fun showNotification(state: Int): Notification.Builder{
            //Log.i(TAG, shuffleOn.toString())
            // Channel ab Oreo erstellen
            val nb = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createChannel()
                Notification.Builder(service, CHANNEL_ID)
            } else {
                Notification.Builder(service)
            }
            // Inhalt setzen
            nb.setContentTitle(currentMediaMetaData?.getString(MediaMetadata.METADATA_KEY_TITLE))
            nb.setContentText(currentMediaMetaData?.getString(MediaMetadata.METADATA_KEY_ARTIST))
            nb.setSubText(currentMediaMetaData?.getLong(MediaMetadata.METADATA_KEY_NUM_TRACKS).toString() + " Lieder noch")
            nb.setSmallIcon(R.drawable.notification_stat_playing)
            nb.setLargeIcon(BitmapFactory.decodeFile(currentMediaMetaData?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)))
            // Media Style aktivieren
            nb.setStyle(Notification.MediaStyle()
                    .setMediaSession(sessionToken)
                    .setShowActionsInCompactView(1,2))
            val i = Intent(applicationContext, MainActivity::class.java)
            nb.setContentIntent(PendingIntent.getActivity(service, 0, i, PendingIntent.FLAG_UPDATE_CURRENT))
            // korrekt anzeigen, ob Shuffle aktiviert ist
            if(shuffleOn){
                nb.addAction(Notification.Action.Builder(R.mipmap.ic_shuffle, "Shuffle", null).build())
            } else {
                nb.addAction(Notification.Action.Builder(R.color.transparent, "Shuffle", null).build())
            }
            // passendes Icon für Play / Pause anzeigen
            var pi: PendingIntent
            if(state == PlaybackState.STATE_PLAYING){
                pi = PendingIntent.getBroadcast(service, 100,
                        Intent(ACTION_MEDIA_PAUSE).setPackage(service.packageName), 0)
                nb.addAction(Notification.Action.Builder(R.mipmap.ic_pause, "Pause",pi).build())
            } else {
                pi = PendingIntent.getBroadcast(service, 100,
                        Intent(ACTION_MEDIA_PLAY).setPackage(service.packageName), 0)
                nb.addAction(Notification.Action.Builder(R.mipmap.ic_play, "Play", pi).build())
            }
            pi = PendingIntent.getBroadcast(service, 100,
                    Intent(ACTION_MEDIA_NEXT).setPackage(service.packageName), 0)
            nb.addAction(Notification.Action.Builder(R.mipmap.ic_next, "Next", pi).build())
            registerBroadcast()
            return nb
        }
        @RequiresApi(Build.VERSION_CODES.O)
        private fun createChannel() {
            val channel = NotificationChannel(CHANNEL_ID, "Music playback", NotificationManager.IMPORTANCE_LOW)
            channel.description = "Music playback controls"
            channel.setShowBadge(false)
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            nm.createNotificationChannel(channel)
        }
        private fun registerBroadcast(){
            val ifilter = IntentFilter()
            ifilter.addAction(ACTION_MEDIA_PLAY)
            ifilter.addAction(ACTION_MEDIA_PAUSE)
            ifilter.addAction(ACTION_MEDIA_NEXT)
            service.registerReceiver(this, ifilter)
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            if(intent != null){
                Log.d(TAG, "onReceive: " + intent.action)
                when(intent.action){
                    ACTION_MEDIA_PLAY -> {
                        if(playingstate == STATE_PAUSE){
                            service.handleOnPlay()
                            playingstate = STATE_PLAY
                            Log.d(TAG, "PLAY: playingstate: " + playingstate + ", Playbackstate:" + msession.controller.playbackState.state)

                        }
                    }
                    ACTION_MEDIA_PAUSE -> {
                        if(playingstate == STATE_PLAY){
                            service.handleOnPause()
                            playingstate = STATE_PAUSE
                            Log.d(TAG, "PAUSE: playingstate: " + playingstate + ", Playbackstate:" + msession.controller.playbackState.state)

                        }
                    }
                    ACTION_MEDIA_NEXT -> {
                        if(playingstate == STATE_PLAY || playingstate == STATE_PAUSE) {
                            service.handleOnNext()
                            playingstate = STATE_NEXT
                            Log.d(TAG, "NEXT: playingstate: " + playingstate + ", Playbackstate:" + msession.controller.playbackState.state)
                        }
                    }
                    else -> Log.d(TAG, "Neuer Intent mit Action:${intent.action}")
                }
            }
        }
    }
}