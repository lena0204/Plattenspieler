package com.lk.plattenspieler.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * Created by Lena on 08.06.17.
 */
class TitleModel(pid:String, ptitle: String, pinterpret: String, part: String) {

    var id: String
    var title: String
    var interpret: String
    var titleart: String
    var cover: Bitmap

    init {
        id = pid
        title = ptitle
        interpret = pinterpret
        titleart = part
        cover = BitmapFactory.decodeFile(titleart)
    }

}