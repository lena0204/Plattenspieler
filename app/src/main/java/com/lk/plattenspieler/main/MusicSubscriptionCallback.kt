package com.lk.plattenspieler.main

import android.media.browse.MediaBrowser
import com.lk.music_service_library.models.MusicList
import com.lk.music_service_library.models.MusicMetadata
import com.lk.music_service_library.observables.MedialistsObservable

/**
 * Erstellt von Lena am 18.08.18.
 */
class MusicSubscriptionCallback(private val mediaBrowser: MediaBrowser): MediaBrowser.SubscriptionCallback() {

    override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowser.MediaItem>) {
        val medialist = MusicList()
        if(parentId == mediaBrowser.root){
            medialist.addFlag(MediaBrowser.MediaItem.FLAG_BROWSABLE)
            for(mediaItem in children){
                medialist.addItem(MusicMetadata.createFromMediaDescription(mediaItem.description))
            }
            MedialistsObservable.setAlbumList(medialist)
        } else if(parentId.contains("ALBUM-")){
            medialist.addFlag(MediaBrowser.MediaItem.FLAG_PLAYABLE)
            for(mediaItem in children){
                medialist.addItem(MusicMetadata.createFromMediaDescription(mediaItem.description))
            }
            MedialistsObservable.setTitleList(medialist)
        }
        mediaBrowser.unsubscribe(parentId)
    }
}