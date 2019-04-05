package com.lk.plattenspieler.observables

import android.app.Application
import android.media.session.PlaybackState
import android.util.Log
import androidx.core.content.edit
import androidx.core.os.bundleOf
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import com.lk.musicservicelibrary.database.SongDBAccess
import com.lk.musicservicelibrary.main.MusicService
import com.lk.musicservicelibrary.models.MusicList
import com.lk.musicservicelibrary.models.MusicMetadata
import com.lk.plattenspieler.main.MainActivityNew
import com.lk.plattenspieler.musicbrowser.ControllerAction
import com.lk.plattenspieler.musicbrowser.EnumActions

/**
 * Erstellt von Lena am 09.09.18.
 */
class PlaybackViewModel(application: Application): AndroidViewModel(application) {

    var metadata = MutableLiveData<MusicMetadata>()
    var playbackState = MutableLiveData<PlaybackState>()
    var queue = MutableLiveData<MusicList>()
    var controllerAction = MutableLiveData<ControllerAction>()

    val app = getApplication<Application>()

    private val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(application)

    init {
        metadata.value = MusicMetadata()
        playbackState.value = PlaybackState.Builder().build()
        queue.value = MusicList()
    }

    fun setObserverToAll(owner: LifecycleOwner, observer: Observer<Any>){
        metadata.observe(owner, observer)
        playbackState.observe(owner, observer)
        queue.observe(owner, observer)
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

    fun restoreSavedState(controllerState: Int?){
        val wasQueueSaved = sharedPreferences.getBoolean(MainActivityNew.PREF_PLAYING, false)
        Log.d("PlaybackViewModel", "restoreSavedState: $wasQueueSaved")
        if(controllerState != PlaybackState.STATE_PLAYING && controllerState != PlaybackState.STATE_PAUSED){
            if(wasQueueSaved)
                restoreQueue()
        } else {
            val action = ControllerAction(EnumActions.PLAYS)
            controllerAction.postValue(action)
        }
    }

    private fun restoreQueue(){
        SongDBAccess.setContentResolver(app.contentResolver)
        val music = SongDBAccess.restoreFirstItem()
        if(music == null){
            sharedPreferences.edit { putBoolean(MainActivityNew.PREF_PLAYING, false) }
        } else {
            sendFirstItemToController(music.id)
            sendQueueIfAvailable()
        }
    }

    private fun sendFirstItemToController(titleId: String){
        val args = bundleOf("I" to 1,
                MusicService.SHUFFLE_KEY to getShufflePreference())
        controllerAction.postValue(ControllerAction(EnumActions.PREPARE_FROM_ID, titleId, args = args))
    }

    private fun sendQueueIfAvailable(){
        SongDBAccess.setContentResolver(app.contentResolver)
        val queueRestored = SongDBAccess.restorePlayingQueue()
        if(queueRestored != null){
            Log.v("ViewModel", "sendQueue")
            val args = bundleOf("L" to queueRestored)
            controllerAction.postValue(ControllerAction(EnumActions.QUEUE_RESTORED, args = args))
        }
    }

    fun saveQueue(){
        if(!queue.value!!.isEmpty()) {
            Log.d("ViewModel", "saveQueue")
            SongDBAccess.setContentResolver(app.contentResolver)
            SongDBAccess.savePlayingQueue(queue.value!!, metadata.value!!)
        }
    }

    private fun getShufflePreference(): Boolean
            = sharedPreferences.getBoolean(MainActivityNew.PREF_SHUFFLE,false)

    fun getShuffleFromPlaybackState(): Boolean =
            playbackState.value?.extras?.getBoolean(MusicService.SHUFFLE_KEY) ?: false
}