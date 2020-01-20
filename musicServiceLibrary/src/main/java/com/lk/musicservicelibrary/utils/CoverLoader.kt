package com.lk.musicservicelibrary.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.Size
import com.lk.musicservicelibrary.R

/**
 * Erstellt von Lena am 2019-12-21.
 */
object CoverLoader {

    private val TAG = "CoverLoader"

    private fun decodeAlbumCover(resources: Resources, coverUri: String): Bitmap {
        var albumArt: Bitmap?
        albumArt = BitmapFactory.decodeFile(coverUri)
        if (albumArt == null) {
            albumArt = decodeFallbackCover(resources)
        }
        return albumArt
    }

    fun decodeAlbumCover(context: Context, contentUri: Uri, coverUri: String): Bitmap {
        return decodeAlbumCover(context, contentUri, coverUri, 120)
    }

    fun decodeAlbumCover(context: Context, contentUri: Uri, coverUri: String, size: Int): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                context.contentResolver.loadThumbnail(contentUri, Size(size, size), null)
            } catch (e: Exception) {
                // Log.w(TAG, "Failed to load cover from content resolver")
                decodeFallbackCover(context.resources)
            }
        } else {
            Log.d(TAG, "Decode old way")
            decodeAlbumCover(context.resources, coverUri)
        }
    }

    private fun decodeFallbackCover(resources: Resources): Bitmap {
        return BitmapFactory.decodeResource(resources, R.mipmap.ic_no_cover)
    }
}