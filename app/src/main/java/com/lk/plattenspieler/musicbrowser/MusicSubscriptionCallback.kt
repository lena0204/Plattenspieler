package com.lk.plattenspieler.musicbrowser

import android.media.browse.MediaBrowser
import com.lk.musicservicelibrary.models.MusicList
import com.lk.musicservicelibrary.models.MusicMetadata
import com.lk.plattenspieler.observables.MediaViewModel

/**
 * Erstellt von Lena am 18.08.18.
 */
class MusicSubscriptionCallback(
        private val mediaBrowser: MediaBrowser,
        private val mediaData: MediaViewModel): MediaBrowser.SubscriptionCallback() {

    override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowser.MediaItem>) {
        val medialist = MusicList()
        if(parentId == mediaBrowser.root){
            medialist.setMediaType(MediaBrowser.MediaItem.FLAG_BROWSABLE)
            for(mediaItem in children){
                medialist.addItem(MusicMetadata.createFromMediaDescription(mediaItem.description))
            }
            mediaData.albumList.value = medialist
        } else if(parentId.contains("ALBUM-")){
            medialist.setMediaType(MediaBrowser.MediaItem.FLAG_PLAYABLE)
            for(mediaItem in children){
                medialist.addItem(MusicMetadata.createFromMediaDescription(mediaItem.description))
            }
            mediaData.titleList.value = medialist
        }
        mediaBrowser.unsubscribe(parentId)
    }
}