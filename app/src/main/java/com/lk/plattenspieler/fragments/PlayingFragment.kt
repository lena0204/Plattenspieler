package com.lk.plattenspieler.fragments

import android.app.Fragment
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.lk.music_service_library.models.*
import com.lk.plattenspieler.R
import com.lk.plattenspieler.utils.LyricsAccess
import com.lk.plattenspieler.utils.ThemeChanger
import kotlinx.android.synthetic.main.fragment_playing.*
import kotlinx.android.synthetic.main.fragment_playing.view.*
import java.util.*

/**
 * Created by Lena on 08.06.17.
 * Zeigt Informationen zum aktuell spielenden Lied (Metadaten) und die Wiedergabeliste an
 */
class PlayingFragment : Fragment(), java.util.Observer {

    // IDEA_ Sekundenticker für den Fortschritt im Lied einrichten (Seekbar)
    // IDEA_ -- Wischgesten um dieses Fragment aufzurufen und zu verstecken

    private val TAG = "com.lk.pl-PlayingFrag"
	private var lyrics: String? = null
    private var started = false

    override fun onResume() {
        super.onResume()
        // Farbe setzen falls lineage
        val color = ThemeChanger.getAccentColorLinage(activity)
        if(color != 0){
            iv_playing_lyrics.backgroundTintList = ColorStateList.valueOf(color)
            iv_playing_lyrics.backgroundTintMode = PorterDuff.Mode.SRC_ATOP
        }
        if(lyrics != null && lyrics != ""){
            iv_playing_lyrics.alpha = 1.0f
        }
        started = true
    }
    override fun onPause() {
        super.onPause()
        started = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_playing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // observe Metadata
        PlaybackObservable.addObserver(this)
        writeMetadata(PlaybackObservable.getMetadata())
        setPlaylist(PlaybackObservable.getQueueLimitedTo30())
        // onClick Listener
        view.iv_playing_lyrics.setOnClickListener {
            if (lyrics != null && lyrics != "") {
                val args = Bundle()
                args.putString("L", lyrics)
                if(!PlaybackObservable.getMetadata().isEmpty()) {
                    args.putString("C", PlaybackObservable.getMetadata().cover_uri)
                }
                val lyricsf = LyricsFragment()
                lyricsf.arguments = args
                fragmentManager?.beginTransaction()
                        ?.addToBackStack(null)
                        ?.replace(R.id.fl_main_content, lyricsf, "TAG_LYRICS")
                        ?.commit()
            }
        }
        if(ThemeChanger.themeIsLineage(activity))
            (activity?.actionBar?.customView as TextView).text = resources.getString(R.string.action_playing)
        activity?.actionBar?.title = getString(R.string.action_playing)
    }

    private fun writeMetadata(data: MusicMetadata){
        if(!data.isEmpty()) {
            tv_playing_title.text = data.title
            tv_playing_artist.text = data.artist
            tv_playing_album.text = data.album
            tv_playing_songnumber.text = data.nr_of_songs_left.toString()
            tv_playing_songnumber.append(" " + getString(R.string.songs))
            tv_playing_duration.text = data.getDurationAsFormattedText()
            // TODO cover wird noch nicht gut eingesetzt weil je nachdem Bitmap oder Drawable erforderlich ist
            var cover = Drawable.createFromPath(data.cover_uri)
            if (cover == null){
                cover = resources.getDrawable(R.mipmap.ic_no_cover, activity?.theme)
            }
            ll_playing_fragment.background = cover
            // Lyrics abfragen
            val optLyrics = PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("PREF_LYRICSSHOW",true)
            if(optLyrics) {
                lyricsAbfragen(data.path)
            }
            // TODO nicht ganz zuverlässig -> SaveState Handling
        }
    }

    private fun setPlaylist(queue: MusicList){
        Log.v(TAG, "SetPlaylist")
        val liste = ArrayList<String>()
        for (item in queue) {
            liste.add(item.title + "\n - " + item.artist)
        }
        lv_playing_list.adapter = ArrayAdapter(activity.applicationContext, R.layout.row_playlist_tv, liste.toTypedArray())
    }

	private fun lyricsAbfragen(filepath: String){
		iv_playing_lyrics.alpha = 0.4f
		this.lyrics = null
		val texte = LyricsAccess.readLyrics(filepath)
        if(texte != ""){
            this.lyrics = texte
            iv_playing_lyrics.alpha = 1.0f
        }
	}

    override fun update(o: Observable?, arg: Any?) {
        if(started) {
            when (arg) {
                is MusicMetadata -> {
                    Log.v(TAG, "Update Metadata")
                    writeMetadata(arg)
                }
                is MusicList -> {
                    Log.v(TAG, "Update queue")
                    setPlaylist(arg)
                }
                else -> Log.v(TAG, "unknown observable update: ${arg.toString()}")
            }
        }
    }
}
