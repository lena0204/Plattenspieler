package com.lk.plattenspieler.observables

import android.app.Application
import android.media.session.PlaybackState
import android.util.Log
import androidx.lifecycle.*
import com.lk.musicservicelibrary.database.*
import com.lk.musicservicelibrary.database.room.PlaylistRoomRepository
import com.lk.musicservicelibrary.models.MusicList
import com.lk.musicservicelibrary.models.MusicMetadata
import com.lk.musicservicelibrary.utils.SharedPrefsWrapper
import com.lk.plattenspieler.musicbrowser.ControllerAction

/**
 * Erstellt von Lena am 09.09.18.
 */
class PlaybackViewModel(application: Application): AndroidViewModel(application) {

    private val TAG = "PlaybackViewModel"

    private var metadata = MutableLiveData<MusicMetadata>()
    private var playbackState = MutableLiveData<PlaybackState>()
    private var queue = MutableLiveData<MusicList>()
    private var controllerAction = MutableLiveData<ControllerAction>()

    private var playlistRepo: PlaylistRepository

    init {
        metadata.value = MusicMetadata()
        playbackState.value = PlaybackState.Builder().build()
        queue.value = MusicList()
        playlistRepo = PlaylistRoomRepository(application)
    }

    fun setObserverToAll(owner: LifecycleOwner, observer: Observer<Any>){
        metadata.observe(owner, observer)
        playbackState.observe(owner, observer)
        queue.observe(owner, observer)
    }

    fun setObserverToPlaybackState(owner: LifecycleOwner, observer: Observer<Any>){
        playbackState.observe(owner, observer)
    }

    fun setObserverToAction(owner: LifecycleOwner, observer: Observer<Any>){
        controllerAction.observe(owner, observer)
    }

    fun callAction(action: ControllerAction) {
        controllerAction.value = action
    }

    // TODO über Commands lösen anstatt von direktem Zugriff
    fun saveQueue(){
        if(!queue.value!!.isEmpty()) {
            Log.d(TAG, "saveQueue")
            playlistRepo.savePlayingQueue(queue.value!!, metadata.value!!)
        } else {
            Log.d(TAG, "just delete playlist")
            playlistRepo.deletePlaylist()
        }
    }

    private fun getShufflePreference(): Boolean
            = SharedPrefsWrapper.readShuffle(this.getApplication())

    fun getMetadata(): MusicMetadata {
        return metadata.value ?: MusicMetadata()
    }
    fun setMetadata(data: MusicMetadata) {
        metadata.value = data
    }

    fun getPlaybackState(): PlaybackState {
        return playbackState.value ?: PlaybackState.Builder().build()
    }
    fun setPlaybackState(data: PlaybackState) {
        playbackState.value = data
    }

    fun setQueue(data: MusicList) {
        queue.value = data
    }

    fun getQueueLimitedTo30(): MusicList{
        var limitedQueue = MusicList()
        if(queue.value!!.size() > 30) {
            for(i in 0..29){
                limitedQueue.addItem(queue.value!!.getItemAt(i))
            }
        } else {
            limitedQueue = queue.value as MusicList
        }
        return limitedQueue
    }
}