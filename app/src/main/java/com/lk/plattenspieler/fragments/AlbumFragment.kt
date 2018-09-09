package com.lk.plattenspieler.fragments

import android.app.Activity
import android.media.browse.MediaBrowser
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.lk.plattenspieler.R
import java.util.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.lk.plattenspieler.observables.MedialistsObservable
import com.lk.musicservicelibrary.models.MusicList
import com.lk.musicservicelibrary.models.MusicMetadata
import com.lk.plattenspieler.utils.AlbumAdapter
import com.lk.plattenspieler.utils.ThemeChanger
import kotlinx.android.synthetic.main.fragment_album.*

/**
 * Created by Lena on 08.06.17.
 * Stellt eine Liste von Alben dar, verwaltet den Observer und den RecyclerView
 */
class AlbumFragment: Fragment(), AlbumAdapter.Click, Observer {

    private val TAG = "AlbumFragment"
    private lateinit var listener: OnClick
    private var started = false

    interface OnClick{
        fun onClickAlbum(albumid: String)
    }

    override fun onClick(albumid: String) {
        listener.onClickAlbum(albumid)
    }

    override fun onAttach(activity: Activity?) {
        super.onAttach(activity)
        listener = activity as OnClick
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_album, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        started = true
        MedialistsObservable.addObserver(this)
        setTitleInActionbar()
        setupRecyclerView(MedialistsObservable.getAlbumList())
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
                item.id = item.id.replace("ALBUM-", "")
                item.cover = MusicMetadata.decodeAlbumcover(item.cover_uri, resources)
                item.alltracks = item.num_tracks_album.toString() + " " + resources.getString(R.string.songs)
                data.addItem(item)
            }
        }
        return data
    }

    override fun update(o: Observable?, arg: Any?) {
        if(arg is MusicList){
            if(arg.getFlag() == MediaBrowser.MediaItem.FLAG_BROWSABLE) {
                if(started) {
                    setupRecyclerView(arg)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        started = false
    }

    override fun onDestroy() {
        super.onDestroy()
        MedialistsObservable.deleteObserver(this)
    }
}