package com.lk.plattenspieler.fragments

import android.app.Activity
import android.app.Fragment
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.browse.MediaBrowser
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.lk.plattenspieler.R
import java.util.*
import android.support.v7.widget.DividerItemDecoration
import android.widget.TextView
import com.lk.music_service_library.observables.MedialistsObservable
import com.lk.music_service_library.models.MusicList
import com.lk.plattenspieler.utils.AlbumAdapter
import com.lk.plattenspieler.utils.ThemeChanger
import kotlinx.android.synthetic.main.fragment_album.*

/**
 * Created by Lena on 08.06.17.
 * Stellt eine Liste von Alben dar, verwaltet den Observer und den RecyclerView
 */
class AlbumFragment: Fragment(), AlbumAdapter.Click, Observer {

    private val TAG = "com.lk.pl-AlbumFragment"
    // Listener und Interface, um onClick weiterzureichen
    private lateinit var listener: OnClick
    private var albums = MusicList()
    private var started = false

    interface OnClick{
        fun onClickAlbum(albumid: String)
    }

    // Listener Methoden vom Adapter
    override fun onClick(albumid: String) {
        listener.onClickAlbum(albumid)
    }

    override fun onAttach(activity: Activity?) {
        super.onAttach(activity)
        Log.v(TAG, "onAttach")
        listener = activity as OnClick
    }

    override fun onStop() {
        super.onStop()
        started = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_album, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        started = true
        MedialistsObservable.addObserver(this)
        if(ThemeChanger.themeIsLineage(activity) && activity?.actionBar?.customView != null)
            (activity?.actionBar?.customView as TextView).text = resources.getString(R.string.app_name)
        activity?.actionBar?.title = getString(R.string.app_name)
        setupRecyclerView(MedialistsObservable.getAlbumList())
    }

    private fun setupRecyclerView(list: MusicList){
        val data = MusicList()
        for (item in list) {
            if (!item.isEmpty()) {
                item.id = item.id.replace("ALBUM-", "")
                // Cover umwandeln
                var albumart: Bitmap?
                albumart = BitmapFactory.decodeFile(item.cover_uri)
                if (albumart == null) {
                    albumart = BitmapFactory.decodeResource(resources, R.mipmap.ic_no_cover)
                }
                item.cover = albumart
                item.alltracks = item.num_tracks_album.toString() + " " + resources.getString(R.string.songs)
                data.addItem(item)
            }
        }
        val lmanager = LinearLayoutManager(activity)
        val divider = DividerItemDecoration(activity, lmanager.orientation)
        //recycler_album.addItemDecoration(divider)
        recycler_album.layoutManager = lmanager
        recycler_album.adapter = AlbumAdapter(data, this)
    }

    override fun update(o: Observable?, arg: Any?) {
        if(arg is MusicList){
            if(arg.getFlag() == MediaBrowser.MediaItem.FLAG_BROWSABLE) {
                albums = arg
                if(started) {
                    setupRecyclerView(albums)
                }
            }
        }
    }

}