package com.lk.plattenspieler.fragments

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
import com.lk.plattenspieler.utils.AlbumAdapter
import com.lk.plattenspieler.utils.AlbumModel
import kotlinx.android.synthetic.main.fragment_album.*

/**
 * Created by Lena on 08.06.17.
 */
class AlbumFragment(): Fragment(), AlbumAdapter.Click {

    private val TAG = "com.lk.pl-AlbumFragment"
    // Listener und Interface, um onClick weiterzureichen
    private lateinit var listener: OnClick
    private var args = Bundle()

    constructor(f: AlbumFragment.OnClick): this(){
        listener = f
    }
    interface OnClick{
        fun onClickAlbum(albumid: String)
    }

    // Listener Methoden vom Adapter
    override fun onClick(albumid: String) {
        listener.onClickAlbum(albumid)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_album, container, false)
    }
    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity.actionBar.title = getString(R.string.app_name)
        args = this.arguments
        setupRecyclerView(args.getParcelableArrayList("Liste"))
    }

    private fun setupRecyclerView(list: ArrayList<MediaBrowser.MediaItem>){
        val data = ArrayList<AlbumModel>()
        for(item in list){
            var albumid = item.description.mediaId
            if(albumid != null && item.description.description != null){
                albumid = albumid.replace("ALBUM-", "")
                val albumtitle = item.description.title.toString()
                val albumartist = item.description.subtitle.toString()
                val albumarray = item.description.description.toString().split("__".toRegex())
                var albumart: Bitmap? = null
				albumart = BitmapFactory.decodeFile(albumarray[0])
				if (albumart == null){
					albumart = BitmapFactory.decodeResource(resources, R.mipmap.ic_no_cover)
				}
                val albumtracks = albumarray[1] + " " + getString(R.string.songs)
                data.add(AlbumModel(albumid, albumtitle, albumart as Bitmap, albumartist, albumtracks))
            }
        }
        recycler_album.layoutManager = LinearLayoutManager(activity)
        val aa = AlbumAdapter(data, this)
        recycler_album.adapter = aa
    }

}