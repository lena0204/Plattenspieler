package com.lk.musicservicelibrary.playback

import android.os.Bundle
import android.util.Log
import com.lk.musicservicelibrary.models.MusicList
import com.lk.musicservicelibrary.models.MusicMetadata
import com.lk.musicservicelibrary.utils.PlaybackStateCreator
import com.lk.musicservicelibrary.utils.QueueCreator

/**
 * Erstellt von Lena am 05/04/2019.
 */
abstract class PlayerState(private var playback: PlaybackCallback) {

    private val TAG = "PlayerState"

    abstract fun playFromId(mediaId: String?, extras: Bundle?)
    abstract fun play()
    abstract fun pause()
    abstract fun stop()
    abstract fun skipToNext()
    abstract fun skipToPrevious()

    protected fun createPlayingList(mediaId: String?, shuffle: Boolean): MusicList {
        var playingList = playback.getQueriedMediaList()
        Log.v(TAG, "MediaID is $mediaId, queried list has ${playingList.count()} items")
        if(shuffle){
            playingList = QueueCreator.shuffleQueueFromMediaList(playingList)
            playingList.setCurrentPlaying(0)
        } else {
            val position = playingList.indexOfFirst { data -> data.id == mediaId }
            playingList.setCurrentPlaying(position)
        }
        Log.d(TAG, "created Playlist with ${playingList.count()} items and currentplaying: ${playingList.getItemAtCurrentPlaying()}")
        return playingList
    }

    protected fun playCurrentPlayingItem(playingList: MusicList) {
        val firstMetadata = playingList.getItemAtCurrentPlaying()
        if(firstMetadata != null && firstMetadata.path != ""){
            playback.getPlayer().preparePlayer(firstMetadata.path)
        } else {
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
                PlaybackStateCreator.createStateForPlaying(0L, shuffle))
            true
        } else {
            stop()
            false
        }
    }

    protected fun skipToPreviousIfPossible(){
        val playlist = playback.getPlayingList().value!!
        val position = playback.getPlayer().getCurrentPosition()
        if(position > 15L) {
            playback.getPlayer().play(0)
        } else {
            if(playlist.getCurrentPlaying() < 0){
                playback.getPlayer().play(0)
            } else {
                playlist.setCurrentPlaying(playlist.getCurrentPlaying() - 1)
                playCurrentPlayingItem(playlist)
                playback.setPlayingList(playlist)
            }
        }
        val shuffle = playback.getShuffleFromPlaybackState()
        playback.setPlaybackState(
            PlaybackStateCreator.createStateForPlaying(0L, shuffle)
        )
    }

    protected fun stopPlayback(){
        playback.getPlayer().stop()
        playback.setPlaybackState(PlaybackStateCreator.createStateForStopped())
        playback.setPlayingList(MusicList())
        playback.setPlayerState(StoppedState(playback))
        Log.v(TAG, "stopped player")
    }

}