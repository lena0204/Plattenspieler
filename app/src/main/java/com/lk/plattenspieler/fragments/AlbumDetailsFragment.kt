package com.lk.plattenspieler.fragments

import android.app.Activity
import android.app.Fragment
import android.content.res.ColorStateList
import android.graphics.*
import android.media.browse.MediaBrowser
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import android.widget.TextView
import com.lk.music_service_library.observables.MedialistsObservable
import com.lk.music_service_library.models.MusicList
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

    // Interface und Listener zum Durchreichen bis zu Activity
    interface OnClick{
        fun onClickTitle(titleid: String)
        fun onShuffleClick(ptitleid: String)
    }
    // Listener vom Adapter
    override fun onClick(albumid: String) {
        listener.onClickTitle(albumid)
    }

    override fun onAttach(activity: Activity?) {
        super.onAttach(activity)
        listener = activity as OnClick
    }
    override fun onStop() {
        super.onStop()
        attached = false
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
        // Farbe setzen falls lineage
        val color = ThemeChanger.getAccentColorLinage(activity)
        if(color != 0){
            fab_shuffle.backgroundTintList = ColorStateList.valueOf(color)
            fab_shuffle.backgroundTintMode = PorterDuff.Mode.SRC_ATOP
        }
    }

    private fun setupRecyclerView(list: MusicList){
        data = MusicList()
        var album = ""
        for(item in list){
            if(!item.isEmpty()){
                album = item.album
                // Cover umwandeln
                var titlecover: Bitmap?
				titlecover = BitmapFactory.decodeFile(item.cover_uri)
				if(titlecover == null){
					titlecover = BitmapFactory.decodeResource(resources, R.mipmap.ic_no_cover)
				}
                item.cover = titlecover
                data.addItem(item)
            }
        }
        if(album.isNotEmpty()){
            if(ThemeChanger.themeIsLineage(activity) && activity?.actionBar?.customView != null)
                (activity?.actionBar?.customView as TextView).text = album
            activity?.actionBar?.title = album  // Observer ist schneller als fragment
        }
        recycler_titles.layoutManager = LinearLayoutManager(activity)
        recycler_titles.adapter = TitleAdapter(data, this)
    }

    override fun update(o: Observable?, arg: Any?) {
        if(arg is MusicList){
            if(arg.getFlag() == MediaBrowser.MediaItem.FLAG_PLAYABLE && attached) {
                setupRecyclerView(arg)
            }
        }
    }

}