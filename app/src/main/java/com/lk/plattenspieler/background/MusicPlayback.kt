package com.lk.plattenspieler.background

import android.annotation.TargetApi
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.*
import android.media.browse.MediaBrowser
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.lk.plattenspieler.models.*
import com.lk.plattenspieler.utils.*

/**
 * Erstellt von Lena am 13.05.18.
 * Verwaltet das Playback mit allen Methoden, inkl. Audiofokus und Update der Metadaten
 */
class MusicPlayback(private val service: MusicService, private val notification: MusicNotification) {

    private val TAG = "MusicPlayback"

    private val amCallback = AudioFocusCallback()
    private val ID: Int = 88
    private val audioAttr = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA).build()
    @TargetApi(26)
    private lateinit var audioFocusRequest: AudioFocusRequest

    private var nm = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var am: AudioManager = service.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var musicProvider = MusicProvider(service.applicationContext)
    private var playingQueue = MusicList()
    private var mediaStack = MediaStack()
    private var currentMusicMetadata = MusicMetadata()
    private var currentMusicId = ""
    private var musicPlayer: MediaPlayer? = null
    // Statusvariablen, um verschiedene Sachen abzufragen
    private var positionMs = -1
    private var audioFocus = EnumAudioFucos.AUDIO_LOSS
    var shuffleOn = false

    fun sendAlbumChildren(albumid: String): MutableList<MediaBrowser.MediaItem>
        = musicProvider.getTitlesForAlbumID(albumid).getMediaItemList()

    fun sendRootChildren(): MutableList<MediaBrowser.MediaItem>
        = musicProvider.getAlbums().getMediaItemList()

    fun setQueue(queue: MusicList){
        playingQueue = queue
        updateMetadata()
    }

    // Ist Audiofokus und ein Titel vorhanden, der abgespielt werden soll
    private fun canPlay(): Boolean {
        if(musicPlayer != null){
            if(musicPlayer!!.isPlaying) musicPlayer!!.stop()
        }
        val result = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(audioAttr)
                    .setOnAudioFocusChangeListener(amCallback)
                    .build()
            am.requestAudioFocus(audioFocusRequest)
        } else {
            am.requestAudioFocus(amCallback, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        if(currentMusicId != "" && result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
            audioFocus = EnumAudioFucos.AUDIO_FOCUS
            return true
        }
        return false
    }

    fun handleOnPlay(){
        if(canPlay()) {
            service.setupSession()
            updateMetadata()
            Log.d(TAG, "handleOnPlay: " + currentMusicId + ", " + currentMusicMetadata.title)
            musicPlayer = MediaPlayer()
            musicPlayer!!.setOnPreparedListener { _ ->
                service.playingstate = EnumPlaybackState.STATE_PLAY
                // spielen starten
                if (positionMs != -1) {
                    musicPlayer!!.seekTo(positionMs)
                }
                musicPlayer!!.start()
                updatePlaybackstate(PlaybackState.STATE_PLAYING)
                // starte im Vordergrund mit Benachrichtigung
                service.startForeground(ID, notification
                        .showNotification(PlaybackState.STATE_PLAYING, currentMusicMetadata, shuffleOn)
                        .build())
            }
            musicPlayer!!.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MusicPlayerError: $what; $extra")
                false
            }
            musicPlayer!!.setOnCompletionListener { _ ->
                // neuen Titel abspielen falls vorhanden; von der Schlange holen und Werte zurücksetzen
                if (!playingQueue.isEmpty()) {
                    mediaStack.pushMedia(currentMusicMetadata)
                    Log.d(TAG, "onPlay(), Größe: " + playingQueue.countItems())
                    val queueItem = playingQueue.getItemAt(0)
                    playingQueue.removeItem(queueItem)
                    service.sendQueue(playingQueue)
                    positionMs = -1
                    currentMusicId = queueItem.id
                    handleOnPlay()
                } else {
                    handleOnStop()
                }
            }
            musicPlayer!!.setAudioAttributes(audioAttr)
            musicPlayer!!.setDataSource(musicProvider.getFileFromMediaId(currentMusicId))
            musicPlayer!!.prepareAsync()
        }
    }

    fun handleOnPlayFromId(pId: String){
        Log.i(TAG, "handleOnPlayFromId")
        playingQueue.removeAll()
        shuffleOn = false
        // Standardmäßig ausschalten wenn neu abgespielt wird, falls shuffle wird das später gesetzt
        service.sendQueue(playingQueue)
        positionMs = -1
        currentMusicId = pId
        handleOnPlay()
    }
    fun handleOnPrepareFromId(pId: String){
        // Nach dem Neustart der App und dem Wiederherstellen der Playliste
        Log.d(TAG, "handleOnPrepareFromId")
        positionMs = -1
        currentMusicId = pId
        updateMetadata()
    }
    fun handleOnPause() {
        service.playingstate = EnumPlaybackState.STATE_PAUSE
        Log.i(TAG, "handleOnPause")
        musicPlayer!!.pause()
        updatePlaybackstate(PlaybackState.STATE_PAUSED)
        service.stopForeground(false)
    }
    fun handleOnPrevious(position: Long){
        val previous = mediaStack.popMedia()
        Log.d(TAG, mediaStack.toString())
        if(previous != null){
            if(position >= 15000){
                positionMs = -1      // zum Anfang des Liedes skippen, wenn schon mehr als 15s gespielt wurde
            } else {
                Log.d(TAG, "Vorgänger ist vorhanden mit Titel: " + previous.title)
                playingQueue.addFirstItem(previous)
                currentMusicId = previous.id
                currentMusicMetadata = previous
                service.sendQueue(playingQueue)
            }
        } else {
            positionMs = -1     // erstes Lied in der Queue
            Log.d(TAG, "Vorgänger ist NULL, nichts passiert.")
        }
        handleOnPlay()
    }
    fun handleOnNext(){
        if(!playingQueue.isEmpty()){
            // PROBLEM_ häufige Nutzung von Next und previous zusammen führt zu doppelten / übersprungenen Liedern
            // von der Schlange holen und Werte zurücksetzen
            mediaStack.pushMedia(currentMusicMetadata)
            Log.i(TAG, "onNext(), Größe: " + playingQueue.countItems())
            val queueItem = playingQueue.getItemAt(0)
            playingQueue.removeItemAt(0)
            positionMs = 0
            currentMusicId = queueItem.id
            service.sendQueue(playingQueue)
            handleOnPlay()
        } else {
            handleOnStop()
        }
    }
    fun handleOnStop(){
        Log.d(TAG, "handleOnStop")
        service.playingstate = EnumPlaybackState.STATE_STOP
        // update Metadata, state und die Playliste
        playingQueue.removeAll()
        mediaStack.popAll()
        shuffleOn = false
        currentMusicId = ""
        currentMusicMetadata = MusicMetadata()
        service.sendQueue(playingQueue)
        updatePlaybackstate(PlaybackState.STATE_STOPPED)
        updateMetadata()
        // AudioFocus freigeben
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            am.abandonAudioFocusRequest(audioFocusRequest)
        } else {
            am.abandonAudioFocus(amCallback)
        }
        audioFocus = EnumAudioFucos.AUDIO_LOSS
        // Player stoppen
        musicPlayer?.stop()
        musicPlayer?.reset()
        musicPlayer?.release()
        musicPlayer = null
        service.handleOnStopService()
    }

    private fun updateMetadata(){
        var number = "0"
        if(!playingQueue.isEmpty()){
            number = playingQueue.countItems().toString()
        }
        // Daten aktualisieren und setzen
        currentMusicMetadata = musicProvider.getMediaMetadata(currentMusicId, number)
        if(currentMusicMetadata.isEmpty()){
            Log.e(TAG, "Metadaten sind null")
        }
        sendBroadcastForLauncher()
        service.sendMetadata(currentMusicMetadata)
    }
    private fun sendBroadcastForLauncher(){
        val extras = Bundle()
        val track = Bundle()
        track.putString("title", currentMusicMetadata.title)
        track.putString("album", currentMusicMetadata.album)
        track.putString("artist",currentMusicMetadata.artist)
        extras.putBundle("track", track)
        extras.putString("aaPath", currentMusicMetadata.cover_uri)
        service.sendBroadcast(Intent("com.lk.plattenspieler.metachanged").putExtras(extras))
        // IDEA_ Broadcast sticky ?
    }
    private fun updatePlaybackstate(state: Int){
        val pb = PlaybackState.Builder()
        Log.d(TAG, state.toString())
        val position = if(musicPlayer == null) {
            0L
        } else {
            musicPlayer!!.currentPosition.toLong()
        }
        positionMs = position.toInt()
        when(state){
            PlaybackState.STATE_PLAYING -> {
                // anfangen oder weiterspielen
                pb.setActions(PlaybackState.ACTION_PAUSE
                        or PlaybackState.ACTION_STOP
                        or PlaybackState.ACTION_SKIP_TO_NEXT
                        or PlaybackState.ACTION_SKIP_TO_PREVIOUS)
                nm.notify(ID, notification
                        .showNotification(state, currentMusicMetadata, shuffleOn).build())
            }
            PlaybackState.STATE_PAUSED -> {
                pb.setActions(PlaybackState.ACTION_PLAY
                        or PlaybackState.ACTION_PLAY_FROM_MEDIA_ID
                        or PlaybackState.ACTION_SKIP_TO_NEXT
                        or PlaybackState.ACTION_SKIP_TO_PREVIOUS
                        or PlaybackState.ACTION_STOP)
                nm.notify(ID, notification
                        .showNotification(state, currentMusicMetadata, shuffleOn).build())
            }
            PlaybackState.STATE_STOPPED -> {
                pb.setActions(PlaybackState.ACTION_PLAY
                        or PlaybackState.ACTION_PLAY_FROM_MEDIA_ID)
                nm.cancel(ID)
            }
        }
        pb.setState(state, position, 1.0f)
        val extras = Bundle()
        extras.putBoolean("shuffle", shuffleOn)
        pb.setExtras(extras)
        service.sendPlaybackstate(pb.build())
    }

    fun addAllSongsToPlayingQueue(){
        Log.d(TAG, "addAllSongsToPlayingQueue")
        val playingID = musicProvider.getFirstTitle()
        handleOnPlayFromId(playingID)
        // alle anderen Songs zufällig hinzufügen
        val listSongs = musicProvider.getAllTitles(playingID)
        playingQueue = QueueCreation.createQueueRandom(listSongs, playingID)
        service.sendQueue(playingQueue)
        updateMetadata()
        nm.notify(ID, notification
                .showNotification(PlaybackState.STATE_PLAYING, currentMusicMetadata, shuffleOn)
                .build())
        shuffleOn = true
    }

    inner class AudioFocusCallback: AudioManager.OnAudioFocusChangeListener{
        override fun onAudioFocusChange(focusChange: Int) {
            // Fälle abfragen und entsprechend pausieren oder leiser stellen
            when(focusChange){
                AudioManager.AUDIOFOCUS_LOSS -> {
                    handleOnStop()
                    audioFocus = EnumAudioFucos.AUDIO_LOSS
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    if(musicPlayer!!.isPlaying){
                        handleOnPause()
                        audioFocus = EnumAudioFucos.AUDIO_PAUSE_PLAYING
                    }
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    musicPlayer!!.setVolume(0.3f, 0.3f)
                    audioFocus = EnumAudioFucos.AUDIO_DUCK
                }
                AudioManager.AUDIOFOCUS_GAIN -> {
                    if(audioFocus == EnumAudioFucos.AUDIO_PAUSE_PLAYING){
                        handleOnPlay()
                    } else {
                        musicPlayer!!.setVolume(0.8f, 0.8f)     // generell leiser spielen
                    }
                }
            }
        }
    }

}