package com.lk.plattenspieler.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * Created by Lena on 08.06.17.
 */
data class TitleModel(
        var id: String,
		var title: String,
		var interpret: String,
		var cover: Bitmap)