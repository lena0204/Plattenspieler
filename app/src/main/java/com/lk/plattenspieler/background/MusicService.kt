package com.lk.plattenspieler.background

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.browse.MediaBrowser
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.ResultReceiver
import android.provider.MediaStore
import android.service.media.MediaBrowserService
import android.support.v4.media.session.MediaButtonReceiver
import android.util.Log
import com.lk.plattenspieler.R
import com.lk.plattenspieler.main.MainActivity
import android.app.NotificationChannel
import android.media.*
import android.os.Build
import android.support.annotation.RequiresApi
import java.util.*

/**
 * Created by Lena on 08.06.17.
 */
class MusicService: MediaBrowserService() {

    private val TAG = "com.lk.pl-MusicService"
    //private val brn = BroadcastReceiverNoisy()
    private val amCallback = AudioFocusCallback()
    private val ID: Int = 88
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
    private var playingID: Long = 0
    private var currentMediaMetaData: MediaMetadata? = null
    private var currentMediaFile: String? = null
    private var currentMediaId: String? = null	// TODO Metadata, ID, File zusammenführen
    private var musicPlayer: MediaPlayer? = null
    // Statusvariablen, um verschiedene Sachen abzufragen
    private var serviceStarted = false
    private var positionMs = -1
    private var audioFocus = AUDIO_LOSS
    private var shuffleOn = false

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
        Log.d(TAG, "Playbackstate: " + msession.controller.playbackState.state)
        if(msession.controller.playbackState.state == PlaybackState.STATE_PAUSED) {
            handleOnStop()
        }
        Log.d(TAG, "Service started: " + this.serviceStarted)
        // der Intent, der benutzt wurde für bind()
        return bool
    }

    // Hierachie der Musiktitel
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

    // Methoden zur Verwaltung des Musikplayers
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
                // starte im Vordergrund mit Benachrichtigung
                this.startForeground(ID, showNotification(PlaybackState.STATE_PLAYING).build())
            }
            musicPlayer!!.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MusicPlayerError: $what; $extra")
                false
            }
            musicPlayer!!.setOnCompletionListener { _ ->
                // neuen Titel abspielen falls vorhanden; von der Schlange holen und Werte zurücksetzen
                if(!playingQueue.isEmpty()){
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
        musicPlayer!!.pause()
        updatePlaybackstate(PlaybackState.STATE_PAUSED)
        this.stopForeground(false)
    }
    fun handleOnStop(){
        Log.d(TAG, "handleOnStop")
        // update Metadata, state und die Playliste
        playingQueue.clear()
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
    fun handleOnNext(){
        if(!playingQueue.isEmpty()){
            // von der Schlange holen und Werte zurücksetzen
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

        shuffleOn = true
	}

    // Update der Abspieldaten und Benachrichtigung erstellen
    private fun showNotification(state: Int): Notification.Builder{
        Log.i(TAG, shuffleOn.toString())
        val nb = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
			Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }
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
        nb.setContentIntent(PendingIntent.getActivity(this, 0, i,0))
        // korrekt anzeigen, ob Shuffle aktiviert ist
        if(shuffleOn){
            nb.addAction(Notification.Action.Builder(R.mipmap.ic_shuffle, "Shuffle", null).build())
        } else {
            nb.addAction(Notification.Action.Builder(R.color.transparent, "Shuffle", null).build())
        }
        // passendes Icon für Play / Pause anzeigen
        if(state == PlaybackState.STATE_PLAYING){
            nb.addAction(Notification.Action.Builder(R.mipmap.ic_pause, "Pause",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(applicationContext, PlaybackState.ACTION_PAUSE)).build())
        } else {
            nb.addAction(Notification.Action.Builder(R.mipmap.ic_play, "Play",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(applicationContext, PlaybackState.ACTION_PLAY)).build())
        }
        nb.addAction(Notification.Action.Builder(R.mipmap.ic_next, "Next",
                MediaButtonReceiver.buildMediaButtonPendingIntent(applicationContext, PlaybackState.ACTION_SKIP_TO_NEXT)).build())
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
            nm.notify(ID, showNotification(state).build())
        } else if(state == PlaybackState.STATE_PAUSED ||
                state == PlaybackState.STATE_SKIPPING_TO_NEXT){
            playbackState.setActions(PlaybackState.ACTION_PLAY
                    or PlaybackState.ACTION_PLAY_FROM_MEDIA_ID
                    or PlaybackState.ACTION_SKIP_TO_NEXT)
            val position = musicPlayer!!.currentPosition.toLong()
            positionMs = position.toInt()
            playbackState.setState(PlaybackState.STATE_PAUSED, position, 1.0f)
            nm.notify(ID,showNotification(state).build())
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

    // innere Callbacks vom MainActivity und vom AudioManager
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
}