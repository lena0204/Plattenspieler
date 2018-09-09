package com.lk.music_service_library.models

import android.media.browse.MediaBrowser
import android.media.session.MediaSession
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Erstellt von Lena am 10.05.18.
 * Repräsentiert eine Liste von [MusicMetadata] mit entsprechenden Aktionen und Konvertierungsmöglichkeiten
 */
@Parcelize
class MusicList(
        private var list: MutableList<MusicMetadata>,
        private var flag: Int = 0
) : Iterable<MusicMetadata>, Parcelable {

    override fun iterator(): Iterator<MusicMetadata> = list.iterator()

    constructor(): this(mutableListOf())

    fun countItems(): Int = list.size

    fun addItem(element: MusicMetadata) = list.add(element)

    fun addFirstItem(element: MusicMetadata) = list.add(0, element)

    fun removeItemAt(i: Int) = list.removeAt(i)

    fun removeAll() = list.clear()

    fun getItemAt(i: Int): MusicMetadata = list[i]

    fun isEmpty(): Boolean = countItems() == 0

    fun addFlag(flag: Int) { this.flag = flag }

    fun getFlag(): Int = flag

    fun getQueueItemList(): MutableList<MediaSession.QueueItem>{
        val qlist = mutableListOf<MediaSession.QueueItem>()
        var counter = 1L
        for (item in list){
            qlist.add(MediaSession.QueueItem(item.getMediaDescription(), counter++))
        }
        return qlist
    }

    fun getMediaItemList(): MutableList<MediaBrowser.MediaItem>{
        val mlist = mutableListOf<MediaBrowser.MediaItem>()
        for (item in list){
            mlist.add(MediaBrowser.MediaItem(item.getMediaDescription(), flag))
        }
        return mlist
    }

    companion object {
        fun createListFromQueue(list: MutableList<MediaSession.QueueItem>): MusicList{
            val mlist = MusicList()
            for(item in list){
                mlist.addItem(MusicMetadata.createFromMediaDescription(item.description))
            }
            return mlist
        }
    }
}