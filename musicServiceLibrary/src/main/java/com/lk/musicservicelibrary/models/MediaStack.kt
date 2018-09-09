package com.lk.musicservicelibrary.models

import android.util.Log

/**
 * Erstellt von Lena am 08.05.18.
 * Repräsentiert einen Stack, der die fünf zuletzt gespielten Titel verwaltet
 */
class MediaStack {

    private var stack = arrayOfNulls<MusicMetadata?>(5)
    private var elementsCounter = 0

    fun pushMedia(element: MusicMetadata) {
        if (elementsCounter <= 4) {
            stack[elementsCounter] = element
            elementsCounter++
        } else {
            removeFirstAndPush(element)
        }
        Log.d("MediaStack", toString())
    }

    private fun removeFirstAndPush(element: MusicMetadata){
        for(i in 1..4){
            stack[i - 1] = stack[i]
        }
        stack[stack.size - 1] = element
    }

    fun topMedia(): MusicMetadata? {
        return if(!isEmtpy())
            stack[elementsCounter-1]
        else
            null
    }

    fun popMedia(): MusicMetadata?{
        return if(!isEmtpy()) {
            elementsCounter--
            val item = stack[elementsCounter]
            stack[elementsCounter] = null
            item
        } else {
            null
        }
    }

    private fun isEmtpy(): Boolean = elementsCounter == 0

    fun popAll(){
        stack = arrayOfNulls(5)
        elementsCounter = 0
    }

    override fun toString(): String {
        var text = "Stack: "
        for(element in stack){
            text += element?.title + ", "
        }
        return text
    }

}