package com.lk.music_service_library.utils

import com.lk.music_service_library.models.MusicList
import java.util.*

/**
 * Erstellt von Lena am 12.05.18.
 * Verwaltet die Erstellung von Wiedergabelisten (sowohl sortiert als auch zufällig)
 */
object QueueCreation {

    private var currentMedialist = MusicList()

    fun createQueueFromTitle(titleid: String, medialist: MusicList): MusicList {
        currentMedialist = medialist
        val indexInMedialist = searchTitleInMedialist(titleid)
        return addFollowingTitlesToQueue(indexInMedialist)
    }

    private fun searchTitleInMedialist(titleid: String): Int {
        for(item in currentMedialist){
            if(item.id == titleid){
                return currentMedialist.indexOf(item)
            }
        }
        return -1
    }

    private fun addFollowingTitlesToQueue(titleindex: Int): MusicList {
        val queue = MusicList()
        if(titleindex != -1){
            var i = titleindex + 1
            while(i < currentMedialist.countItems()){
                queue.addItem(currentMedialist.getItemAt(i))
                i++
            }
        }
        return queue
    }

    fun createRandomQueue(medialist: MusicList, titleid: String): MusicList {
        copyMediaList(medialist)
        return shuffleMediaListToQueue(titleid)

    }

    private fun copyMediaList(medialist: MusicList){
        currentMedialist = MusicList()
        for(item in medialist){
            currentMedialist.addItem(item)
        }
    }

    private fun shuffleMediaListToQueue(titleid: String): MusicList {
        val random = Random()
        val queue = MusicList()
        var randomValue: Int
        while(!currentMedialist.isEmpty()){
            randomValue = random.nextInt(currentMedialist.countItems())
            val element = currentMedialist.removeItemAt(randomValue)
            if(element.id != titleid)
                queue.addItem(element)
        }
        return queue
    }

}