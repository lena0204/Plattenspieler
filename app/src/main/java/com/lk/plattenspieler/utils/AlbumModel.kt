package com.lk.plattenspieler.utils

import android.graphics.Bitmap

/**
 * Created by Lena on 08.06.17.
 */

data class AlbumModel (
		var id: String,
		var title: String,
		var albumart: Bitmap,
		var artist: String,
		var songnumber: String)
