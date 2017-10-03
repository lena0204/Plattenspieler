package com.lk.plattenspieler.fragments

import android.app.Fragment
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import com.lk.plattenspieler.R
import com.lk.plattenspieler.utils.TitleAdapter
import com.lk.plattenspieler.utils.TitleModel
import kotlinx.android.synthetic.main.fragment_album_details.*

/**
 * Created by Lena on 08.06.17.
 */
class AlbumDetailsFragment(): Fragment(), TitleAdapter.onClickTitle {

    lateinit var listener: onClick
    var data = ArrayList<TitleModel>()
    lateinit var fab_shuffle: ImageButton

    constructor(act: AlbumDetailsFragment.onClick): this(){
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
        fab_shuffle = v.findViewById(R.id.fab_shuffle) as ImageButton
        return v
    }
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val args = this.arguments.getParcelableArrayList<MediaBrowserCompat.MediaItem>("Liste")
        fab_shuffle.setOnClickListener { listener.onShuffleClick(data[0].id) }
        setupRecyclerView(args)
    }

    private fun setupRecyclerView(list: ArrayList<MediaBrowserCompat.MediaItem>){
        data = ArrayList<TitleModel>()
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
        if(album.isNotEmpty()) this.activity.actionBar.title = album
        recycler_album_details.layoutManager = LinearLayoutManager(activity)
        val aa = TitleAdapter(data, this)
        recycler_album_details.adapter = aa
    }

}