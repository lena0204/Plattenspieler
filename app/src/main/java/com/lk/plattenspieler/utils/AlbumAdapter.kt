package com.lk.plattenspieler.utils

import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.lk.plattenspieler.R
import kotlinx.android.synthetic.main.row_album.view.*

/**
 * Created by Lena on 08.06.17.
 */
class AlbumAdapter(private var dataset: ArrayList<AlbumModel>, val cl: Click) : RecyclerView.Adapter<AlbumAdapter.ViewHolder>(){

    val TAG = "AlbumAdapter"

    // Interface, um mit dem Fragment zu kommunzieren
    interface Click{
        fun onClick(albumid: String)
    }

    // ViewHolder Methoden
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val album = dataset[position]
        holder.getTvID().text = album.id
        holder.getTvAlbum().text = album.title
        val interpret = album.artist + " | " + album.songnumber
        holder.getTvArtist().text = interpret
        // passendes Icon setzen
        holder.getIvCover().setImageBitmap(album.cover)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.row_album, parent, false)
        return ViewHolder(v)
    }
    override fun getItemCount(): Int = dataset.size

    // ViewHolder Klasse
    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {

        private var tvId: TextView
        private var tvAlbum: TextView
        private var tvArtist: TextView
        private var ivCover: ImageView

        init{
            tvId = v.tv_album_id
            tvAlbum = v.tv_album_title
            tvArtist = v.tv_album_artist
            ivCover = v.iv_album_cover
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