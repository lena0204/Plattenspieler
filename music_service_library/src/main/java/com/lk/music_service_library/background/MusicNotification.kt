package com.lk.music_service_library.background

import android.app.*
import android.content.*
import android.graphics.BitmapFactory
import android.media.session.PlaybackState
import android.os.Build
import android.util.Log
import com.lk.music_service_library.R
import com.lk.music_service_library.models.MusicMetadata

/*
 * Erstellt von Lena am 11.05.18.
 * Erstellt eine Benachrichtigung für den Service
 */

class MusicNotification(private val service: MusicService) {

    private val CHANNEL_ID = "plattenspieler_playback"
    private val TAG = "MusicNotification"
    private val ACTION = "com.lk.plattenspieler.ACTION_LAUNCH_PL"

    fun showNotification(state: Int, currentMusicMetadata: MusicMetadata, shuffleOn: Boolean): Notification.Builder{
        //Log.i(TAG, shuffleOn.toString())
        // Channel ab Oreo erstellen
        val nb = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
            Notification.Builder(service, CHANNEL_ID)
        } else {
            Notification.Builder(service)
        }
        // Inhalt setzen
        nb.setContentTitle(currentMusicMetadata.title)
        nb.setContentText(currentMusicMetadata.artist)
        nb.setSubText(currentMusicMetadata.songnr.toString() + " Lieder noch")
        nb.setSmallIcon(R.drawable.notification_stat_playing)
        val albumart = BitmapFactory.decodeFile(currentMusicMetadata.cover_uri)
        if (albumart != null){
            nb.setLargeIcon(albumart)
            Log.v(TAG, "set large Icon")
        }
        // Media Style aktivieren
        nb.setStyle(Notification.MediaStyle()
                .setMediaSession(service.sessionToken)
                .setShowActionsInCompactView(1,2))
        nb.setContentIntent(PendingIntent.getActivity(service.applicationContext, 10,
                Intent(ACTION), PendingIntent.FLAG_UPDATE_CURRENT))
        // korrekt anzeigen, ob Shuffle aktiviert ist
        if(shuffleOn){
            nb.addAction(Notification.Action.Builder(R.mipmap.ic_shuffle, "Shuffle", null).build())
        } else {
            nb.addAction(Notification.Action.Builder(R.color.transparent, "Shuffle", null).build())
        }
        // passendes Icon für Play / Pause anzeigen
        var pi: PendingIntent
        if(state == PlaybackState.STATE_PLAYING){
            pi = PendingIntent.getBroadcast(service, 100,
                    Intent(MusicService.ACTION_MEDIA_PAUSE).setPackage(service.packageName), 0)
            nb.addAction(Notification.Action.Builder(R.mipmap.ic_pause, "Pause",pi).build())
        } else {
            pi = PendingIntent.getBroadcast(service, 101,
                    Intent(MusicService.ACTION_MEDIA_PLAY).setPackage(service.packageName), 0)
            nb.addAction(Notification.Action.Builder(R.mipmap.ic_play, "Play", pi).build())
        }
        pi = PendingIntent.getBroadcast(service, 102,
                Intent(MusicService.ACTION_MEDIA_NEXT).setPackage(service.packageName), 0)
        nb.addAction(Notification.Action.Builder(R.mipmap.ic_next, "Next", pi).build())
        return nb
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

    fun cb(){
        service.applicationContext.sendBroadcast(Intent(MusicService.ACTION_MEDIA_PLAY).setPackage(service.packageName))
    }
}
