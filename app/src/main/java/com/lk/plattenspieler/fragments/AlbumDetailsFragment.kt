package com.lk.plattenspieler.fragments

import android.content.res.ColorStateList
import android.graphics.*
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.lk.musicservicelibrary.main.MusicService
import com.lk.musicservicelibrary.models.MusicList
import com.lk.musicservicelibrary.models.MusicMetadata
import com.lk.plattenspieler.R
import com.lk.plattenspieler.main.ThemeChanger
import com.lk.plattenspieler.musicbrowser.ControllerAction
import com.lk.plattenspieler.musicbrowser.EnumActions
import com.lk.plattenspieler.observables.*
import com.lk.plattenspieler.utils.*
import kotlinx.android.synthetic.main.fragment_album_details.*

/**
* Erstellt von Lena am 08.06.17.
 * Stellt eine Liste von Titel in einem Album dar, inkl. Verwaltung von Observer und RecyclerView
*/
class AlbumDetailsFragment: Fragment(), TitleAdapter.OnClickTitle, Observer<MusicList> {

    private val TAG = "com.lk.pl-AlbumDetailsF"
    private var data = MusicList()

    private lateinit var mediaListViewModel: MediaViewModel
    private lateinit var playbackViewModel: PlaybackViewModel

    override fun onClick(titleId: String) {
        val action = ControllerAction(EnumActions.PLAY_FROM_ID, titleId, args = shuffleBundle(false))
        playbackViewModel.controllerAction.value = action
    }

    private fun shuffleBundle(shuffle: Boolean): Bundle {
        return bundleOf(MusicService.SHUFFLE_KEY to shuffle)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
        = inflater.inflate(R.layout.fragment_album_details, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        fab_shuffle.setOnClickListener {
            val action = ControllerAction(EnumActions.SHUFFLE,
                    data.getItemAt(0).id,
                    args = shuffleBundle(true))
            playbackViewModel.controllerAction.value = action
        }
        mediaListViewModel = ViewModelProviders.of(requireActivity()).get(MediaViewModel::class.java)
        mediaListViewModel.titleList.observe(this, this)
        playbackViewModel = ViewModelProviders.of(requireActivity()).get(PlaybackViewModel::class.java)
        setupRecyclerView(mediaListViewModel.titleList.value!!)
        setColorIfLineageTheme()
    }

    override fun onChanged(titlesList: MusicList?) {
        setupRecyclerView(titlesList!!)
    }

    private fun setupRecyclerView(list: MusicList){
        data = MusicList()
        var album = ""
        for(item in list){
            if(!item.isEmpty()){
                album = item.album
                item.cover = MusicMetadata.decodeAlbumCover(item.cover_uri, resources)
                data.addItem(item)
            }
        }
        setAlbumTitleToActionBar(album)
        recycler_titles.layoutManager = LinearLayoutManager(activity)
        recycler_titles.adapter = TitleAdapter(data, this)
    }


    private fun setAlbumTitleToActionBar(album: String){
        if(album.isNotEmpty()){
            if(ThemeChanger.themeIsLineage(activity) && activity?.actionBar?.customView != null) {
                (activity?.actionBar?.customView as TextView).text = album
            }
            activity?.actionBar?.title = album
        }
    }

    private fun setColorIfLineageTheme(){
        val color = ThemeChanger.getAccentColorLinage(activity)
        if(color != 0){
            fab_shuffle.backgroundTintList = ColorStateList.valueOf(color)
            fab_shuffle.backgroundTintMode = PorterDuff.Mode.SRC_ATOP
        }
    }

}