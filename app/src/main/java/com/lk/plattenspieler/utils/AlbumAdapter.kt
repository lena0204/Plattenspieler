package com.lk.plattenspieler.utils

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.lk.plattenspieler.R
import com.lk.plattenspieler.models.MusicList
import kotlinx.android.synthetic.main.row_album.view.*

/**
 * Created by Lena on 08.06.17.
 * Adapter für den RecyclerView, um die Albumdaten korrekt darzustellen
 */
class AlbumAdapter(private var dataset: MusicList, val cl: Click) : RecyclerView.Adapter<AlbumAdapter.ViewHolder>(){

    val TAG = "AlbumAdapter"

    // Interface, um mit dem Fragment zu kommunzieren
    interface Click{
        fun onClick(albumid: String)
    }

    // ViewHolder Methoden
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val album = dataset.getItemAt(position)
        holder.getTvID().text = album.id
        holder.getTvAlbum().text = album.album
        val interpret = album.artist + " | " + album.alltracks
        holder.getTvArtist().text = interpret
        // passendes Icon setzen
        holder.getIvCover().setImageBitmap(album.cover)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.row_album, parent, false)
        return ViewHolder(v)
    }
    override fun getItemCount(): Int = dataset.countItems()

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