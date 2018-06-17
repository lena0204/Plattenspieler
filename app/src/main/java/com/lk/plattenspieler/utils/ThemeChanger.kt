package com.lk.plattenspieler.utils

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Color
import android.support.v4.app.FragmentActivity
import android.util.Log
import com.lk.plattenspieler.R
import com.lk.plattenspieler.main.MainActivityNew

/**
 * Created by Lena on 08.09.17.
 * Verwaltet das Design der App (anwenden und abspeichern)
 */
object ThemeChanger{

    private const val TAG = "com.lk.pl-ThemeChanger"

    // Theme ändern beim Start der Activity nach den Vorgaben und Textfarben passend setzen
    fun onActivityCreateSetTheme(activity: Activity, iTheme: EnumTheme) {
        when (iTheme) {
            EnumTheme.THEME_LIGHT -> {
                activity.setTheme(R.style.AppTheme)
                Log.d(TAG, "Changed to light theme")
            }
            EnumTheme.THEME_DARK -> {
                activity.setTheme(R.style.AppThemeDark)
                Log.d(TAG, "Changed to dark theme")
            }
            EnumTheme.THEME_LIGHT_T -> {
                activity.setTheme(R.style.AppThemeT)
                Log.d(TAG, "Changed to light theme teal")
            }
            EnumTheme.THEME_DARK_T -> {
                activity.setTheme(R.style.AppThemeDarkT)
                Log.d(TAG, "Changed to dark theme teal")
            }
            EnumTheme.THEME_LINEAGE -> {
                activity.setTheme(R.style.AppThemeLineage)
                Log.d(TAG, "Changed to lineage theme")
            }
        }
    }

    fun getAccentColor(iTheme: EnumTheme, act: Activity): Int = when (iTheme) {
            EnumTheme.THEME_LIGHT, EnumTheme.THEME_DARK -> R.color.colorAccent
            EnumTheme.THEME_LIGHT_T, EnumTheme.THEME_DARK_T -> R.color.colorAccent_t
            EnumTheme.THEME_LINEAGE -> 0
    }

    fun writeThemeToPreferences(sp: SharedPreferences, iTheme: EnumTheme){
        val theme: Int = when(iTheme){
            EnumTheme.THEME_DARK -> 1
            EnumTheme.THEME_LIGHT_T -> 2
            EnumTheme.THEME_DARK_T -> 3
            EnumTheme.THEME_LINEAGE -> 4
            else -> 0
        }
        sp.edit().putInt(MainActivityNew.PREF_DESIGN, theme).apply()
    }

    fun readThemeFromPreferences(sp: SharedPreferences): EnumTheme{
        val iTheme = sp.getInt(MainActivityNew.PREF_DESIGN, 0)
        return when (iTheme){
            1 -> EnumTheme.THEME_DARK
            2 -> EnumTheme.THEME_LIGHT_T
            3 -> EnumTheme.THEME_DARK_T
            4 -> EnumTheme.THEME_LINEAGE
            else -> EnumTheme.THEME_LIGHT
        }
    }
}