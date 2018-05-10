package com.lk.plattenspieler.fragments

import android.app.Fragment
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.lk.plattenspieler.R
import com.lk.plattenspieler.utils.AlbumAdapter
import com.lk.plattenspieler.models.MusicList
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
        setupRecyclerView(args.getParcelable("Liste"))
    }

    private fun setupRecyclerView(list: MusicList){
        val data = MusicList()
        for(item in list){
            if(!item.isEmpty()){
                item.id = item.id.replace("ALBUM-", "")
                var albumart: Bitmap?
				albumart = BitmapFactory.decodeFile(item.cover_uri)
				if (albumart == null){
					albumart = BitmapFactory.decodeResource(resources, R.mipmap.ic_no_cover)
				}
                item.cover = albumart
                item.alltracks = item.num_tracks.toString() + " " + resources.getString(R.string.songs)
                data.addItem(item)
            }
        }
        recycler_album.layoutManager = LinearLayoutManager(activity)
        recycler_album.adapter = AlbumAdapter(data, this)
    }

}