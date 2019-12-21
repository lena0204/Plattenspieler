package com.lk.plattenspieler.fragments

import android.content.res.ColorStateList
import android.graphics.*
import android.media.session.PlaybackState
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.lk.musicservicelibrary.main.MusicService
import com.lk.musicservicelibrary.models.MusicMetadata
import com.lk.musicservicelibrary.utils.CoverLoader
import com.lk.plattenspieler.R
import com.lk.plattenspieler.main.ThemeChanger
import com.lk.plattenspieler.musicbrowser.ControllerAction
import com.lk.plattenspieler.musicbrowser.EnumActions
import com.lk.plattenspieler.observables.PlaybackViewModel
import kotlinx.android.synthetic.main.bar_music_information.*

/**
 * Created by Lena on 08.06.17.
 * Zeigt Informationen zum aktuell spielenden Lied (Metadaten) und die Playbackkontrolle an
 */
class MusicBarFragment : Fragment(), Observer<Any> {

    private val TAG = "com.lk.pl-MusicBar"
    private lateinit var playbackViewModel: PlaybackViewModel

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
            val action = ControllerAction(EnumActions.PLAY_PAUSE)
            playbackViewModel.callAction(action)
        }
        ib_main_next.setOnClickListener {
            val action = ControllerAction(EnumActions.NEXT)
            playbackViewModel.callAction(action)
        }
        ib_main_previous.setOnClickListener {
            val action = ControllerAction(EnumActions.PREVIOUS)
            playbackViewModel.callAction(action)
        }
    }

    private fun setupData(){
        playbackViewModel = ViewModelProviders.of(requireActivity()).get(PlaybackViewModel::class.java)
        playbackViewModel.setObserverToAll(this, this)
        writeMetadata(playbackViewModel.getMetadata())
        updatePlayback(playbackViewModel.getPlaybackState())
    }

    override fun onChanged(update: Any?) {
        when(update) {
            is MusicMetadata -> {
                if(update.isEmpty())
                    writeEmpty()
                else
                    writeMetadata(update)
            }
            is PlaybackState -> updatePlayback(update)
        }
    }

    private fun writeMetadata(data: MusicMetadata){
        if(!data.isEmpty()) {
            val cover = CoverLoader.decodeAlbumCover(requireContext(), data.content_uri, data.cover_uri)
            iv_main_cover.setImageBitmap(cover)
            tv_music_title.text = data.title
            // TODO nicht ganz zuverlässig -> SaveState Handling
        }
    }

    private fun updatePlayback(playbackState: PlaybackState){
        updateShuffleMode(playbackState)
        if(playbackState.state == PlaybackState.STATE_PLAYING){
            ib_main_play.setImageBitmap(BitmapFactory.decodeResource(resources, R.mipmap.ic_pause))
        } else {
            ib_main_play.setImageBitmap(BitmapFactory.decodeResource(resources, R.mipmap.ic_play))
        }
    }

    private fun updateShuffleMode(playbackState: PlaybackState) {
        val mode = playbackState.extras?.getBoolean(MusicService.SHUFFLE_KEY)
        if(mode == true){
            ib_main_shuffle?.alpha = 0.8f
        } else {
            ib_main_shuffle?.alpha = 0.3f
        }
    }

    override fun onResume() {
        super.onResume()
        setAccentColorIfLineageTheme()
    }

    private fun setAccentColorIfLineageTheme(){
        val color = ThemeChanger.getAccentColorLinage(activity)
        if(color != 0){
            ib_main_shuffle.imageTintList = ColorStateList.valueOf(color)
            ib_main_shuffle.imageTintMode = PorterDuff.Mode.SRC_ATOP
        }
    }

    private fun writeEmpty(){
        tv_music_title?.text = ""
        iv_main_cover?.setImageResource(R.color.transparent)
        ib_main_shuffle?.alpha = 0.5f
    }
}
