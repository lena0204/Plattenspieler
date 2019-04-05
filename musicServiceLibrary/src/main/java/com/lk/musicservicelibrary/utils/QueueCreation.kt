package com.lk.musicservicelibrary.utils

import com.lk.musicservicelibrary.models.MusicList
import java.util.*

/**
 * Erstellt von Lena am 12.05.18.
 * Verwaltet die Erstellung von Wiedergabelisten (sowohl sortiert als auch zufällig)
 */
object QueueCreation {

    private var currentMedialist = MusicList()

    fun createQueueFromTitle(musicList: MusicList, titleId: String): MusicList {
        currentMedialist = musicList
        val indexInMedialist = searchTitleInMediaList(titleId)
        return addFollowingTitlesToQueue(indexInMedialist)
    }

    private fun searchTitleInMediaList(titleId: String): Int {
        for(item in currentMedialist){
            if(item.id == titleId){
                return currentMedialist.indexOf(item)
            }
        }
        return -1
    }

    private fun addFollowingTitlesToQueue(titleIndex: Int): MusicList {
        val queue = MusicList()
        if(titleIndex != -1){
            var i = titleIndex + 1
            while(i < currentMedialist.size()){
                queue.addItem(currentMedialist.getItemAt(i))
                i++
            }
        }
        return queue
    }

    fun createRandomQueue(mediaList: MusicList, titleId: String): MusicList {
        copyMediaList(mediaList)
        return shuffleMediaListToQueue(titleId)

    }

    private fun copyMediaList(mediaList: MusicList){
        currentMedialist = MusicList()
        for(item in mediaList){
            currentMedialist.addItem(item)
        }
    }

    private fun shuffleMediaListToQueue(titleId: String): MusicList {
        val random = Random()
        val queue = MusicList()
        var randomValue: Int
        while(!currentMedialist.isEmpty()){
            randomValue = random.nextInt(currentMedialist.size())
            val element = currentMedialist.removeItemAt(randomValue)
            if(element.id != titleId)
                queue.addItem(element)
        }
        return queue
    }
}