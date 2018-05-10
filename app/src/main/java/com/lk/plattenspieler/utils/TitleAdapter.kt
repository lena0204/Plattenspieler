package com.lk.plattenspieler.utils

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.lk.plattenspieler.R
import com.lk.plattenspieler.models.MusicList
import kotlinx.android.synthetic.main.row_album_details.view.*

/**
 * Created by Lena on 08.06.17.
 */
class TitleAdapter(private var dataset: MusicList, var cl: TitleAdapter.OnClickTitle) : RecyclerView.Adapter<TitleAdapter.ViewHolderTitle>() {

    // Interface, um mit dem Fragment zu kommunzieren
    interface OnClickTitle{
        fun onClick(albumid: String)
    }

    override fun onBindViewHolder(holder: ViewHolderTitle, position: Int) {
        val title = dataset.getItemAt(position)
        holder.getTvID().text = title.id
        holder.getTvTitle().text = title.title
        holder.getTvInterpret().text = title.artist
        // passendes Icon setzen
        holder.getIvCover().setImageBitmap(title.cover)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderTitle {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.row_album_details, parent, false)
        return ViewHolderTitle(v)
    }
    override fun getItemCount(): Int = dataset.countItems()

    // ViewHolder Klasse
    inner class ViewHolderTitle(v: View) : RecyclerView.ViewHolder(v) {

        private var tvId: TextView
        private var tvTitle: TextView
        private var tvInterpret: TextView
        private var ivCover: ImageView

        init{
            tvId = v.tv_title_id
            tvTitle = v.tv_title_title
            tvInterpret = v.tv_title_interpret
            ivCover = v.iv_title_cover
            v.setOnClickListener { cl.onClick(tvId.text.toString()) }
        }
        fun getTvID(): TextView = tvId
        fun getTvTitle(): TextView = tvTitle
        fun getIvCover(): ImageView = ivCover
        fun getTvInterpret(): TextView = tvInterpret

    }

}