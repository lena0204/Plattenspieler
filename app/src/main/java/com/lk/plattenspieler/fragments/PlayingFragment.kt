package com.lk.plattenspieler.fragments

import android.app.Fragment
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.media.MediaMetadata
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import com.lk.plattenspieler.R
import com.lk.plattenspieler.main.MainActivity
import com.lk.plattenspieler.utils.ThemeChanger
import kotlinx.android.synthetic.main.fragment_playing.*
import kotlinx.android.synthetic.main.fragment_playing.view.*
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.mp3.MP3File
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.id3.ID3v24FieldKey
import org.jaudiotagger.tag.mp4.Mp4FieldKey
import org.jaudiotagger.tag.mp4.Mp4Tag
import java.io.File

/**
 * Created by Lena on 08.06.17.
 */
class PlayingFragment : Fragment(), MainActivity.CallbackPlaying {

    // TODO Sekundenticker für den Fortschritt im Lied einrichten (Seekbar)
    // TODO Previous button um zum Anfang des Liedes zu springen
    // TODO -- Wischgesten um dieses Fragment aufzurufen und zu verstecken

    private val TAG = "com.lk.pl-PlayingFrag"
    private var ivShuffle: ImageView? = null
	private var ivLyrics: ImageView? = null
    private var lvPlaylist: ListView? = null
    private lateinit var args: Bundle
    private var created = false         // Abfangen, dass das Fragment noch nicht angezeigt wird
	private var lyrics: String? = null
	private var coverPath: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val v = inflater.inflate(R.layout.fragment_playing, container, false)
        ivShuffle = v.iv_playing_shuffle
        lvPlaylist = v.lv_playing_list
		ivLyrics = v.iv_playing_lyrics
        //updateShuffleMode()
		ivLyrics?.setOnClickListener {
			if(lyrics != null && lyrics != "") {
				val args = Bundle()
				args.putString("L", lyrics)
				args.putString("C", coverPath)
				val lyricsf = LyricsFragment()
				lyricsf.arguments = args
				fragmentManager.beginTransaction()
						.addToBackStack(null)
						.replace(R.id.frame_layout, lyricsf, "TAG_LYRICS")
						.commit()
			}
		}
        return v
    }
	// TODO Lyrics schreiben über Menü
    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Buttons reichen die Nachrichten über Broadcasts weiter, damit die Interfaces sich
        // nicht in die Quere kommen
        val accentcolor = ThemeChanger().getAccentColor(
                PreferenceManager.getDefaultSharedPreferences(context).getInt(MainActivity.PREF_DESIGN, 0))
        ivShuffle?.backgroundTintList = ColorStateList.valueOf(resources.getColor(accentcolor, activity.theme))
        ivShuffle?.backgroundTintMode = PorterDuff.Mode.SRC_ATOP
		ivLyrics?.backgroundTintList = ColorStateList.valueOf(resources.getColor(accentcolor, activity.theme))
		ivLyrics?.backgroundTintMode = PorterDuff.Mode.SRC_ATOP
		if(lyrics != null && lyrics != ""){
			ivLyrics?.alpha = 1.0f
		}
        activity.actionBar.setTitle(R.string.action_playing)
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
    private fun setMetaData(args: Bundle){
        val data = args.getString("T")
        if(data.isNotEmpty()) {
            val dataArray = data.split("__".toRegex())
            if (dataArray[1] != "null") {
                // ID ist an Stelle 0 gespeichert
                tv_playing_album.text = dataArray[3]
                tv_playing_songnumber.text = dataArray[6]
                tv_playing_songnumber.append(" " + getString(R.string.songs))
                // ms umrechnen auf Minuten und Sekunden
                var dur: Long = dataArray[4].toLong()
                dur /= 1000
                val min = (dur / 60).toInt()
                val sec = (dur % 60).toInt()
                var s = String.format("%02d", sec)
				s = "$min:$s"
                tv_playing_duration.text = s
                // Cover anzeigen
                val cover = Drawable.createFromPath(dataArray[5])
				coverPath = dataArray[5]
                ll_playing_fragment.background = cover
				// Lyrics abfragen
				lyricsAbfragen(dataArray[7])
            }
        }
        //Log.i(TAG, "Hat Key shuffle: " + args.containsKey("shuffle").toString())
        val shuffle = args.getBoolean("shuffle", false)
        updateShuffleMode(shuffle)
    }
    private fun setPlaylist(items: String){
        val itemsArray = items.split(Regex("__"))
        val stringItems: Array<String> = itemsArray.toTypedArray()
        lvPlaylist?.adapter = ArrayAdapter(context, R.layout.row_playlist_tv, stringItems)
    }

	private fun lyricsAbfragen(filepath: String){
		ivLyrics?.alpha = 0.4f
		this.lyrics = null
		Log.d(TAG, filepath)
		if(filepath.contains("mp3")){
			// Mp3 Datei
			val mp3File = AudioFileIO.read(File(filepath)) as MP3File
			if(mp3File.hasID3v2Tag()) {
				var lyrics = mp3File.iD3v2TagAsv24.getFirst(ID3v24FieldKey.LYRICS)
				if (lyrics != null && lyrics != "") {
					ivLyrics?.alpha = 1.0f
					this.lyrics = lyrics
					if (lyrics.length > 120) {
						lyrics = lyrics.substring(0, 120)
					}
					Log.d(TAG, "MP3-Datei hat Lyrics abgespeichert: $lyrics")
				}
			}
		} else {
			// m4a Datei
			val m4aTag = AudioFileIO.read(File(filepath)).tag as Mp4Tag
			var lyrics = m4aTag.getFirst(Mp4FieldKey.LYRICS)
			if(lyrics != null  && lyrics != ""){
				ivLyrics?.alpha = 1.0f
				this.lyrics = lyrics
				if (lyrics.length > 60) {
					lyrics = lyrics.substring(0, 60)
				}
				Log.d(TAG, "M4A-Datei hat Lyrics abgespeichert: $lyrics")
			}
		}
	}

    // Updates während des Anzeigen dieses Fragments
    override fun updateMetadata(data: MediaMetadata, queue: String) {
        if(created) {
            if(data.getString(MediaMetadata.METADATA_KEY_TITLE) != "null") {
                setPlaylist(queue)
                tv_playing_album.text = data.getString(MediaMetadata.METADATA_KEY_ALBUM)
                tv_playing_songnumber.text = data.getLong(MediaMetadata.METADATA_KEY_NUM_TRACKS).toString()
                tv_playing_songnumber.append(" Lieder")
                var dur = data.getLong(MediaMetadata.METADATA_KEY_DURATION)
                dur /= 1000
                val min = (dur / 60).toInt()
                val sec = dur % 60
                var s = String.format("%02d", sec)
				s = "$min:$s"
                tv_playing_duration.text = s
                val cover = Drawable.createFromPath(data.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI))
                ll_playing_fragment.background = cover
				coverPath = data.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
				// Lyrics abfragen
				lyricsAbfragen(data.getString(MediaMetadata.METADATA_KEY_WRITER))
            }
        }
    }

    // Button passend anzeigen, wenn Zufallswiedergabe erfolgt
    override fun updateShuffleMode(mode: Boolean) {
        if(mode){
            ivShuffle?.alpha = 1.0f
        } else {
            ivShuffle?.alpha = 0.4f
        }
    }

}