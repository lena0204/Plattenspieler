package com.lk.plattenspieler.fragments

import android.app.Activity
import android.app.Fragment
import android.content.res.ColorStateList
import android.graphics.*
import android.media.session.PlaybackState
import android.os.Bundle
import android.util.Log
import android.view.*
import com.lk.music_service_library.models.*
import com.lk.music_service_library.observables.MusicPlaybackState
import com.lk.music_service_library.observables.PlaybackObservable
import com.lk.plattenspieler.R
import com.lk.plattenspieler.utils.ThemeChanger
import kotlinx.android.synthetic.main.bar_music_information.*
import java.util.*

/**
 * Created by Lena on 08.06.17.
 * Zeigt Informationen zum aktuell spielenden Lied (Metadaten) und die Playbackkontrolle an
 */
class MusicBarFragment : Fragment(), java.util.Observer {


    private val TAG = "com.lk.pl-MusicBar"
    private var started = false
    private lateinit var listener: OnClick

    interface OnClick{
        fun onClickPlay()
        fun onClickNext()
        fun onClickPrevious()
    }

    override fun onAttach(activity: Activity?) {
        super.onAttach(activity)
        Log.v(TAG, "onAttach")
        listener = activity as OnClick
    }

    override fun onResume() {
        super.onResume()
        Log.v(TAG, "onResume")
        started = true
        // Farbe setzen falls lineage
        val color = ThemeChanger.getAccentColorLinage(activity)
        if(color != 0){
            ib_main_shuffle.imageTintList = ColorStateList.valueOf(color)
            ib_main_shuffle.imageTintMode = PorterDuff.Mode.SRC_ATOP
        }
    }
    override fun onPause() {
        super.onPause()
        started = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.bar_music_information, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // observe Metadata
        PlaybackObservable.addObserver(this)
        writeMetadata(PlaybackObservable.getMetadata())
        updatePlayback(PlaybackObservable.getState())
        // onClick Listener
        ib_main_play.setOnClickListener {
            listener.onClickPlay()
        }
        ib_main_next.setOnClickListener {
            listener.onClickNext()
        }
        ib_main_previous.setOnClickListener {
            listener.onClickPrevious()
        }
    }

    private fun writeMetadata(data: MusicMetadata){
        if(!data.isEmpty()) {
            var cover: Bitmap?
            cover = BitmapFactory.decodeFile(data.cover_uri)
            if (cover == null) {
                cover = BitmapFactory.decodeResource(resources, R.mipmap.ic_no_cover)
            }
            iv_main_cover.setImageBitmap(cover)
            tv_music_title.text = data.title
            // TODO nicht ganz zuverlässig -> SaveState Handling
        }
    }
    private fun writeEmpty(){
        tv_music_title?.text = ""
        iv_main_cover?.background = null
        ib_main_shuffle?.alpha = 0.5f
    }

    private fun updatePlayback(mps: MusicPlaybackState){
        updateShuffleMode(mps.shuffleOn)
        if(mps.state == PlaybackState.STATE_PLAYING){
            ib_main_play.setImageBitmap(BitmapFactory.decodeResource(resources, R.mipmap.ic_pause))
        } else {
            ib_main_play.setImageBitmap(BitmapFactory.decodeResource(resources, R.mipmap.ic_play))
        }
    }
    private fun updateShuffleMode(mode: Boolean) {
        if(mode){
            ib_main_shuffle?.alpha = 0.8f
        } else {
            ib_main_shuffle?.alpha = 0.3f
        }
    }

    override fun update(o: Observable?, arg: Any?) {
        Log.v(TAG, "update observable")
        if(started) {
            when (arg) {
                is MusicMetadata -> {
                    Log.v(TAG, "Update Metadata")
                    if(!arg.isEmpty()) {
                        writeMetadata(arg)
                    } else {
                        writeEmpty()
                    }
                }
                is MusicPlaybackState -> {
                    Log.v(TAG, "Update playbackstate")
                    updatePlayback(arg)
                }
                else -> Log.w(TAG, "unknown observable update: ${arg.toString()}")
            }
        }
    }
}
