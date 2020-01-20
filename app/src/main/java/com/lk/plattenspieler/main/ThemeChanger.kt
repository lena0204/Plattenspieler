package com.lk.plattenspieler.main

import androidx.fragment.app.FragmentActivity
import android.content.SharedPreferences
import android.graphics.Color
import androidx.preference.PreferenceManager
import android.util.Log
import com.lk.plattenspieler.R
import com.lk.plattenspieler.utils.EnumTheme
import lineageos.style.StyleInterface

/**
 * Created by Lena on 08.09.17.
 * Verwaltet das Design der App (anwenden und abspeichern)
 */
object ThemeChanger {

    private const val TAG = "com.lk.pl-ThemeChanger"
    private const val BLACK_STYLE = 4
    // interne Codierung der ROM, da von Lineage nicht offiziell unterstützt

    fun setThemeAfterCreatingActivity(activity: FragmentActivity, iTheme: EnumTheme) {
        when (iTheme) {
            EnumTheme.THEME_LIGHT -> setLightPink(activity)
            EnumTheme.THEME_DARK -> setDarkPink(activity)
            EnumTheme.THEME_LIGHT_T -> setLightTeal(activity)
            EnumTheme.THEME_DARK_T -> setDarkTeal(activity)
            EnumTheme.THEME_LINEAGE -> setLineageTheme(activity)
        }
    }

    private fun setLightPink(activity: FragmentActivity){
        activity.setTheme(R.style.AppTheme)
        Log.d(TAG, "Changed to light theme")
    }

    private fun setDarkPink(activity: FragmentActivity){
        activity.setTheme(R.style.AppThemeDark)
        Log.d(TAG, "Changed to dark theme")
    }

    private fun setLightTeal(activity: FragmentActivity){
        activity.setTheme(R.style.AppThemeT)
        Log.d(TAG, "Changed to light theme teal")
    }

    private fun setDarkTeal(activity: FragmentActivity){
        activity.setTheme(R.style.AppThemeDarkT)
        Log.d(TAG, "Changed to dark theme teal")
    }

    private fun setLineageTheme(activity: FragmentActivity){
        val si = StyleInterface.getInstance(activity)
        Log.d(TAG, "Globalstyle: " + si.globalStyle)
        val style = si.globalStyle
        when(style){
            StyleInterface.STYLE_GLOBAL_DARK -> activity.setTheme(R.style.AppThemeDarkL)
            BLACK_STYLE -> activity.setTheme(R.style.AppThemeBlackL)
            else -> R.style.AppThemeL
        }
        // IDEA_ Daynight Theme mit implementieren (evtl eigene Zeiten dafür)
        Log.d(TAG, "Changed to lineage theme (daynight to light theme)")
    }

    fun getAccentColorLinage(activity: FragmentActivity?): Int {
        return if(themeIsLineage(activity)){
            obtainColorAccentAttribute(activity)
        } else {
            0
        }
    }

    private fun obtainColorAccentAttribute(activity: FragmentActivity?): Int{
        val colorAttribute = intArrayOf(android.R.attr.colorAccent)
        val typedArray = activity?.obtainStyledAttributes(android.R.style.Theme_DeviceDefault, colorAttribute)
        if(typedArray != null){
            return typedArray.getColor(0, Color.BLACK).also { typedArray.recycle() }
        }
        return 0
    }

    fun writeThemeToPreferences(sp: SharedPreferences, iTheme: EnumTheme){
        val theme = when(iTheme){
            EnumTheme.THEME_DARK -> 1
            EnumTheme.THEME_LIGHT_T -> 2
            EnumTheme.THEME_DARK_T -> 3
            EnumTheme.THEME_LINEAGE -> 4
            else -> 0
        }
        sp.edit().putInt(MainActivityNew.PREF_DESIGN, theme).apply()
    }

    private fun readThemeFromPreferences(activity: FragmentActivity?): EnumTheme =
            readThemeFromPreferences(PreferenceManager.getDefaultSharedPreferences(activity))

    fun readThemeFromPreferences(sp: SharedPreferences): EnumTheme {
        val iTheme = sp.getInt(MainActivityNew.PREF_DESIGN, 0)
        return when (iTheme){
            1 -> EnumTheme.THEME_DARK
            2 -> EnumTheme.THEME_LIGHT_T
            3 -> EnumTheme.THEME_DARK_T
            4 -> EnumTheme.THEME_LINEAGE
            else -> EnumTheme.THEME_LIGHT
        }
    }

    fun themeIsLineage(activity: FragmentActivity?): Boolean {
        val design = readThemeFromPreferences(activity)
        return design == EnumTheme.THEME_LINEAGE
    }
}