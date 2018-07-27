package com.lk.plattenspieler.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import android.util.Log
import com.lk.plattenspieler.R
import com.lk.plattenspieler.main.MainActivityNew
import com.lk.plattenspieler.utils.EnumTheme

/**
 * Erstellt von Lena am 27.07.18.
 */
class PrefFragment: PreferenceFragment() {

    private lateinit var prefListener: SharedPreferences.OnSharedPreferenceChangeListener
    private lateinit var mActivity: MainActivityNew
    private var losSupport = false

    private val PREF_LOS = "PREF_THEMELOS"
    private val PREF_COLOR = "PREF_THEMECOLOR"
    private val PREF_DARK = "PREF_THEMELIGHT"
    private val TAG = "PrefFragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.fragment_pref)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mActivity = activity as MainActivityNew
        val sp = PreferenceManager.getDefaultSharedPreferences(mActivity)
        losSupport = arguments.getBoolean("LOS", false)
        setEnabled(sp)
        prefListener = getPrefListener()
    }

    override fun onResume() {
        super.onResume()
        this.preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(prefListener)
    }

    override fun onPause() {
        super.onPause()
        this.preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(prefListener)
    }

    private fun getPrefListener(): SharedPreferences.OnSharedPreferenceChangeListener =
            SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                when(key){
                    PREF_COLOR, PREF_DARK, PREF_LOS -> {
                        val los = sharedPreferences.getBoolean(PREF_LOS, false)
                        var theme = EnumTheme.THEME_LIGHT
                        if(los){
                            // Theme changer aufrufen
                            theme = EnumTheme.THEME_LINEAGE
                        } else {
                            val teal = sharedPreferences.getBoolean(PREF_COLOR, false)
                            val dark = sharedPreferences.getBoolean(PREF_DARK, false)
                            if(dark){
                                theme = if(teal)
                                    EnumTheme.THEME_DARK_T
                                else
                                    EnumTheme.THEME_DARK
                            } else {
                                if(teal)
                                    theme = EnumTheme.THEME_LIGHT_T
                            }
                        }
                        mActivity.setDesignFromPref(theme)
                        setEnabled(sharedPreferences)
                    }
                }
            }

    private fun setEnabled(sp: SharedPreferences){
        if(losSupport) {
            val los = sp.getBoolean(PREF_LOS, false)
            if (los) {
                findPreference(PREF_COLOR).isEnabled = false
                findPreference(PREF_DARK).isEnabled = false
            } else {
                findPreference(PREF_COLOR).isEnabled = true
                findPreference(PREF_DARK).isEnabled = true
            }
        } else {
            findPreference(PREF_LOS).isEnabled = false
        }
    }
}