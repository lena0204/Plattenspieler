package com.lk.plattenspieler.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.lk.plattenspieler.R
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.lk.musicservicelibrary.models.MusicList
import com.lk.musicservicelibrary.models.MusicMetadata
import com.lk.musicservicelibrary.utils.CoverLoader
import com.lk.plattenspieler.main.ThemeChanger
import com.lk.plattenspieler.musicbrowser.ControllerAction
import com.lk.plattenspieler.musicbrowser.EnumActions
import com.lk.plattenspieler.observables.*
import com.lk.plattenspieler.utils.*
import kotlinx.android.synthetic.main.fragment_album.*

/**
 * Created by Lena on 08.06.17.
 * Stellt eine Liste von Alben dar, verwaltet den Observer und den RecyclerView
 */
class AlbumFragment: Fragment(), AlbumAdapter.Click, Observer<MusicList> {

    private val TAG = "AlbumFragment"
    private lateinit var mediaViewModel: MediaViewModel
    private lateinit var playbackViewModel: PlaybackViewModel

    override fun onClick(albumid: String) {
        val action = ControllerAction(EnumActions.SHOW_ALBUM, albumid)
        playbackViewModel.callAction(action)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_album, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mediaViewModel = ViewModelProviders.of(requireActivity()).get(MediaViewModel::class.java)
        mediaViewModel.albumList.observe(this, this)
        playbackViewModel = ViewModelProviders.of(requireActivity()).get(PlaybackViewModel::class.java)
        setTitleInActionbar()
        if(mediaViewModel.albumList.value != null) {
            setupRecyclerView(mediaViewModel.albumList.value!!)
        }
    }

    override fun onChanged(albumList: MusicList?) {
        setupRecyclerView(albumList!!)
    }

    private fun setTitleInActionbar(){
        if(ThemeChanger.themeIsLineage(activity) && activity?.actionBar?.customView != null)
            (activity?.actionBar?.customView as TextView).text = resources.getString(R.string.app_name)
        activity?.actionBar?.title = getString(R.string.app_name)
    }

    private fun setupRecyclerView(list: MusicList){
        val data = getDataFromMusicList(list)
        recycler_album.layoutManager = LinearLayoutManager(activity)
        recycler_album.adapter = AlbumAdapter(data, this)
    }

    private fun getDataFromMusicList(list: MusicList): MusicList{
        val data = MusicList()
        for (item in list) {
            if (!item.isEmpty()) {
                item.cover = CoverLoader.decodeAlbumCover(requireContext(), item.content_uri, item.cover_uri)
                item.allTracksFormatted = item.num_tracks_album.toString() + " " + resources.getString(R.string.songs)
                data.addItem(item)
            }
        }
        return data
    }
}