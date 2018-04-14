package com.lk.plattenspieler.utils

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.lk.plattenspieler.R

/**
 * Created by Lena on 08.09.17.
 */
class ThemeChanger{

    companion object {
        val THEME_LIGHT = 0
        val THEME_DARK = 1
        val THEME_LIGHT_T = 2
        val THEME_DARK_T = 3
    }
    private val TAG = "ThemeChanger"

    // ändert das Theme, indem das Theme übergeben wird und die Activity neugestartet wird
    // wobei dabei die zweite Methode ausgeführt wird
    fun changeToTheme(activity: Activity) {
        activity.finish()
        activity.startActivity(Intent(activity, activity.javaClass))
    }

    // Theme ändern beim Start der Activity nach den Vorgaben und Textfarben passend setzen
    fun onActivityCreateSetTheme(activity: Activity, iTheme: Int) {
        when (iTheme) {
            THEME_LIGHT -> {
                activity.setTheme(R.style.AppTheme)
                Log.d(TAG, "Changed to light theme")
            }
            THEME_DARK -> {
                activity.setTheme(R.style.AppThemeDark)
                Log.d(TAG, "Changed to dark theme")
            }
            THEME_LIGHT_T -> {
                activity.setTheme(R.style.AppThemeT)
                Log.d(TAG, "Changed to light theme teal")
            }
            THEME_DARK_T -> {
                activity.setTheme(R.style.AppThemeDarkT)
                Log.d(TAG, "Changed to dark theme teal")
            }
        }
    }

    fun getAccentColor(iTheme: Int): Int = when (iTheme) {
            THEME_LIGHT, THEME_DARK -> R.color.colorAccent
            THEME_LIGHT_T, THEME_DARK_T -> R.color.colorAccent_t
            else -> R.color.colorAccent
    }


}