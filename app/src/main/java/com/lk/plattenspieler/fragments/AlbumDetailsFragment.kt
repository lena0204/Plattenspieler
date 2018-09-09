package com.lk.plattenspieler.fragments

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.*
import android.media.browse.MediaBrowser
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.lk.plattenspieler.observables.MedialistsObservable
import com.lk.musicservicelibrary.models.MusicList
import com.lk.musicservicelibrary.models.MusicMetadata
import com.lk.plattenspieler.R
import com.lk.plattenspieler.utils.ThemeChanger
import com.lk.plattenspieler.utils.TitleAdapter
import kotlinx.android.synthetic.main.fragment_album_details.*
import java.util.*

/**
* Erstellt von Lena am 08.06.17.
 * Stellt eine Liste von Titel in einem Album dar, inkl. Verwaltung von Observer und RecyclerView
*/
class AlbumDetailsFragment: Fragment(), TitleAdapter.OnClickTitle, Observer {

    private val TAG = "com.lk.pl-AlbumDetailsF"
    private lateinit var listener: OnClick
    private var data = MusicList()
    private var attached = false

    interface OnClick{
        fun onClickTitle(titleid: String)
        fun onShuffleClick(ptitleid: String)
    }

    override fun onClick(albumid: String) {
        listener.onClickTitle(albumid)
    }

    override fun onAttach(activity: Activity?) {
        super.onAttach(activity)
        listener = activity as OnClick
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
        = inflater.inflate(R.layout.fragment_album_details, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        attached = true
        fab_shuffle.setOnClickListener {
            listener.onShuffleClick(data.getItemAt(0).id)
        }
        MedialistsObservable.addObserver(this)
        setupRecyclerView(MedialistsObservable.getTitleList())
        setColorIfLineageTheme()
    }

    override fun update(o: Observable?, arg: Any?) {
        if(arg is MusicList){
            if(arg.getFlag() == MediaBrowser.MediaItem.FLAG_PLAYABLE && attached) {
                setupRecyclerView(arg)
            }
        }
    }

    private fun setupRecyclerView(list: MusicList){
        data = MusicList()
        var album = ""
        for(item in list){
            if(!item.isEmpty()){
                album = item.album
                item.cover = MusicMetadata.decodeAlbumcover(item.cover_uri, resources)
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
            activity?.actionBar?.title = album  // Observer ist schneller als fragment
        }
    }

    private fun setColorIfLineageTheme(){
        val color = ThemeChanger.getAccentColorLinage(activity)
        if(color != 0){
            fab_shuffle.backgroundTintList = ColorStateList.valueOf(color)
            fab_shuffle.backgroundTintMode = PorterDuff.Mode.SRC_ATOP
        }
    }

    override fun onStop() {
        super.onStop()
        attached = false
    }

    override fun onDestroy() {
        super.onDestroy()
        MedialistsObservable.deleteObserver(this)
    }

}