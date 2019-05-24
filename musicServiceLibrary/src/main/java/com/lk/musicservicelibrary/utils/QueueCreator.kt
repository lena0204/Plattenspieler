package com.lk.musicservicelibrary.utils

import com.lk.musicservicelibrary.models.MusicList
import java.util.*

/**
 * Erstellt von Lena am 12.05.18.
 * Verwaltet die Erstellung von zufälligen Wiedergabelisten
 */
object QueueCreator {

    private var currentMediaList = MusicList()

    fun shuffleQueueFromMediaList(mediaList: MusicList): MusicList {
        copyMediaList(mediaList)
        return shuffleMediaListToQueue()

    }

    private fun copyMediaList(mediaList: MusicList){
        currentMediaList = MusicList()
        for(item in mediaList){
            currentMediaList.addItem(item)
        }
    }

    private fun shuffleMediaListToQueue(): MusicList {
        val random = Random()
        val queue = MusicList()
        var randomValue: Int
        while(!currentMediaList.isEmpty()){
            randomValue = random.nextInt(currentMediaList.size())
            val element = currentMediaList.getItemAt(randomValue)
            queue.addItem(element)
            currentMediaList.removeItemAt(randomValue)
        }
        return queue
    }
}