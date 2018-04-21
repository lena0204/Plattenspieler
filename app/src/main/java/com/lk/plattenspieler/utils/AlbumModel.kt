package com.lk.plattenspieler.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * Created by Lena on 08.06.17.
 */

data class AlbumModel (
		var id: String,
		var title: String,
		private var albumart: String,
		var artist: String,
		var songnumber: String){

	var cover: Bitmap = BitmapFactory.decodeFile(albumart)

}
