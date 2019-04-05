package com.lk.musicservicelibrary.main

import android.media.session.PlaybackState
import android.util.Log
import androidx.core.os.bundleOf
import com.lk.musicservicelibrary.models.*
import com.lk.musicservicelibrary.system.MusicDataRepository
import com.lk.musicservicelibrary.utils.*
import kotlinx.coroutines.*

/**
 * Erstellt von Lena am 02.09.18.
 */
internal class MetadataRepository(private val dataRepository: MusicDataRepository) {

    private val TAG = MetadataRepository::class.java.simpleName
    private val listeners = DelegatingFunctions<PlaybackData>()

    private var playbackData = PlaybackData()
    private var stack = MediaStack()
    private var shuffleOn = false

    fun getCurrentMusicId() = playbackData.metadata.id
    fun getCurrentMusicPath() = playbackData.metadata.path

    fun createInitialData(): PlaybackData {
        val builder = PlaybackState.Builder()
        builder.setActions(PlaybackState.ACTION_PLAY_FROM_MEDIA_ID
                    or PlaybackState.ACTION_PLAY)
        playbackData.playbackState = builder.build()
        return playbackData
    }

    fun addQueueCreatedListener(listener: listenerFunction<PlaybackData>){
        listeners += listener
    }

    fun getFirstItemForShuffleAll(): String = dataRepository.getFirstTitleIDForShuffle()

    fun updatePrepareFromId(id: String, _shuffleOn: Boolean = false): PlaybackData{
        updateMetadata(id)
        shuffleOn = _shuffleOn
        return playbackData
    }

    fun updatePlayFromId(id: String, _shuffleOn: Boolean = false): PlaybackData{
        updateMetadata(id)
        shuffleOn = _shuffleOn
        stack.popAll()
        playbackData.queue.removeAll()
        return playbackData
    }

    fun updatePlay(position: Long): PlaybackData {
        statePlay(position)
        return playbackData
    }

    fun updatePause(position: Long): PlaybackData {
        statePause(position)
        return playbackData
    }

    fun updateNext(): PlaybackData {
        if(!playbackData.queue.isEmpty()){
            stack.pushMedia(playbackData.metadata)
            val id = playbackData.queue.getItemAt(0).id
            playbackData.queue.removeItemAt(0)
            updateMetadata(id)
        } else {
            updateStop()
        }
        return playbackData
    }

    fun updatePrevious(position: Long): PlaybackData {
        val metadata = stack.topMedia()
        if(metadata != null && position <= 15000){
            playbackData.queue.insertAsFirstItem(playbackData.metadata)
            stack.popMedia()
            val id = metadata.id
            updateMetadata(id)
        }
        return playbackData
    }

    fun updateStop(): PlaybackData {
        shuffleOn = false
        stack.popAll()
        playbackData.queue.removeAll()
        playbackData.metadata = MusicMetadata()
        stateStop()
        return playbackData
    }

    private fun updateMetadata(id: String){
        var number = 0L
        if(!playbackData.queue.isEmpty()){
            number = playbackData.queue.size().toLong()
        }
        playbackData.metadata = dataRepository.getTitleByID(id)
        playbackData.metadata.nr_of_songs_left = number
        if(playbackData.metadata.isEmpty()){
            Log.e(TAG, "Metadaten sind null")
        }
    }

    private fun statePlay(position: Long){
        val builder = PlaybackState.Builder()
        builder.setActions(PlaybackState.ACTION_PAUSE
                or PlaybackState.ACTION_PLAY_FROM_MEDIA_ID
                or PlaybackState.ACTION_STOP
                or PlaybackState.ACTION_SKIP_TO_NEXT
                or PlaybackState.ACTION_SKIP_TO_PREVIOUS)
        builder.setState(PlaybackState.STATE_PLAYING, position, 1.0f)
        setShuffleAndBuild(builder)

    }

    private fun setShuffleAndBuild(builder: PlaybackState.Builder){
        val extras = bundleOf(MusicService.SHUFFLE_KEY to shuffleOn)
        builder.setExtras(extras)
        playbackData.playbackState = builder.build()
    }

    private fun statePause(position: Long){
        val builder = PlaybackState.Builder()
        builder.setActions(PlaybackState.ACTION_PLAY
                or PlaybackState.ACTION_PLAY_FROM_MEDIA_ID
                or PlaybackState.ACTION_SKIP_TO_NEXT
                or PlaybackState.ACTION_SKIP_TO_PREVIOUS
                or PlaybackState.ACTION_STOP)
        builder.setState(PlaybackState.STATE_PAUSED, position, 1.0f)
        setShuffleAndBuild(builder)
    }

    private fun stateStop(){
        val builder = PlaybackState.Builder()
        builder.setActions(PlaybackState.ACTION_PLAY_FROM_MEDIA_ID)
        builder.setState(PlaybackState.STATE_STOPPED, 0L, 1.0f)
        setShuffleAndBuild(builder)
    }

    fun createQueueFromMediaList(mediaList: MusicList, titleId: String, queueType: QueueType) {
        GlobalScope.launch (Dispatchers.IO) {
            var queueDetailed = MusicList()
            if(queueType == QueueType.QUEUE_ALL_SHUFFLE){
                queueDetailed = dataRepository.getAllTitles(titleId)
            } else if(queueType == QueueType.QUEUE_RESTORED){
                queueDetailed = mediaList
            } else {
                val queue = if(queueType == QueueType.QUEUE_ORDERED){
                    QueueCreation.createQueueFromTitle(mediaList, titleId)
                } else {
                    QueueCreation.createRandomQueue(mediaList, titleId)
                }
                for(queueItem in queue){
                    queueDetailed.addItem(dataRepository.getTitleByID(queueItem.id))
                }
            }
            playbackData.queue = queueDetailed
            Log.v(TAG, "newQueue with ${queueDetailed.size()} items.")
            updateMetadata(titleId)
            listeners.callWithParameter(playbackData)
        }
    }
}