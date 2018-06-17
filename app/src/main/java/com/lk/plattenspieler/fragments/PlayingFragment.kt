package com.lk.plattenspieler.fragments

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.lk.plattenspieler.R
import com.lk.plattenspieler.main.MainActivityNew
import com.lk.plattenspieler.models.*
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
    // IDEA_ Previous button um zum Anfang des Liedes zu springen
    // IDEA_ -- Wischgesten um dieses Fragment aufzurufen und zu verstecken

    private val TAG = "com.lk.pl-PlayingFrag"
	private var lyrics: String? = null
    private var started = false

    override fun onResume() {
        super.onResume()
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
        setPlaylist(PlaybackObservable.getQueue())
        updateShuffleMode(PlaybackObservable.getState().shuffleOn)
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
                        ?.replace(R.id.frame_layout, lyricsf, "TAG_LYRICS")
                        ?.commit()
            }
        }
        val accentcolor = ThemeChanger.getAccentColor(
                ThemeChanger.readThemeFromPreferences(PreferenceManager.getDefaultSharedPreferences(activity)), activity!!)
        if(accentcolor != 0) {
            view.iv_playing_shuffle.backgroundTintList = ColorStateList.valueOf(resources.getColor(accentcolor, activity?.theme))
            view.iv_playing_lyrics.backgroundTintList = ColorStateList.valueOf(resources.getColor(accentcolor, activity?.theme))
        } else {
            view.iv_playing_shuffle.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
            view.iv_playing_lyrics.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
        }
        view.iv_playing_shuffle.backgroundTintMode = PorterDuff.Mode.SRC_ATOP
        view.iv_playing_lyrics.backgroundTintMode = PorterDuff.Mode.SRC_ATOP
        activity?.actionBar?.setTitle(R.string.action_playing)
    }

    private fun writeMetadata(data: MusicMetadata){
        if(!data.isEmpty()) {
            tv_playing_album.text = data.album
            tv_playing_songnumber.text = data.songnr.toString()
            tv_playing_songnumber.append(" " + getString(R.string.songs))
            tv_playing_duration.text = data.getDuration()
            var cover = Drawable.createFromPath(data.cover_uri)
            if (cover == null){
                cover = resources.getDrawable(R.mipmap.ic_no_cover, activity?.theme)
            }
            ll_playing_fragment.background = cover
            // Lyrics abfragen
            val optLyrics = PreferenceManager.getDefaultSharedPreferences(activity).getInt(MainActivityNew.PREF_LYRICS,0)
            if(optLyrics == 1) {
                lyricsAbfragen(data.path)
            }
            // TODO nicht ganz zuverlässig -> SaveState Handling
        }
    }

    private fun setPlaylist(queue: MusicList){
        val liste = ArrayList<String>()
        for (item in queue) {
            liste.add(item.title + "\n - " + item.artist)
        }
        lv_playing_list.adapter = ArrayAdapter(context, R.layout.row_playlist_tv, liste.toTypedArray())
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

    private fun updateShuffleMode(mode: Boolean) {
        if(mode){
            iv_playing_shuffle.alpha = 1.0f
        } else {
            iv_playing_shuffle.alpha = 0.4f
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
                is MusicPlaybackState -> {
                    Log.v(TAG, "Update playbackstate")
                    updateShuffleMode(arg.shuffleOn)
                }
                else -> Log.w(TAG, "unknown observable update: ${arg.toString()}")
            }
        }
    }
}
