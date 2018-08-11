package com.lk.music_service_library.background

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.session.PlaybackState
import android.os.Bundle
import android.util.Log
import com.lk.music_service_library.models.*
import com.lk.music_service_library.utils.QueueCreation
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch

/**
 * Erstellt von Lena am 11.08.18.
 */
class PlaybackData (
        private val service: MusicService,
        private val notification: MusicNotification) {

    private val TAG = "PlaybackData"

    private val ID = 88
    private val notificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val musicProvider = MusicProvider(service.applicationContext)

    private var playingQueue = MusicList()
    private var mediaStack = MediaStack()
    private var playbackStateBuilder = PlaybackState.Builder()

    var positionMs = -1
    var shuffleOn = false
    var currentMusicMetadata = MusicMetadata()
    var currentMusicId = ""

    fun getAlbumsFromProvider() = musicProvider.getAlbums().getMediaItemList()

    fun getTitlesFromProvider(albumid: String) = musicProvider.getTitlesForAlbumID(albumid).getMediaItemList()

    fun getFileForSong(): String = musicProvider.getFilePathFromMediaId(currentMusicId)

    fun getFirstSong(): String = musicProvider.getFirstTitleForShuffle()


    fun fillQueueAsync(playingID: String){
        launch(CommonPool) {
            val listSongs = musicProvider.getAllTitles(playingID)
            playingQueue = QueueCreation.createQueueRandom(listSongs, playingID)
            service.setQueueToSession(playingQueue)
            // updateMetadata notwendig, weil eine Schlange braucht, um die Länge dieser zu bestimmen
            updateMetadata()
            notificationManager.notify(ID, notification
                    .showNotification(PlaybackState.STATE_PLAYING, currentMusicMetadata, shuffleOn)
                    .build())
        }
    }

    fun setQueue(queue: MusicList){
        playingQueue = queue
        updateMetadata()
    }

    fun isQueueEmpty() : Boolean = playingQueue.isEmpty()


    fun resetPlaybackParameters(){
        playingQueue.removeAll()
        mediaStack.popAll()
        shuffleOn = false
        positionMs = -1
    }

    fun setQueueAndMusicId(id: String){
        service.setQueueToSession(playingQueue)
        currentMusicId = id
    }

    fun prepareForPlaying(id: String){
        positionMs = -1
        currentMusicId = id
        updateMetadata()
    }

    fun startServiceWithNotification(){
        service.startForeground(ID, notification
                .showNotification(PlaybackState.STATE_PLAYING, currentMusicMetadata, shuffleOn)
                .build())
    }

    fun prepareAfterCompletion(){
        mediaStack.pushMedia(currentMusicMetadata)
        prepareNextQueueItem()
    }

    private fun prepareNextQueueItem(){
        Log.d(TAG, "Größe: " + playingQueue.countItems())
        val queueItem = playingQueue.getItemAt(0)
        playingQueue.removeItemAt(0)
        positionMs = 0
        currentMusicId = queueItem.id
        service.setQueueToSession(playingQueue)
    }

    fun setNext(){
        // von der Schlange holen und Werte zurücksetzen
        Log.i(TAG, "onNext(), Größe: " + playingQueue.countItems())
        mediaStack.pushMedia(currentMusicMetadata)
        prepareNextQueueItem()
    }


    fun setPrevious(position: Long){
        val previous = mediaStack.popMedia()
        Log.d(TAG, mediaStack.toString())
        if(previous != null){
            skipToPrevious(position, previous)
        } else {
            resetPosition()
            Log.v(TAG, "Vorgänger ist NULL, nichts passiert.")
        }
    }

    private fun skipToPrevious(position: Long, previous: MusicMetadata){
        if(position >= 15000){
            // wenn schon mehr als 15 Sekunden gespielt wurden, nur zum Anfang des Liedes springen
            resetPosition()
        } else {
            Log.d(TAG, "Vorgänger ist vorhanden mit Titel: " + previous.title)
            addAsFirstToQueue(previous)
        }
    }

    private fun resetPosition(){
        positionMs = -1
    }

    private fun addAsFirstToQueue(previous: MusicMetadata){
        playingQueue.addFirstItem(previous)
        currentMusicId = previous.id
        currentMusicMetadata = previous
        service.setQueueToSession(playingQueue)
    }





    fun updateMetadata(){
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


    fun updatePlaybackstate(state: Int){
        playbackStateBuilder = PlaybackState.Builder()
        Log.d(TAG, state.toString())
        setPlaybackActions(state)
        setNotification(state)
        setShuffleBuilderExtra()
        playbackStateBuilder.setState(state, positionMs.toLong(), 1.0f)
        service.setPlaybackStateToSession(playbackStateBuilder.build())
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
    }


    fun removeMusicDataFromSession(){
        playingQueue.removeAll()
        mediaStack.popAll()
        shuffleOn = false
        currentMusicId = ""
        currentMusicMetadata = MusicMetadata()
        service.setQueueToSession(playingQueue)
        updateMetadata()
    }

}