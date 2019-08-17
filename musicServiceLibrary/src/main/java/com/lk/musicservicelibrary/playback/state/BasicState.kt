package com.lk.musicservicelibrary.playback.state

import android.os.Bundle
import android.os.Handler
import android.util.Log
import com.lk.musicservicelibrary.main.MusicService
import com.lk.musicservicelibrary.models.MusicList
import com.lk.musicservicelibrary.playback.*
import com.lk.musicservicelibrary.utils.PlaybackStateFactory
import com.lk.musicservicelibrary.utils.QueueCreator

/**
 * Erstellt von Lena am 05/04/2019.
 */
abstract class BasicState(private var playback: PlaybackCallback) {

    private val TAG = "PlayerState"
    private val progressHandler = Handler()
    private val updateRunnable = updateRoutine()

    abstract var type: States
    abstract fun play()
    abstract fun pause()
    abstract fun skipToNext()
    abstract fun skipToPrevious()

    open fun playFromId(mediaId: String?, extras: Bundle?) {
        extras?.classLoader = this.javaClass.classLoader
        val shuffle = extras?.getBoolean(MusicService.SHUFFLE_KEY) ?: false
        val playingList = createPlayingList(mediaId, shuffle)
        playCurrentPlayingItem(playingList)
        playback.setPlayingList(playingList)
        playback.setPlaybackState(
            PlaybackStateFactory.createState(States.PLAYING, 0L, shuffle)
        )
        playback.setPlayerState(PlayingState(playback))
        updateProgress()
    }

    private fun createPlayingList(mediaId: String?, shuffle: Boolean): MusicList {
        var playingList = playback.getQueriedMediaList()
        if(shuffle){
            playingList = QueueCreator.shuffleQueueFromMediaList(playingList)
            playingList.setCurrentPlaying(0)
        } else {
            val position = playingList.indexOfFirst { data -> data.id == mediaId }
            playingList.setCurrentPlaying(position)
        }
        Log.v(TAG, "created Playlist with ${playingList.count()} items and currentplaying: ${playingList.getItemAtCurrentPlaying()}")
        return playingList
    }

    private fun playCurrentPlayingItem(playingList: MusicList) {
        val currentMetadata = playingList.getItemAtCurrentPlaying()
        if(currentMetadata != null && currentMetadata.path != ""){
            playback.getPlayer().preparePlayer(currentMetadata.path)
        } else {
            // TODO handle Error? -> should provoke a user feedback
            Log.e(TAG, "FirstMetadata is null!! List has ${playingList.count()} items.")
        }
    }

    private fun updateProgress() {
        progressHandler.postDelayed(updateRunnable, 500)
    }

    private fun updateRoutine(): Runnable {
        return Runnable {
            Log.v(TAG,"run...")
            val position = playback.getPlayer().getCurrentPosition()
            val type = playback.getPlayerState().type   // muss über Playback abgefragt werden, da sonst immer Stopped aufgerufen wird
            updateState(type, position.toLong())
        }
    }

    protected fun skipToNextOrStop(): Boolean {
        val playlist = playback.getPlayingList().value!!
        Log.v(TAG, "$playlist")
        return if(playlist.getCurrentPlaying() < playlist.size() - 1) {
            playlist.setCurrentPlaying(playlist.getCurrentPlaying() + 1)
            playback.setPlayingList(playlist)

            playback.getPlayer().resetPosition()
            if(playback.getPlayerState().type == States.PLAYING) {
                playCurrentPlayingItem(playlist)
            }
            updateState(playback.getPlayerState().type)
            true
        } else {
            stop()
            false
        }
    }

    protected fun skipToPreviousIfPossible(){
        val playlist = playback.getPlayingList().value!!
        val position = playback.getPlayer().getCurrentPosition()
        playback.getPlayer().resetPosition()
        if(position > 15000L || playlist.getCurrentPlaying() < 0) { // position are in milliseconds
            if(playback.getPlayerState().type == States.PLAYING) {
                playback.getPlayer().play(0)
            }
        } else {
            playlist.setCurrentPlaying(playlist.getCurrentPlaying() - 1)
            playback.setPlayingList(playlist)
            if(playback.getPlayerState().type == States.PLAYING) {
                playCurrentPlayingItem(playlist)
            }
        }
        updateState(playback.getPlayerState().type)
    }

    protected fun updateState(type: States, position: Long = 0L) {
        val shuffle = playback.getShuffleFromPlaybackState()
        playback.setPlaybackState(PlaybackStateFactory.createState(type, position, shuffle))
        if(type == States.PLAYING) {
            updateProgress()
        }
    }

    open fun stop(){
        playback.getPlayer().stop()
        playback.setPlaybackState(PlaybackStateFactory.createState(States.STOPPED))
        playback.setPlayingList(MusicList())
        playback.setPlayerState(StoppedState(playback))
        Log.v(TAG, "stopped player")
    }

}