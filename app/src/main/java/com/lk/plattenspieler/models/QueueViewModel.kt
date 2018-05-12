package com.lk.plattenspieler.models

import android.app.Application
import android.arch.lifecycle.*
import android.util.Log

/**
 * Erstellt von Lena am 11.05.18.
 */
class QueueViewModel(application: Application): AndroidViewModel(application) {

    private var queue: MutableLiveData<MusicList> = MutableLiveData()

    fun getQueue(): LiveData<MusicList>{
        Log.d("QueueViewModel", "Return: Länge: " + queue.value?.countItems() + ", App:" + getApplication())
        return queue
    }
    fun setQueue(newQueue: MusicList){
        Log.d("QueueViewModel", "set: Länge: " + newQueue.countItems())
        queue.value = newQueue
    }
}