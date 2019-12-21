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
            Commands.ADD_ALL -> addAllSongsToPlayingQueue()
            Commands.ADD_QUEUE -> addQueue(args)
            Commands.RESTORE_QUEUE -> restore()
        }
    }

    private fun restore() {
        val list = playbackCallback.getPlaylistRepository().restorePlayingQueue()
        if(!list.isEmpty()) {
            Log.d(TAG, "Has items true: $list")
            if(!list.isEmpty()) {
                Log.d(TAG, "Restored playlist from DB with ${list.size()} items: $list")
                playbackCallback.setQueriedMediaList(list)
                playbackCallback.preparePlayback()
            }
        } else {
            Log.d(TAG, "Keine Items in der DB.")
        }
    }

    private fun addQueue(args: Bundle?) {
        if(args != null) {
            args.classLoader = this.javaClass.classLoader
            val newQueue: MusicList = args.getParcelable(MusicService.QUEUE_KEY) ?: MusicList()
            playbackCallback.setQueriedMediaList(newQueue)
            playbackCallback.setPlayingList(newQueue)
        }
    }

    private fun addAllSongsToPlayingQueue(){
        val repo = playbackCallback.getDataRepository()
        val firstTitle = repo.queryFirstTitle()
        if(!firstTitle.isEmpty()) {
            playFirstTitle(firstTitle)

            var musicList = repo.queryTitles(firstTitle.id)
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
        playbackCallback.onPlayFromMediaId(firstTitle.id, null)
    }

}