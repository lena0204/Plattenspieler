package com.lk.music_service_library.background

import android.annotation.TargetApi
import android.content.Context
import android.media.*
import android.media.browse.MediaBrowser
import android.media.session.PlaybackState
import android.os.Build
import android.util.Log
import com.lk.music_service_library.models.*
import com.lk.music_service_library.utils.*

/**
 * Erstellt von Lena am 13.05.18.
 * Verwaltet das Playback mit allen Methoden, inkl. Audiofokus und Update der Metadaten
 */
class MusicPlayback(
        private val service: MusicService,
        private val notification: MusicNotification) {

    private val TAG = "MusicPlayback"
    private val audioManager = service.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    //private val musicProvider = MusicProvider(service.applicationContext)
    private val playbackData = PlaybackData(service, notification)
    private val audioFocusCallback = AudioFocusCallback()
    /*private val ID = 88
    private val notificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
*/
    private val audioAttr = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA).build()

    private var musicPlayer: MediaPlayer? = null
    /*private var playingQueue = MusicList()
    private var mediaStack = MediaStack()
    private var currentMusicMetadata = MusicMetadata()
    private var currentMusicId = ""
    private var playbackStateBuilder = PlaybackState.Builder()

    private var positionMs = -1
    var shuffleOn = false*/

    @TargetApi(26)
    private var audioFocusRequest: AudioFocusRequest? = null
    private var audioFucosStatus = EnumAudioFucos.AUDIO_LOSS


    fun sendAlbumChildren(albumid: String): MutableList<MediaBrowser.MediaItem>
        = playbackData.getTitlesFromProvider(albumid)

    fun sendRootChildren(): MutableList<MediaBrowser.MediaItem>
        = playbackData.getAlbumsFromProvider()

    fun setQueue(queue: MusicList){
        playbackData.setQueue(queue)
    }

    fun handleOnPlayFromId(pId: String){
        Log.i(TAG, "handleOnPlayFromId")
        playbackData.resetPlaybackParameters()
        playbackData.setQueueAndMusicId(pId)
        handleOnPlay()
    }

    fun handleOnPrepareFromId(pId: String){
        Log.d(TAG, "handleOnPrepareFromId")
        playbackData.prepareForPlaying(pId)
    }

    fun handleOnPlay(){
        if(canPlay()) {
            service.startServiceIfNecessary()
            playbackData.updateMetadata()
            Log.v(TAG, "handleOnPlay: " + playbackData.currentMusicMetadata.title)
            setupMediaPlayer()
        }
    }

    private fun canPlay(): Boolean {
        if(musicPlayer != null){
            if(musicPlayer!!.isPlaying) musicPlayer!!.stop()
        }
        if(playbackData.currentMusicId != "" &&
                checkAudioFocus() == AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
            audioFucosStatus = EnumAudioFucos.AUDIO_FOCUS
            return true
        }
        return false
    }

    private fun checkAudioFocus(): Int{
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(audioAttr)
                    .setOnAudioFocusChangeListener(audioFocusCallback)
                    .build()
            audioManager.requestAudioFocus(audioFocusRequest)
        } else {
            audioManager.requestAudioFocus(audioFocusCallback, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
    }

    private fun setupMediaPlayer(){
        musicPlayer = MediaPlayer()
        musicPlayer!!.setOnPreparedListener { _ -> startPlaying() }
        musicPlayer!!.setOnErrorListener { _, what, extra ->
            Log.e(TAG, "MusicPlayerError: $what; $extra")
            false
        }
        musicPlayer!!.setOnCompletionListener { _ -> playNextTitleAfterCompletion() }
        musicPlayer!!.setAudioAttributes(audioAttr)
        musicPlayer!!.setDataSource(playbackData.getFileForSong())
        musicPlayer!!.prepareAsync()
    }

    private fun startPlaying(){
        service.playingstate = EnumPlaybackState.STATE_PLAY
        if (playbackData.positionMs != -1) {
            musicPlayer!!.seekTo(playbackData.positionMs)
        }
        musicPlayer!!.start()
        updatePlaybackstate(PlaybackState.STATE_PLAYING)
        playbackData.startServiceWithNotification()
    }

    private fun playNextTitleAfterCompletion(){
        if (!playbackData.isQueueEmpty()) {
            playbackData.prepareAfterCompletion()
            handleOnPlay()
        } else {
            handleOnStop()
        }
    }

    fun handleOnPause() {
        Log.i(TAG, "handleOnPause")
        service.playingstate = EnumPlaybackState.STATE_PAUSE
        updatePlaybackstate(PlaybackState.STATE_PAUSED)
        musicPlayer!!.pause()
        service.stopForeground(false)
    }

    fun handleOnPrevious(position: Long){
        playbackData.setPrevious(position)
        handleOnPlay()
    }

    // PROBLEM_ häufige Nutzung von Next und previous zusammen führt zu doppelten / übersprungenen Liedern
    fun handleOnNext(){
        if(!playbackData.isQueueEmpty()){
            playbackData.setNext()
            handleOnPlay()
        } else {
            handleOnStop()
        }
    }

    fun handleOnStop(){
        Log.d(TAG, "handleOnStop")
        service.playingstate = EnumPlaybackState.STATE_STOP
        updatePlaybackstate(PlaybackState.STATE_STOPPED)
        playbackData.removeMusicDataFromSession()
        releaseAudioFocus()
        releaseMusicPlayer()
        service.stopService()
    }

    private fun releaseAudioFocus(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null){
            audioManager.abandonAudioFocusRequest(audioFocusRequest)
        } else {
            audioManager.abandonAudioFocus(audioFocusCallback)
        }
        audioFucosStatus = EnumAudioFucos.AUDIO_LOSS
    }

    private fun releaseMusicPlayer(){
        musicPlayer?.stop()
        musicPlayer?.reset()
        musicPlayer?.release()
        musicPlayer = null
    }

    private fun updatePlaybackstate(state: Int){
        setCurrentPlaybackPosition()
        playbackData.updatePlaybackstate(state)
    }

    private fun setCurrentPlaybackPosition() {
        playbackData.positionMs = if(musicPlayer == null) {
            0
        } else {
            musicPlayer!!.currentPosition
        }
    }

    /*private fun updateMetadata(){
        val remainingSongs = getRemainingSongNumber()
        currentMusicMetadata = musicProvider.getMediaMetadata(currentMusicId, remainingSongs)
        setAlbumcoverIfProvided()
        sendBroadcastForLightningLauncher()
        service.setMetadataToSession(currentMusicMetadata)
    }

    private fun getRemainingSongNumber(): String {
        var remainingSongs = "0"
        if(!playingQueue.isEmpty()){
            remainingSongs = playingQueue.countItems().toString()
        }
        return remainingSongs
    }

    private fun setAlbumcoverIfProvided(){
        if(currentMusicMetadata.isEmpty()){
            Log.e(TAG, "Metadaten sind leer")
        } else {
            currentMusicMetadata.cover = service.decodeAlbumcover(currentMusicMetadata.cover_uri)
        }
    }

    // IDEA_ Broadcast sticky?
    private fun sendBroadcastForLightningLauncher(){
        val extras = Bundle()
        val track = Bundle()
        track.putString("title", currentMusicMetadata.title)
        track.putString("album", currentMusicMetadata.album)
        track.putString("artist",currentMusicMetadata.artist)
        extras.putBundle("track", track)
        extras.putString("aaPath", currentMusicMetadata.cover_uri)
        service.sendBroadcast(Intent("com.lk.plattenspieler.metachanged").putExtras(extras))
    }

    private fun updatePlaybackstate(state: Int){
        playbackStateBuilder = PlaybackState.Builder()
        Log.d(TAG, state.toString())
        setCurrentPlaybackPosition()
        setPlaybackActions(state)
        setNotification(state)
        setShuffleBuilderExtra()
        playbackStateBuilder.setState(state, positionMs.toLong(), 1.0f)
        service.setPlaybackStateToSession(playbackStateBuilder.build())
    }

    private fun setCurrentPlaybackPosition() {
        positionMs = if(musicPlayer == null) {
            0
        } else {
            musicPlayer!!.currentPosition
        }
    }

    private fun setPlaybackActions(state: Int){
        when(state) {
            PlaybackState.STATE_PLAYING -> setPlayingActions()
            PlaybackState.STATE_PAUSED -> setPausedActions()
            PlaybackState.STATE_STOPPED -> setStoppedActions()
        }
    }

    private fun setPlayingActions(){
        playbackStateBuilder.setActions(PlaybackState.ACTION_PAUSE
                or PlaybackState.ACTION_STOP
                or PlaybackState.ACTION_SKIP_TO_NEXT
                or PlaybackState.ACTION_SKIP_TO_PREVIOUS)
    }

    private fun setPausedActions(){
        playbackStateBuilder.setActions(PlaybackState.ACTION_PLAY
                or PlaybackState.ACTION_PLAY_FROM_MEDIA_ID
                or PlaybackState.ACTION_SKIP_TO_NEXT
                or PlaybackState.ACTION_SKIP_TO_PREVIOUS
                or PlaybackState.ACTION_STOP)
    }

    private fun setStoppedActions(){
        playbackStateBuilder.setActions(PlaybackState.ACTION_PLAY
                or PlaybackState.ACTION_PLAY_FROM_MEDIA_ID)
    }

    private fun setNotification(state: Int){
        when(state){
            PlaybackState.STATE_PLAYING,
            PlaybackState.STATE_PAUSED ->
                notificationManager.notify(ID, notification
                        .showNotification(state, currentMusicMetadata, shuffleOn).build())
            PlaybackState.STATE_STOPPED -> notificationManager.cancel(ID)
        }
    }

    private fun setShuffleBuilderExtra(){
        val extras = Bundle()
        Log.v(TAG, "UpdatePlaybackstate: shuffleOn is $shuffleOn")
        extras.putBoolean("shuffle", shuffleOn)
        playbackStateBuilder.setExtras(extras)
    }*/

    fun addAllSongsToPlayingQueue(){
        Log.d(TAG, "addAllSongsToPlayingQueue")
        val playingID = playbackData.getFirstSong()
        handleOnPlayFromId(playingID)
        playbackData.shuffleOn = true
        playbackData.fillQueueAsync(playingID)
    }

    inner class AudioFocusCallback: AudioManager.OnAudioFocusChangeListener{
        override fun onAudioFocusChange(focusChange: Int) {
            when(focusChange){
                AudioManager.AUDIOFOCUS_LOSS -> {
                    handleOnStop()
                    audioFucosStatus = EnumAudioFucos.AUDIO_LOSS
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    if(musicPlayer!!.isPlaying){
                        handleOnPause()
                        audioFucosStatus = EnumAudioFucos.AUDIO_PAUSE_PLAYING
                    }
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    musicPlayer!!.setVolume(0.3f, 0.3f)
                    audioFucosStatus = EnumAudioFucos.AUDIO_DUCK
                }
                AudioManager.AUDIOFOCUS_GAIN -> {
                    if(audioFucosStatus == EnumAudioFucos.AUDIO_PAUSE_PLAYING){
                        handleOnPlay()
                    } else {
                        musicPlayer!!.setVolume(0.8f, 0.8f)
                        // generell leiser spielen
                    }
                }
            }
        }
    }

}