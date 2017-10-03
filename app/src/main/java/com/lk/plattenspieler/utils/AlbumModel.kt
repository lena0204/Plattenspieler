package com.lk.plattenspieler.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * Created by Lena on 08.06.17.
 */

class AlbumModel (pid: String, ptitle: String, part: String, partist: String, ptracks: String){

    var id: String
    var title: String
    var albumart: String
    var songnumber: String
    var artist: String
    var cover: Bitmap

    init {
        id = pid
        title = ptitle
        albumart = part
        artist = partist
        songnumber = ptracks
        cover = BitmapFactory.decodeFile(albumart)
    }
}
