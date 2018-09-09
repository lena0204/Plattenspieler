package com.lk.plattenspieler.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build

/**
 * Erstellt von Lena am 18.08.18.
 */

class PermissionRequester(private val activityNew: MainActivityNew) {

    companion object {
        const val PERMISSION_REQUEST_READ = 8009
        const val PERMISSION_REQUEST_WRITE = 8010
        const val PERMISSION_REQUEST_DESIGN = 8011
    }

    fun checkReadPermission(): Boolean{
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        return requestPermission(permissions, PERMISSION_REQUEST_READ)
    }
    fun checkWritePermission(): Boolean{
        val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return requestPermission(permissions, PERMISSION_REQUEST_WRITE)
    }

    fun requestDesignReadPermission(): Boolean{
        val permissions = arrayOf(lineageos.platform.Manifest.permission.CHANGE_STYLE)
        return requestPermission(permissions, PERMISSION_REQUEST_DESIGN)
    }

    private fun requestPermission(perm: Array<String>, requestCode: Int): Boolean{
        return if(hasToRequest(perm[0])){
            activityNew.requestPermissions(perm, requestCode)
            false
        } else {
            true
        }
    }

    private fun hasToRequest(permission: String): Boolean =
            MainActivityNew.isVersionGreaterThan(Build.VERSION_CODES.M)
                    && activityNew.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED

}