package com.lk.music_service_library.utils

import com.lk.music_service_library.models.MusicList
import java.util.*

/**
 * Erstellt von Lena am 12.05.18.
 * Verwaltet die Erstellung von Wiedergabelisten (sowohl sortiert als auch zufällig)
 */
object QueueCreation {

    fun createQueueFromTitle(titleid: String, medialist: MusicList): MusicList{
        var indexInMedialist = -1
        for(item in medialist){
            if(item.id == titleid){
                indexInMedialist = medialist.indexOf(item)
                break
            }
        }
        val queue = MusicList()
        if(indexInMedialist != -1){
            var i = indexInMedialist + 1
            while(i < medialist.countItems()){
                queue.addItem(medialist.getItemAt(i))
                i++
            }
        }
        return queue
    }

    fun createQueueRandom(medialist: MusicList, titleid: String): MusicList {
        val listSongs = MusicList()
        val queue = MusicList()
        for(item in medialist){
            listSongs.addItem(item)
        }
        val random = Random()
        var i: Int
        // zufällige Liste erstellen und an die QUEUE hängen, ersten Titel aus der Queue abspielen
        while(!listSongs.isEmpty()){
            i = random.nextInt(listSongs.countItems())
            val element = listSongs.removeItemAt(i)
            if(element.id != titleid) queue.addItem(element)
        }
        return queue
    }

}