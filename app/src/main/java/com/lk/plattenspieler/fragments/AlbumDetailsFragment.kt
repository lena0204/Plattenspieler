package com.lk.plattenspieler.fragments

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.*
import android.media.browse.MediaBrowser
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.lk.plattenspieler.R
import com.lk.plattenspieler.models.MedialistObservable
import com.lk.plattenspieler.models.MusicList
import com.lk.plattenspieler.utils.*
import kotlinx.android.synthetic.main.fragment_album_details.*
import kotlinx.android.synthetic.main.fragment_album_details.view.*
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_album_details, container, false)
        val design = ThemeChanger.readThemeFromPreferences(PreferenceManager.getDefaultSharedPreferences(context))
        v.fab_shuffle.background = resources.getDrawable(R.drawable.fab_background_pink, activity?.theme)
        if(design == EnumTheme.THEME_LINEAGE){
            // bei Lineage Theme den Akzent manuell drüber legen, weil dynamische Erkennung nicht funktionierts
            val attr = intArrayOf(android.R.attr.colorAccent)
            val typedArray = activity?.obtainStyledAttributes(android.R.style.Theme_DeviceDefault, attr)
            val color = typedArray?.getColor(0, Color.BLACK)
            typedArray?.recycle()
            if(color != null) {
                v.fab_shuffle.backgroundTintList = ColorStateList.valueOf(color)
                v.fab_shuffle.backgroundTintMode = PorterDuff.Mode.SRC_ATOP
            }
        }
        return v
    }

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
        recycler_album_details.layoutManager = LinearLayoutManager(activity)
        recycler_album_details.adapter = TitleAdapter(data, this)
    }

    override fun update(o: Observable?, arg: Any?) {
        if(arg is MusicList){
            if(arg.getFlag() == MediaBrowser.MediaItem.FLAG_PLAYABLE) {
                if(attached) {
                    setupRecyclerView(arg)
                }
            }
        }
    }

}