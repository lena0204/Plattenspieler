package com.lk.musicservicelibrary.models

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
        private var mediaType: Int = 0
) : Iterable<MusicMetadata>, Parcelable {

    override fun iterator(): Iterator<MusicMetadata> = list.iterator()

    constructor(): this(mutableListOf())

    fun size(): Int = list.size

    fun addItem(element: MusicMetadata) = list.add(element)

    fun insertAsFirstItem(element: MusicMetadata) = list.add(0, element)

    fun removeItemAt(i: Int) = list.removeAt(i)

    fun removeAll() = list.clear()

    fun getItemAt(i: Int): MusicMetadata = list[i]

    fun isEmpty(): Boolean = size() == 0

    fun setMediaType(mediaType: Int) { this.mediaType = mediaType }

    fun getMediaType(): Int = mediaType

    fun getQueueItemList(): MutableList<MediaSession.QueueItem>{
        val queueItemList = mutableListOf<MediaSession.QueueItem>()
        var counter = 1L
        for (item in list){
            queueItemList.add(MediaSession.QueueItem(item.getMediaDescription(), counter++))
        }
        return queueItemList
    }

    fun getMediaItemList(): MutableList<MediaBrowser.MediaItem>{
        val mediaItemList = mutableListOf<MediaBrowser.MediaItem>()
        for (item in list){
            mediaItemList.add(MediaBrowser.MediaItem(item.getMediaDescription(), mediaType))
        }
        return mediaItemList
    }

    companion object {
        fun createListFromQueue(list: MutableList<MediaSession.QueueItem>): MusicList{
            val musicList = MusicList()
            for(item in list){
                musicList.addItem(MusicMetadata.createFromMediaDescription(item.description))
            }
            return musicList
        }
    }
}