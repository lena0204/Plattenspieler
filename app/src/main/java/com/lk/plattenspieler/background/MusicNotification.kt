package com.lk.plattenspieler.background

import android.app.*
import android.content.*
import android.graphics.BitmapFactory
import android.media.session.PlaybackState
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Log
import com.lk.plattenspieler.R
import com.lk.plattenspieler.main.MainActivity
import com.lk.plattenspieler.models.MusicMetadata

/**
 * Erstellt von Lena am 11.05.18.
 */
class MusicNotification(val service: MusicService): BroadcastReceiver() {

    private val STATE_STOP = 1
    private val STATE_PLAY = 2
    private val STATE_PAUSE = 3
    private val STATE_NEXT = 4
    private val CHANNEL_ID = "plattenspieler_playback"
    private val TAG = "MusicNotification"
    private var playingstate = STATE_STOP

    private val ACTION_MEDIA_PLAY = "com.lk.pl-ACTION_MEDIA_PLAY"
    private val ACTION_MEDIA_PAUSE = "com.lk.pl-ACTION_MEDIA_PAUSE"
    private val ACTION_MEDIA_NEXT = "com.lk.pl-ACTION_MEDIA_NEXT"

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
        }
        // Media Style aktivieren
        nb.setStyle(Notification.MediaStyle()
                .setMediaSession(service.sessionToken)
                .setShowActionsInCompactView(1,2))
        val i = Intent(service.applicationContext, MainActivity::class.java)
        nb.setContentIntent(PendingIntent.getActivity(service, 0, i, PendingIntent.FLAG_UPDATE_CURRENT))
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
                    Intent(ACTION_MEDIA_PAUSE).setPackage(service.packageName), 0)
            nb.addAction(Notification.Action.Builder(R.mipmap.ic_pause, "Pause",pi).build())
        } else {
            pi = PendingIntent.getBroadcast(service, 100,
                    Intent(ACTION_MEDIA_PLAY).setPackage(service.packageName), 0)
            nb.addAction(Notification.Action.Builder(R.mipmap.ic_play, "Play", pi).build())
        }
        pi = PendingIntent.getBroadcast(service, 100,
                Intent(ACTION_MEDIA_NEXT).setPackage(service.packageName), 0)
        nb.addAction(Notification.Action.Builder(R.mipmap.ic_next, "Next", pi).build())
        registerBroadcast()
        return nb
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Music playback", NotificationManager.IMPORTANCE_LOW)
        channel.description = "Music playback controls"
        channel.setShowBadge(false)
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        val manager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
    private fun registerBroadcast(){
        val ifilter = IntentFilter()
        ifilter.addAction(ACTION_MEDIA_PLAY)
        ifilter.addAction(ACTION_MEDIA_PAUSE)
        ifilter.addAction(ACTION_MEDIA_NEXT)
        service.registerReceiver(this, ifilter)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent != null){
            Log.d(TAG, "onReceive: " + intent.action)
            when(intent.action){
                ACTION_MEDIA_PLAY -> {
                    if(playingstate == STATE_PAUSE){
                        service.handleOnPlay()
                        playingstate = STATE_PLAY
                        //Log.d(TAG, "PLAY: playingstate: " + playingstate + ", Playbackstate:" + service..controller.playbackState.state)
                    }
                }
                ACTION_MEDIA_PAUSE -> {
                    if(playingstate == STATE_PLAY){
                        service.handleOnPause()
                        playingstate = STATE_PAUSE
                        //Log.d(TAG, "PAUSE: playingstate: " + playingstate + ", Playbackstate:" + msession.controller.playbackState.state)

                    }
                }
                ACTION_MEDIA_NEXT -> {
                    if(playingstate == STATE_PLAY || playingstate == STATE_PAUSE) {
                        service.handleOnNext()
                        playingstate = STATE_NEXT
                        //Log.d(TAG, "NEXT: playingstate: " + playingstate + ", Playbackstate:" + msession.controller.playbackState.state)
                    }
                }
                else -> Log.d(TAG, "Neuer Intent mit Action:${intent.action}")
            }
        }
    }
}