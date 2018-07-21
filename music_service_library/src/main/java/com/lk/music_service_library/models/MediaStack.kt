package com.lk.music_service_library.models

import android.util.Log

/**
 * Erstellt von Lena am 08.05.18.
 * Repräsentiert einen Stack, der die fünf zuletzt gespielten Titel verwaltet
 */
class MediaStack {

    // Repräsentiert einen Stack der die fünf zuletzt gespielten Titel verwaltet
    private var stack = arrayOfNulls<MusicMetadata?>(5)
    private var elements = 0

    /**
     * Fügt ein neues Element dem Stack hinzu, falls [element] nicht null ist
     * Falls dieser voll ist, wird das älteste Element gelöscht
     * @param element Metadaten des letzten Musiktitels [MusicMetadata]
     */
    fun pushMedia(element: MusicMetadata?){
        if(element != null) {
            if (elements <= 4) {
                stack[elements] = element
                elements++
            } else {
                // Erstes löschen und alle anderen nach vorne schieben
                var i = 1
                while (i < 5) {
                    stack[i - 1] = stack[i]
                    i++
                }
                stack[i-1] = element
            }
        }
        Log.d("MediaStack", toString())
    }

    /**
     * @return letzte Metadaten [MusicMetadata]
     */
    fun topMedia(): MusicMetadata?{
        return if(!isEmtpy())
            stack[elements-1]
        else
            null
    }

    /**
     * Entfernt die obersten Metadaten und gibt sie zurück
     * @return letzte Metadaten [MusicMetadata]
     */
    fun popMedia(): MusicMetadata?{
        return if(!isEmtpy()) {
            elements--
            val item = stack[elements]
            stack[elements] = null
            item
        } else {
            null
        }
    }

    /**
     * @return sind Elemente vorhanden
     */
    private fun isEmtpy(): Boolean = elements == 0

    /**
     * Entfernt alle noch vorhandenen Metadaten
     */
    fun popAll(){
        stack = arrayOfNulls(5)
        elements = 0
    }

    override fun toString(): String {
        var text = "Stack: "
        for(element in stack){
            text += element?.title + ", "
        }
        return text
    }

}