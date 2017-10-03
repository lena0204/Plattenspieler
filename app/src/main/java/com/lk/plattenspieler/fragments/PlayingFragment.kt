package com.lk.plattenspieler.fragments

import android.app.Fragment
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import com.lk.plattenspieler.R
import com.lk.plattenspieler.main.MainActivity
import kotlinx.android.synthetic.main.fragment_playing.*
import kotlinx.android.synthetic.main.fragment_playing.view.*

/**
 * Created by Lena on 08.06.17.
 */
class PlayingFragment : Fragment(), MainActivity.CallbackPlaying {

    // TODO Sekundenticker für den Fortschritt im Lied einrichten

    val TAG = "PlayingFragment"
    var iv_shuffle: ImageView? = null
    var lv_playlist: ListView? = null
    var shuffle_bo: Boolean = false
    lateinit var args: Bundle
    var created = false         // Abfangen, dass das Fragment noch nicht angezeigt wird

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val v = inflater.inflate(R.layout.fragment_playing, container, false)
        iv_shuffle = v.iv_playing_shuffle
        lv_playlist = v.lv_playing_list
        return v
    }
    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Buttons reichen die Nachrichten über Broadcasts weiter, damit die Interfaces sich
        // nicht in die Quere kommen
        activity.actionBar.setTitle(R.string.action_playing)
        if(shuffle_bo){
            iv_shuffle?.alpha = 1.0f
        } else {
            iv_shuffle?.alpha = 0.4f
        }
        args = this.arguments
        created = true
        setMetaData(args)
        setPlaylist(args.getString("Q"))
    }
    override fun onDestroy() {
        super.onDestroy()
        created = false
    }

    // wenn eine neue Instanz von PlayingFragment erstellt wird
    fun setMetaData(args: Bundle){
        val data = args.getString("T")
        if(data.isNotEmpty()){
            val dataArray = data.split("__".toRegex())
            if(dataArray[1] != "null") {
                // ID ist an Stelle 0 gespeichert
                tv_playing_album.text = dataArray[3]
                tv_playing_songnumber.text = dataArray[6]
                tv_playing_songnumber.append(" " + getString(R.string.songs))
                // ms umrechnen auf Minuten und Sekunden
                var dur: Long = dataArray[4].toLong()
                dur /= 1000
                val min = (dur / 60).toInt()
                val sec = (dur % 60).toInt()
                val s = String.format("%02d", sec)
                tv_playing_duration.text = "$min:$s"
                // Cover anzeigen
                val cover = Drawable.createFromPath(dataArray[5])
                ll_playing_fragment.background = cover
            }
        }
    }
    fun setPlaylist(items: String){
        val itemsArray = items.split(Regex("__"))
        val stringItems: Array<String> = itemsArray.toTypedArray()
        lv_playlist?.adapter = ArrayAdapter(context, R.layout.row_playlist_tv, stringItems)
    }

    // Updates während des Anzeigen dieses Fragments
    override fun updateMetadata(data: MediaMetadataCompat, queue: String) {
        if(created) {
            if(data.getString(MediaMetadataCompat.METADATA_KEY_TITLE) != "null") {
                setPlaylist(queue)
                tv_playing_album.text = data.getString(MediaMetadataCompat.METADATA_KEY_ALBUM)
                tv_playing_songnumber.text = data.getLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS).toString()
                tv_playing_songnumber.append(" Lieder")
                var dur = data.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                dur /= 1000
                val min = (dur / 60).toInt()
                val sec = dur % 60
                val s = String.format("%02d", sec)
                tv_playing_duration.text = "$min:$s"
                val cover = Drawable.createFromPath(data.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI))
                ll_playing_fragment.background = cover
            }
        }
    }

    // Button passend anzeigen, wenn Zufallswiedergabe erfolgt
    override fun updateShuffleMode(mode: Boolean) {
        if(mode){
            iv_shuffle?.alpha = 1.0f
            shuffle_bo = true
        } else {
            iv_shuffle?.alpha = 0.4f
            shuffle_bo = false
        }
    }

}