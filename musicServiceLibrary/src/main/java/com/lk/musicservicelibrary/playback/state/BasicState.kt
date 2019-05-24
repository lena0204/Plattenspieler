package com.lk.musicservicelibrary.playback.state

import android.os.Bundle
import android.util.Log
import com.lk.musicservicelibrary.main.MusicService
import com.lk.musicservicelibrary.models.MusicList
import com.lk.musicservicelibrary.playback.*
import com.lk.musicservicelibrary.utils.PlaybackStateBuilder
import com.lk.musicservicelibrary.utils.QueueCreator

/**
 * Erstellt von Lena am 05/04/2019.
 */
abstract class BasicState(private var playback: PlaybackCallback) {

    private val TAG = "PlayerState"

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
            PlaybackStateBuilder.createStateForPlaying(0L, shuffle)
        )
        playback.setPlayerState(PlayingState(playback))
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

    protected fun skipToNextOrStop(): Boolean {
        val playlist = playback.getPlayingList().value!!
        return if(playlist.getCurrentPlaying() < playlist.size()) {
            playlist.setCurrentPlaying(playlist.getCurrentPlaying() + 1)
            playCurrentPlayingItem(playlist)
            playback.setPlayingList(playlist)
            val shuffle = playback.getShuffleFromPlaybackState()
            playback.setPlaybackState(
                PlaybackStateBuilder.createStateForPlaying(0L, shuffle))
            true
        } else {
            stop()
            false
        }
    }

    protected fun skipToPreviousIfPossible(){
        val playlist = playback.getPlayingList().value!!
        val position = playback.getPlayer().getCurrentPosition()
        Log.v(TAG, "Current position $position")
        if(position > 15000L) { // position are in milliseconds
            playback.getPlayer().play(0)
            Log.v(TAG, "restart current item")
        } else {
            if(playlist.getCurrentPlaying() < 0){
                playback.getPlayer().play(0)
                Log.v(TAG, "restart current item as there's no previous")
            } else {
                playlist.setCurrentPlaying(playlist.getCurrentPlaying() - 1)
                playCurrentPlayingItem(playlist)
                playback.setPlayingList(playlist)
                Log.v(TAG, "start previous item")
            }
        }
        val shuffle = playback.getShuffleFromPlaybackState()
        playback.setPlaybackState(
            PlaybackStateBuilder.createStateForPlaying(0L, shuffle)
        )
    }

    open fun stop(){
        playback.getPlayer().stop()
        playback.setPlaybackState(PlaybackStateBuilder.createStateForStopped())
        playback.setPlayingList(MusicList())
        playback.setPlayerState(StoppedState(playback))
        Log.v(TAG, "stopped player")
    }

}