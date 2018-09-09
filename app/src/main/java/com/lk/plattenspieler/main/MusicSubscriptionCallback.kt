package com.lk.plattenspieler.main

import android.media.browse.MediaBrowser
import com.lk.plattenspieler.observables.MedialistsObservable
import com.lk.musicservicelibrary.models.MusicList
import com.lk.musicservicelibrary.models.MusicMetadata

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