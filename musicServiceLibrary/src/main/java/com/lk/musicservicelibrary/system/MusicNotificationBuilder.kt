package com.lk.musicservicelibrary.system

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import com.lk.musicservicelibrary.R
import com.lk.musicservicelibrary.main.MusicService
import com.lk.musicservicelibrary.models.MusicMetadata
import com.lk.musicservicelibrary.utils.CoverLoader

/**
 * Erstellt von Lena am 02.09.18.
 */
class MusicNotificationBuilder(private val service: MusicService) {

    private val CHANNEL_ID = "plattenspieler_playback"
    private val TAG = "MusicNotification"
    private val ACTION = "com.lk.plattenspieler.ACTION_LAUNCH_PL"

    private lateinit var notificationBuilder: Notification.Builder

    fun showNotification(state: Int, currentMusicMetadata: MusicMetadata, shuffleOn: Boolean): Notification {
        initializeBuilder()
        setNotificationContent(currentMusicMetadata)
        setMediaStyle()
        configureShuffleAction(shuffleOn)
        setClickActions(state)
        return notificationBuilder.build()
    }

    private fun initializeBuilder(){
        notificationBuilder = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
            Notification.Builder(service, CHANNEL_ID)
        } else {
            Notification.Builder(service)
        }
    }

    private fun createChannel() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Music playback", NotificationManager.IMPORTANCE_LOW)
            channel.description = "Music playback controls"
            channel.setShowBadge(false)
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            val manager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun setNotificationContent(currentMusicMetadata: MusicMetadata){
        notificationBuilder.setContentTitle(currentMusicMetadata.title)
        notificationBuilder.setContentText(currentMusicMetadata.artist)
        var songsLeft = currentMusicMetadata.nr_of_songs_left.toString()
        songsLeft += " " + service.resources.getString(R.string.notification_songs_left)
        notificationBuilder.setSubText(songsLeft)
        notificationBuilder.setSmallIcon(R.drawable.ic_stat_musicnotification)
        if(currentMusicMetadata.cover_uri != "" || currentMusicMetadata.content_uri != Uri.EMPTY){
            val cover = CoverLoader.decodeAlbumCover(service.applicationContext,
            currentMusicMetadata.content_uri, currentMusicMetadata.cover_uri)
            notificationBuilder.setLargeIcon(cover)
        }

    }

    private fun setMediaStyle(){
        notificationBuilder.style = Notification.MediaStyle()
                .setMediaSession(service.sessionToken)
                .setShowActionsInCompactView(1,2)
    }

    private fun configureShuffleAction(shuffleOn: Boolean){
        if(shuffleOn){
            notificationBuilder.addAction(Notification.Action.Builder(R.mipmap.ic_shuffle, "Shuffle", null).build())
        } else {
            notificationBuilder.addAction(Notification.Action.Builder(R.color.transparent, "Shuffle", null).build())
        }
    }

    private fun setClickActions(state: Int){
        setContentIntent()
        setPlayAction(state)
        setNextAction()
    }

    private fun setContentIntent(){
        notificationBuilder.setContentIntent(PendingIntent.getActivity(service.applicationContext, 10,
                Intent(ACTION), PendingIntent.FLAG_UPDATE_CURRENT))
    }

    private fun setNextAction(){
        val pi = PendingIntent.getBroadcast(service, 102,
                Intent(MusicService.ACTION_MEDIA_NEXT).setPackage(service.packageName), 0)
        notificationBuilder.addAction(Notification.Action.Builder(R.mipmap.ic_next, "Next", pi).build())
    }

    private fun setPlayAction(state: Int){
        if(state == PlaybackState.STATE_PLAYING){
            val pi = PendingIntent.getBroadcast(service, 100,
                    Intent(MusicService.ACTION_MEDIA_PAUSE).setPackage(service.packageName), 0)
            notificationBuilder.addAction(Notification.Action.Builder(R.mipmap.ic_pause, "Pause", pi).build())
        } else {
            val pi = PendingIntent.getBroadcast(service, 101,
                    Intent(MusicService.ACTION_MEDIA_PLAY).setPackage(service.packageName), 0)
            notificationBuilder.addAction(Notification.Action.Builder(R.mipmap.ic_play, "Play", pi).build())
        }
    }
}