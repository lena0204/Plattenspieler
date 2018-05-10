package com.lk.plattenspieler.fragments

import android.app.Fragment
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import com.lk.plattenspieler.R
import com.lk.plattenspieler.main.MainActivity
import com.lk.plattenspieler.models.MusicList
import com.lk.plattenspieler.utils.ThemeChanger
import com.lk.plattenspieler.utils.TitleAdapter
import kotlinx.android.synthetic.main.fragment_album_details.*

/**
* Erstellt von Lena am 08.06.17.
*/
class AlbumDetailsFragment(): Fragment(), TitleAdapter.OnClickTitle {

    private val TAG = "com.lk.pl-AlbumDetailsF"
    private lateinit var listener: OnClick
    private var data = MusicList()
    private lateinit var fabShuffle: ImageButton

    constructor(act: AlbumDetailsFragment.OnClick): this() {
        listener = act
    }
    // Interface und Listener zum Durchreichen bis zu Activity
    interface OnClick{
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
        fabShuffle.setOnClickListener { listener.onShuffleClick(data.getItemAt(0).id) }
        setupRecyclerView(this.arguments.getParcelable("Liste"))
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
            this.activity.actionBar.title = album
        }
        recycler_album_details.layoutManager = LinearLayoutManager(activity)
        recycler_album_details.adapter = TitleAdapter(data, this)
    }

}