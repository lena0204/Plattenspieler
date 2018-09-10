package com.lk.plattenspieler.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lk.musicservicelibrary.models.MusicList
import com.lk.plattenspieler.R
import kotlinx.android.synthetic.main.row_music_data.view.*

/**
 * Created by Lena on 08.06.17.
 * Adapter für den RecyclerView, um die Albumdaten korrekt darzustellen
 */
class AlbumAdapter(private var dataset: MusicList, val cl: Click)
            : RecyclerView.Adapter<AlbumAdapter.ViewHolder>(){

    interface Click{
        fun onClick(albumid: String)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val album = dataset.getItemAt(position)
        holder.getTvID().text = album.id
        holder.getTvAlbum().text = album.album
        holder.getTvArtist().text = "${album.artist} | ${album.alltracks}"
        holder.getIvCover().setImageBitmap(album.cover)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.row_music_data, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int = dataset.countItems()


    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {

        private var tvId: TextView = v.tv_music_id
        private var tvAlbum: TextView = v.tv_music_title
        private var tvArtist: TextView = v.tv_music_info
        private var ivCover: ImageView = v.iv_music_cover

        init{
            v.setOnClickListener {
                cl.onClick(tvId.text.toString())
            }
        }
        fun getTvID(): TextView = tvId
        fun getTvAlbum(): TextView = tvAlbum
        fun getTvArtist(): TextView = tvArtist
        fun getIvCover(): ImageView = ivCover

    }

}