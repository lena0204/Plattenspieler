package com.lk.musicservicelibrary.playback

import android.os.Bundle
import android.os.ResultReceiver
import android.util.Log
import androidx.core.os.bundleOf
import com.lk.musicservicelibrary.main.MusicService
import com.lk.musicservicelibrary.models.MusicList
import com.lk.musicservicelibrary.models.MusicMetadata
import com.lk.musicservicelibrary.utils.*

/**
 * Erstellt von Lena am 05/04/2019.
 */
class CommandResolver(private val playbackCallback: PlaybackCallback) {

    private val TAG = "CommandResolver"

    fun resolveCommand(command: String, args: Bundle?, cb: ResultReceiver?){
        Log.i(TAG, "Resolve command: $command")
        when(command){
            Commands.ADD_QUEUE -> addQueueToService(args, QueueType.QUEUE_ORDERED)
            Commands.ADD_ALL -> addAllSongsToPlayingQueue()
        }
    }

    private fun addQueueToService(args: Bundle?, flag: QueueType){
        val mediaList: MusicList
        if(args != null) {
            Log.d(TAG, "addQueueToService: Load musiclist from parcel")
            args.classLoader = this.javaClass.classLoader
            val list = args.getParcelable<MusicList>("L")
            if(list != null) {
                mediaList = list
                playbackCallback.setQueriedMediaList(mediaList)
            }
        }
    }

    private fun addAllSongsToPlayingQueue(){
        val repo = playbackCallback.getDataRepository()
        val firstTitle = repo.getFirstTitleForShuffle()
        if(!firstTitle.isEmpty()) {
            playFirstTitle(firstTitle)

            var musicList = repo.getAllTitles(firstTitle.id)
            musicList = QueueCreator.shuffleQueueFromMediaList(musicList)
            musicList.insertAsFirstItem(firstTitle)
            musicList.setCurrentPlaying(0)
            playbackCallback.setPlayingList(musicList)
        }
    }

    private fun playFirstTitle(firstTitle: MusicMetadata) {
        val musicList = MusicList()
        musicList.addItem(firstTitle)
        playbackCallback.setPlayingList(musicList)
        val extras = bundleOf(MusicService.SHUFFLE_KEY to true)
        playbackCallback.onPlayFromMediaId(firstTitle.id, extras)
    }

}