package com.lk.plattenspieler.observables

import android.app.Application
import androidx.lifecycle.*
import com.lk.musicservicelibrary.models.MusicList

/**
 * Erstellt von Lena am 09.09.18.
 */
class MediaViewModel(application: Application): AndroidViewModel(application) {

    var albumlist = MutableLiveData<MusicList>()
    var titleList = MutableLiveData<MusicList>()

    fun setObserversToAll(owner: LifecycleOwner, observer: Observer<MusicList>){
        albumlist.observe(owner, observer)
        titleList.observe(owner, observer)
    }

}