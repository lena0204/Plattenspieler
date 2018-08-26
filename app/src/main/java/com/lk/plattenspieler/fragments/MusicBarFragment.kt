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
import com.lk.music_service_library.observables.*
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.bar_music_information, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupOnClickListener()
        setupData()
    }

    private fun setupOnClickListener(){
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

    private fun setupData(){
        PlaybackDataObservable.addObserver(this)
        writeMetadata(PlaybackDataObservable.metadata)
        updatePlayback(PlaybackDataObservable.getState())
    }

    private fun writeMetadata(data: MusicMetadata){
        if(!data.isEmpty()) {
            iv_main_cover.setImageBitmap(data.cover)
            tv_music_title.text = data.title
            // TODO nicht ganz zuverlässig -> SaveState Handling
        }
    }

    private fun updatePlayback(playbackState: PlaybackState){
        updateShuffleMode(PlaybackDataObservable.shuffleOn)
        if(playbackState.state == PlaybackState.STATE_PLAYING){
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

    override fun onResume() {
        super.onResume()
        Log.v(TAG, "onResume")
        started = true
        setAccentColorIfLineageTheme()
    }

    private fun setAccentColorIfLineageTheme(){
        val color = ThemeChanger.getAccentColorLinage(activity)
        if(color != 0){
            ib_main_shuffle.imageTintList = ColorStateList.valueOf(color)
            ib_main_shuffle.imageTintMode = PorterDuff.Mode.SRC_ATOP
        }
    }

    override fun update(o: Observable?, arg: Any?) {
        Log.v(TAG, "update observable")
        if(started && arg is PlaybackActions) {
            when (arg) {
                PlaybackActions.ACTION_UPDATE_METADATA -> {
                    val metadata = PlaybackDataObservable.metadata
                    if(!metadata.isEmpty()) {
                        writeMetadata(metadata)
                    } else {
                        writeEmpty()
                    }
                }
                PlaybackActions.ACTION_UPDATE_PLAYBACKSTATE -> {
                    updatePlayback(PlaybackDataObservable.getState())
                }
            }
        }
    }

    private fun writeEmpty(){
        tv_music_title?.text = ""
        iv_main_cover?.background = null
        ib_main_shuffle?.alpha = 0.5f
    }

    override fun onPause() {
        super.onPause()
        started = false
    }

    override fun onDestroy() {
        super.onDestroy()
        PlaybackDataObservable.deleteObserver(this)
    }
}
