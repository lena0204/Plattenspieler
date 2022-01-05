package com.lk.plattenspieler.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lk.plattenspieler.R
import com.lk.musicservicelibrary.models.MusicList
import kotlinx.android.synthetic.main.row_music_data.view.*

/**
 * Created by Lena on 08.06.17.
 * Adapter für den RecyclerView, der die Titeldaten korrekt darstellt
 */
class TitleAdapter(private var dataset: MusicList, var cl: TitleAdapter.OnClickTitle) : RecyclerView.Adapter<TitleAdapter.ViewHolderTitle>() {

    interface OnClickTitle{
        fun onClick(titleId: String)
    }

    override fun onBindViewHolder(holder: ViewHolderTitle, position: Int) {
        val title = dataset.getItemAt(position)
        holder.getTvID().text = title.id
        holder.getTvTitle().text = title.title
        holder.getTvInterpret().text = title.artist
        holder.getIvCover().setImageBitmap(title.cover)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderTitle {
        // val v = LayoutInflater.from(parent.context).inflate(R.layout.row_music_data_simple, parent, false)
        val v = LayoutInflater.from(parent.context).inflate(R.layout.row_music_data, parent, false)
        return ViewHolderTitle(v)
    }

    override fun getItemCount(): Int = dataset.size()

    inner class ViewHolderTitle(v: View) : RecyclerView.ViewHolder(v) {

        private var tvId: TextView = v.tv_music_id
        private var tvTitle: TextView = v.tv_music_title
        private var tvInterpret: TextView = v.tv_music_info
        private var ivCover: ImageView = v.iv_music_cover

        init{
            v.setOnClickListener { cl.onClick(tvId.text.toString()) }
        }
        fun getTvID(): TextView = tvId
        fun getTvTitle(): TextView = tvTitle
        fun getIvCover(): ImageView = ivCover
        fun getTvInterpret(): TextView = tvInterpret

    }

}