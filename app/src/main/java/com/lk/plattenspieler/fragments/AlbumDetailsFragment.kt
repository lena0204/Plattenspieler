package com.lk.plattenspieler.fragments

import android.app.Activity
import android.app.Fragment
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.browse.MediaBrowser
import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import com.lk.plattenspieler.R
import com.lk.plattenspieler.models.MedialistObservable
import com.lk.plattenspieler.models.MusicList
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
        MedialistObservable.addObserver(this)
        setupRecyclerView(MedialistObservable.getMediaList())
    }

    private fun setupRecyclerView(list: MusicList){
        data = MusicList()
        var album = ""
        for(item in list){
            if(!item.isEmpty()){
                album = item.album
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
            this.activity?.actionBar?.title = album  // Observer ist schneller als fragment
        }
        // TODO noch nicht so schön mit den Dividern
        val lmanager = LinearLayoutManager(activity)
        val divider = DividerItemDecoration(activity, lmanager.orientation)
        //recycler_album_details.addItemDecoration(divider)
        recycler_titles.layoutManager = lmanager
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