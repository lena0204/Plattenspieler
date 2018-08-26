package com.lk.music_service_library.observables

import android.media.session.PlaybackState
import android.util.Log
import com.lk.music_service_library.models.*
import com.lk.music_service_library.observables.PlaybackActions.*
import com.lk.music_service_library.utils.PlaybackDataUpdater

/**
 * Erstellt von Lena am 12.05.18.
 * Verwaltet alle Daten, die an das Playback gekoppelt sind (Metadaten, Wiedergabeliste, Status)
 */
object PlaybackDataObservable: ObserverAwareObservable() {

    private const val TAG = "PlaybackDataObservable"

    var musicQueue = MusicList()
        private set
    private var musicStack = MediaStack()

    var metadata = MusicMetadata()
        private set
    var musicFilePath = ""
        private set
    var musicId = ""
        private set

    private var playbackState = PlaybackState.Builder().build()
    var shuffleOn = false
        private set
    var positionMs = 0L
        private set

    private fun notifiy(message: PlaybackActions){
        setChanged()
        notifyObservers(message)
    }

    fun playFromId(titleId: String){
        musicQueue.removeAll()
        musicStack.popAll()
        setNewMusicInformation(titleId)
        notifiy(ACTION_TRY_PLAYING)
    }

    private fun setNewMusicInformation(titleId: String){
        shuffleOn = false
        positionMs = 0
        musicId = titleId
    }

    fun setQueue(queue: MusicList){
        musicQueue = queue
        notifiy(ACTION_UPDATE_QUEUE)
    }

    fun setShuffleOn(){
        shuffleOn = true
        notifiy(ACTION_UPDATE_PLAYBACKSTATE)
    }

    fun prepareFromId(titleId: String, newMetadata: MusicMetadata){
        setNewMusicInformation(titleId)
        updateMetadata(newMetadata)
    }

    fun tryPlaying(){
        notifiy(ACTION_TRY_PLAYING)
    }

    fun canPlay(newMetadata: MusicMetadata){
        Log.v(TAG, "canPlay: " + metadata.title)
        metadata = newMetadata
        musicFilePath = metadata.path
        updateMetadata(metadata)
        updatePlaybackstate(PlaybackState.STATE_PLAYING)
        notifiy(ACTION_CAN_PLAY)
    }

    fun pause(position: Long){
        positionMs = position
        updatePlaybackstate(PlaybackState.STATE_PAUSED)
        notifiy(ACTION_PAUSE)
    }

    fun next(){
        if(!musicQueue.isEmpty()){
            musicStack.pushMedia(metadata)
            val queueItem = musicQueue.getItemAt(0)
            musicQueue.removeItemAt(0)
            positionMs = 0
            musicId = queueItem.id
            notifiy(ACTION_UPDATE_QUEUE)
            notifiy(ACTION_TRY_PLAYING)
        } else {
            stop()
        }
    }

    fun previous(position: Long){
        positionMs = position
        val previous = musicStack.popMedia()
        if(previous != null){
            skipToPrevious(positionMs, previous)
        } else {
            resetPosition()
            Log.v(TAG, "Vorgänger ist NULL, nichts passiert.")
        }
        notifiy(ACTION_TRY_PLAYING)
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
        positionMs = 0
    }

    private fun addAsFirstToQueue(previous: MusicMetadata){
        musicQueue.addFirstItem(previous)
        notifiy(ACTION_UPDATE_QUEUE)
        musicId = previous.id
        metadata = previous
    }

    fun stop(){
        removeMusicData()
        notifiy(ACTION_STOP)
        updatePlaybackstate(PlaybackState.STATE_STOPPED)
        updateMetadata(metadata)
    }

    private fun removeMusicData(){
        musicQueue.removeAll()
        musicStack.popAll()
        shuffleOn = false
        musicId = ""
        metadata = MusicMetadata()
        notifiy(ACTION_UPDATE_QUEUE)
    }

    private fun updateMetadata(newMetadata: MusicMetadata){
        metadata = PlaybackDataUpdater.updateMetadata(newMetadata)
        notifiy(ACTION_UPDATE_METADATA)
    }

    private fun updatePlaybackstate(state: Int){
        playbackState = PlaybackDataUpdater.updatePlaybackstate(state)
        notifiy(ACTION_UPDATE_PLAYBACKSTATE)
    }

    fun getQueueLimitedTo30(): MusicList {
        var shorterQueue = MusicList()
        if(musicQueue.countItems() > 30) {
            for(i in 0..30){
                shorterQueue.addItem(musicQueue.getItemAt(i))
            }
        } else {
            shorterQueue = musicQueue
        }
        Log.v(TAG, "getShorterQueue: Größe: " + shorterQueue.countItems())
        return shorterQueue
    }

    fun getState(): PlaybackState {
        logState()
        return playbackState
    }

    private fun logState(){
        Log.v(TAG, "getState: state (2 -> pause, 3 -> play, " +
                "10 -> next, 9 -> previous, 1 -> stop): " + playbackState.state)
        Log.v(TAG, "getState: shuffle on $shuffleOn")
    }

    override fun toString(): String {
        return "PlaybackObservable: mit Titel: " + metadata.title +
                ", State: " + playbackState.state + ", Queuelänge: " + musicQueue.countItems() +
                ", alle Observer: " + this.countObservers()
    }

}