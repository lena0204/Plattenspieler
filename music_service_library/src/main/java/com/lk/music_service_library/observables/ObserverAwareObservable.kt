package com.lk.music_service_library.observables

import android.util.Log
import java.util.*

/**
 * Erstellt von Lena am 15.08.18.
 */
open class ObserverAwareObservable: Observable() {

    private var observers = mutableListOf<Observer>()

    override fun addObserver(o: Observer?) {
        super.addObserver(o)
        if(o != null)
            observers.add(o)
    }

    override fun deleteObserver(o: Observer?) {
        super.deleteObserver(o)
        observers.remove(o)
    }

    fun printObserver(){
        var ausgabe = "Observer: "
        for(o in observers){
            ausgabe += o.javaClass.canonicalName + ", "
        }
        Log.i("Observable", ausgabe)
    }

    fun addObserverIfNotConnected(o: Observer){
        if(!observers.contains(o))
            addObserver(o)
    }

}