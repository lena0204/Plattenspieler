package com.lk.plattenspieler.fragments

import android.app.Fragment
import android.media.browse.MediaBrowser
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import com.lk.plattenspieler.R
import com.lk.plattenspieler.main.MainActivity
import com.lk.plattenspieler.utils.ThemeChanger
import com.lk.plattenspieler.utils.TitleAdapter
import com.lk.plattenspieler.utils.TitleModel
import kotlinx.android.synthetic.main.fragment_album_details.*

/**
* Erstellt von Lena am 08.06.17.
*/
class AlbumDetailsFragment(): Fragment(), TitleAdapter.onClickTitle {

    val TAG = "com.lk.pl-AlbumDetailsFragment"
    private lateinit var listener: onClick
    private var data = ArrayList<TitleModel>()
    private lateinit var fabShuffle: ImageButton

    constructor(act: AlbumDetailsFragment.onClick): this() {
        listener = act
    }
    // Interface und Listener zum Durchreichen bis zu Activity
    interface onClick{
        fun onClickTitle(titleid: String)
        fun onShuffleClick(ptitleid: String)
    }
    // Listener vom Adapter
    override fun onClick(albumid: String) {
        listener.onClickTitle(albumid)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_album_details, container, false)
        fabShuffle = v.findViewById(R.id.fab_shuffle) as ImageButton
        val design = PreferenceManager.getDefaultSharedPreferences(context).getInt(MainActivity.PREF_DESIGN, 0)
        if(design == ThemeChanger.THEME_LIGHT || design == ThemeChanger.THEME_DARK){
            fabShuffle.background = resources.getDrawable(R.drawable.fab_background_pink, activity.theme)
        } else {
            fabShuffle.background = resources.getDrawable(R.drawable.fab_background_teal, activity.theme)
        }
        return v
    }
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val args = this.arguments.getParcelableArrayList<MediaBrowser.MediaItem>("Liste")
        fabShuffle.setOnClickListener { listener.onShuffleClick(data[0].id) }
        setupRecyclerView(args)
    }

    private fun setupRecyclerView(list: ArrayList<MediaBrowser.MediaItem>){
        data = ArrayList()
        var album = ""
        for(item in list){
            val titleid = item.description.mediaId
            if(titleid != null && item.description.description != null){
                val title = item.description.title.toString()
                val titleinterpret = item.description.subtitle.toString()
                val titlearray = item.description.description.toString().split("__".toRegex())
                album = titlearray[0]
                val titlecover = titlearray[1]
                data.add(TitleModel(titleid, title, titleinterpret, titlecover))
            }
        }
        if(album.isNotEmpty()){
            this.activity.actionBar.title = album
        }
        recycler_album_details.layoutManager = LinearLayoutManager(activity)
        val aa = TitleAdapter(data, this)
        recycler_album_details.adapter = aa
    }

}