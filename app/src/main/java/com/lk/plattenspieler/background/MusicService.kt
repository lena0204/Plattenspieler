package com.lk.plattenspieler.background

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.app.NotificationCompat
import android.util.Log
import com.lk.plattenspieler.R
import com.lk.plattenspieler.main.MainActivity

/**
 * Created by Lena on 08.06.17.
 */
class MusicService(): MediaBrowserServiceCompat() {

    private val TAG = "MusicService"
    //private val brn = BroadcastReceiverNoisy()
    private val amCallback = AudioFocusCallback()
    private val ID: Int = 88
    private val AUDIO_FOCUS = 0
    private val AUDIO_LOSS = 1
    private val AUDIO_DUCK = 2
    private val AUDIO_PAUSE_PLAYING = 3

    lateinit private var playbackState: PlaybackStateCompat.Builder
    lateinit private var metadataState: MediaMetadataCompat.Builder
    lateinit private var msession: MediaSessionCompat
    lateinit private var c: android.content.Context
    lateinit private var musicProvider: MusicProvider
    lateinit private var am: AudioManager
    lateinit private var nm: NotificationManager

    private var playingQueue = mutableListOf<MediaSessionCompat.QueueItem>()
    private var playingID: Long = 0
    private var currentMediaMetaData: MediaMetadataCompat? = null
    private var currentMediaFile: String? = null
    private var currentMediaId: String? = null
    private var musicPlayer: MediaPlayer? = null
    // Statusvariablen, um verschiedene Sachen abzufragen
    private var serviceStarted = false
    private var positionMs = -1
    private var audioFocus = AUDIO_LOSS

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        c = applicationContext
        // Audiofokus und Benachrichtigung initialisieren
        am = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        nm = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Session aufsetzen (mit Provider, Token, Callback und Flags setzen)
        musicProvider = MusicProvider(c)
        msession = MediaSessionCompat(c, TAG)
        msession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        msession.setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS)
        msession.setCallback(MusicSessionCallback())
        sessionToken = msession.sessionToken
        // Playback und Metadata initialisieren
        playbackState = PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY)
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE)
        metadataState = MediaMetadataCompat.Builder()
        msession.setPlaybackState(playbackState.build())
        msession.setMetadata(metadataState.build())
        // Queuetitel setzen
        msession.setQueueTitle(getString(R.string.queue_title))
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Auf Benachrichtigung reagieren
        MediaButtonReceiver.handleIntent(msession, intent)
        return super.onStartCommand(intent, flags, startId)
    }
    override fun onUnbind(intent: Intent?): Boolean {
        val bool = super.onUnbind(intent)
        Log.d(TAG, "Service started: " + this.serviceStarted)
        // der Intent, der benutzt wurde für bind()
        return bool
    }

    // Hierachie der Musiktitel
    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): MediaBrowserServiceCompat.BrowserRoot? {
        // nur mein eigenes Paket zulassen zum Abfragen
        if(this.packageName == clientPackageName){
            return MediaBrowserServiceCompat.BrowserRoot(musicProvider.ROOT_ID, null)
        }
        return MediaBrowserServiceCompat.BrowserRoot("", null)
    }
    override fun onLoadChildren(parentId: String, result: MediaBrowserServiceCompat.Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        // eigene Hierachie aufbauen mit Browsable und Playable MediaItems
        Log.d(TAG, "onLoadChildren in Service with ParentID: " + parentId)
        if(parentId == musicProvider.ROOT_ID){
            sendRootChildren(result)
        } else if(parentId.contains("ALBUM-")){
            sendAlbumChildren(result, parentId)
        } else {
            android.util.Log.e(TAG, "No known parent ID")       // Fehler
        }
    }
    private fun sendAlbumChildren(result: MediaBrowserServiceCompat.Result<MutableList<MediaBrowserCompat.MediaItem>>, albumid: String){
        // alle Titel eines Albums (SELECT und WHERE festlegen)

        val projection = Array<String>(3, init = {i -> ""})
        projection[0] = MediaStore.Audio.Media._ID
        projection[1] = MediaStore.Audio.Media.TITLE
        projection[2] = MediaStore.Audio.Media.ARTIST
        var selection = albumid.replace("ALBUM-", "")
        selection = android.provider.MediaStore.Audio.Media.ALBUM_ID + "='" + selection + "'"
        // Datenbank abfragen
        val cursor = contentResolver.query(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,selection,null,null)
        val list = MutableList(cursor.count,
                { i -> MediaBrowserCompat.MediaItem(MediaDescriptionCompat.Builder().setMediaId(i.toString()).build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE) } )
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
    private fun sendRootChildren(result: MediaBrowserServiceCompat.Result<MutableList<MediaBrowserCompat.MediaItem>>){
        // Alben abfragen (SELECT, ORDERBY definieren)
        val orderby = MediaStore.Audio.Albums.ALBUM + " ASC"
        val projection = Array<String>(5, init = {_ -> ""})
        projection[0] = MediaStore.Audio.Albums._ID
        projection[1] = MediaStore.Audio.Albums.ALBUM
        projection[2] = MediaStore.Audio.Albums.ALBUM_ART
        projection[3] = MediaStore.Audio.Albums.ARTIST
        projection[4] = MediaStore.Audio.Albums.NUMBER_OF_SONGS
        // Datenbankabfrage und Weitergabe an den Provider
        val cursor = contentResolver.query(android.provider.MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, null,null,null,orderby)
        val list = MutableList(cursor.count,
                { i -> MediaBrowserCompat.MediaItem(MediaDescriptionCompat.Builder().setMediaId(i.toString()).build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE) } )
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
        val result = am.requestAudioFocus(amCallback, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
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
                updatePlaybackstate(PlaybackStateCompat.STATE_PLAYING)
                // starte im Vordergrund mit Benachrichtigung
                this.startForeground(ID, showNotification(PlaybackStateCompat.STATE_PLAYING, currentMediaMetaData).build())
            }
            musicPlayer!!.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MusicPlayerError: " + what + "; " + extra)
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
            musicPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
            musicPlayer!!.setDataSource(currentMediaFile)
            musicPlayer!!.prepareAsync()
        }
    }
    fun handleOnPlayFromId(pId: String?){
        playingQueue.clear()
        msession.setQueue(playingQueue)
        positionMs = -1
        currentMediaId = pId
        currentMediaFile = musicProvider.getFileFromMediaId(currentMediaId)
        handleOnPlay()
    }
    fun handleOnPrepareFromId(pId: String?){
        // Nach dem Neustart der App und dem Wiederherstellen der Playliste
        positionMs = -1
        currentMediaId = pId
        currentMediaFile = musicProvider.getFileFromMediaId(currentMediaId)
        updateMetadata()
    }
    fun handleOnPause() {
        musicPlayer!!.pause()
        updatePlaybackstate(PlaybackStateCompat.STATE_PAUSED)
        this.stopForeground(false)
    }
    fun handleOnStop(){
        Log.d(TAG, "handleOnStop")
        // update Metadata, state und die Playliste
        playingQueue.clear()
        playingID = 0
        msession.setQueue(playingQueue)
        updatePlaybackstate(PlaybackStateCompat.STATE_STOPPED)
        // AudioFocus freigeben
        am.abandonAudioFocus(amCallback)
        audioFocus = AUDIO_LOSS
        // Session und Service beenden
        msession.release()
        this.stopSelf()
        serviceStarted = false
        // Player stoppen
        musicPlayer!!.stop()
        musicPlayer!!.reset()
        musicPlayer!!.release()
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

    // Update der Abspieldaten und Benachrichtigung erstellen
    fun showNotification(state: Int, metadata: MediaMetadataCompat?): NotificationCompat.Builder{
        // TODO Cancel Button anzeigen
        val nb = NotificationCompat.Builder(this)
        nb.setContentTitle(metadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
        nb.setContentText(metadata?.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
        nb.setSmallIcon(R.drawable.notification_stat_playing)
        nb.setLargeIcon(BitmapFactory.decodeFile(metadata?.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)))
        // Media Style aktivieren
        nb.setStyle(NotificationCompat.MediaStyle()
                .setMediaSession(sessionToken)
                .setShowActionsInCompactView(1,2))
        val i = Intent(applicationContext, MainActivity::class.java)
        nb.setContentIntent(PendingIntent.getActivity(this, 0, i,0))
        // korrekt anzeigen, ob Shuffle aktiviert ist
        if(msession.controller.isShuffleModeEnabled){
            nb.addAction(R.mipmap.ic_shuffle, "Shuffle", null)
        } else {
            nb.addAction(R.color.transparent, "Shuffle", null)
        }
        // passendes Icon für Play / Pause anzeigen
        if(state == PlaybackStateCompat.STATE_PLAYING){
            nb.addAction(R.mipmap.ic_pause, "Pause", MediaButtonReceiver.buildMediaButtonPendingIntent(applicationContext, PlaybackStateCompat.ACTION_PAUSE))
        } else {
            nb.addAction(R.mipmap.ic_play, "Play", MediaButtonReceiver.buildMediaButtonPendingIntent(applicationContext, PlaybackStateCompat.ACTION_PLAY))
        }
        nb.addAction(R.mipmap.ic_next, "Next", MediaButtonReceiver.buildMediaButtonPendingIntent(applicationContext, PlaybackStateCompat.ACTION_SKIP_TO_NEXT))
        return nb
    }
    fun updateMetadata(){
        var number = "0"
        if(!playingQueue.isEmpty()){
            number = playingQueue.size.toString()
        }
        // Daten aktualisieren und setzen
        currentMediaMetaData = musicProvider.getMediaDescription(currentMediaId, number)
        msession.setMetadata(currentMediaMetaData)
    }
    fun updatePlaybackstate(state: Int){
        if(state == PlaybackStateCompat.STATE_PLAYING){
            // anfangen oder weiterspielen
            playbackState.setActions(PlaybackStateCompat.ACTION_PAUSE
                    or PlaybackStateCompat.ACTION_STOP
                    or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    or PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE_ENABLED)
            val position = musicPlayer!!.currentPosition.toLong()
            positionMs = position.toInt()
            playbackState.setState(PlaybackStateCompat.STATE_PLAYING, position, 1.0f)
            nm.notify(ID, showNotification(state, currentMediaMetaData).build())
        } else if(state == PlaybackStateCompat.STATE_PAUSED ||
                state == PlaybackStateCompat.STATE_SKIPPING_TO_NEXT){
            playbackState.setActions(PlaybackStateCompat.ACTION_PLAY
                    or PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                    or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    or PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE_ENABLED)
            val position = musicPlayer!!.currentPosition.toLong()
            positionMs = position.toInt()
            playbackState.setState(PlaybackStateCompat.STATE_PAUSED, position, 1.0f)
            nm.notify(ID,showNotification(state, currentMediaMetaData).build())
        } else if(state == PlaybackStateCompat.STATE_STOPPED){
            playbackState.setActions(PlaybackStateCompat.ACTION_PLAY
                    or PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                    or PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE_ENABLED)
            positionMs = 0
            playbackState.setState(PlaybackStateCompat.STATE_STOPPED, 0, 1.0f)
            nm.cancel(ID)
        }
        msession.setPlaybackState(playbackState.build())
    }

    // innere Callbacks vom MainActivity und vom AudioManager
    inner class MusicSessionCallback: MediaSessionCompat.Callback(){
        override fun onPlay() {
            handleOnPlay()
        }
        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle) {
            handleOnPlayFromId(mediaId)
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

        override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {
            handleOnPrepareFromId(mediaId)
        }
        override fun onSetShuffleModeEnabled(enabled: Boolean) {
            msession.setShuffleModeEnabled(enabled)
        }
        override fun onAddQueueItem(description: MediaDescriptionCompat) {
            playingQueue.add(MediaSessionCompat.QueueItem(description, playingID++))
            msession.setQueue(playingQueue)
            // Zum sicherstellen, dass die richtige Anzahl an Liedern angezeigt wird
            updateMetadata()
        }
        override fun onRemoveQueueItem(description: MediaDescriptionCompat) {
            super.onRemoveQueueItem(description)
            var i = 0
            // Description aus der Liste entfernen und aktualisieren
            while(i < playingQueue.size){
                if(playingQueue[i].description == description){
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
    inner class AudioFocusCallback: AudioManager.OnAudioFocusChangeListener{
        override fun onAudioFocusChange(focusChange: Int) {
            // Fälle abfragen und entsprechend pausieren oder leiser stellen
            if(focusChange == AudioManager.AUDIOFOCUS_LOSS){
                handleOnStop()
                audioFocus = AUDIO_LOSS
            } else if(focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                if(musicPlayer!!.isPlaying){
                    handleOnPause()
                    audioFocus = AUDIO_PAUSE_PLAYING
                }
            } else if(focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK){
                musicPlayer!!.setVolume(0.3f, 0.3f)
                audioFocus = AUDIO_DUCK
            } else if(focusChange == AudioManager.AUDIOFOCUS_GAIN){
                if(audioFocus == AUDIO_PAUSE_PLAYING){
                    handleOnPlay()
                } else {
                    musicPlayer!!.setVolume(0.8f, 0.8f)     // generell leiser spielen
                }
            }
        }
    }
}