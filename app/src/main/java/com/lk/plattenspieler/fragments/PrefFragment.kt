package com.lk.plattenspieler.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.preference.*
import com.lk.plattenspieler.R
import com.lk.plattenspieler.main.MainActivityNew
import com.lk.plattenspieler.utils.EnumTheme

/**
 * Erstellt von Lena am 27.07.18.
 */
class PrefFragment: PreferenceFragmentCompat() {

    private lateinit var prefListener: SharedPreferences.OnSharedPreferenceChangeListener
    private lateinit var mActivity: MainActivityNew
    private var losSupport = false

    private val PREF_LOS = "PREF_THEMELOS"
    private val PREF_COLOR = "PREF_THEMECOLOR"
    private val PREF_DARK = "PREF_THEMELIGHT"
    private val TAG = "PrefFragment"

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.fragment_pref)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mActivity = activity as MainActivityNew
        losSupport = if(arguments != null) {
            arguments?.getBoolean("LOS", false) as Boolean
        } else {
            false
        }
        Log.d(TAG, "losSupport: $losSupport")
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity)
        enableValidPreferences(sharedPreferences)
        prefListener = getPrefListener()
    }

    private fun enableValidPreferences(sp: SharedPreferences){
       /* when {
            losSupport && isLosThemeEnabled(sp) -> setStandardThemeEnabled(false)
            losSupport && !isLosThemeEnabled(sp) -> setStandardThemeEnabled(true)
            !losSupport -> {
                findPreference<SwitchPreference>(PREF_LOS)?.isEnabled = false
                findPreference<SwitchPreference>(PREF_LOS)?.setSummary(R.string.pref_themelos_unsupported)
            }
        }*/
    }

    private fun isLosThemeEnabled(sp: SharedPreferences): Boolean = sp.getBoolean(PREF_LOS, false)

    private fun setStandardThemeEnabled(enabled: Boolean){
        findPreference<SwitchPreference>(PREF_COLOR)?.isEnabled = enabled
        findPreference<SwitchPreference>(PREF_DARK)?.isEnabled = enabled
    }

    private fun getPrefListener(): SharedPreferences.OnSharedPreferenceChangeListener =
            SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                when(key){
                    PREF_COLOR, PREF_DARK, PREF_LOS -> {
                        changeTheme(sharedPreferences)
                        enableValidPreferences(sharedPreferences)
                    }
                }
            }

    private fun changeTheme(sharedPreferences: SharedPreferences){
        val teal = sharedPreferences.getBoolean(PREF_COLOR, false)
        val dark = sharedPreferences.getBoolean(PREF_DARK, false)
        val theme = when {
            !teal && dark -> EnumTheme.THEME_DARK
            teal && !dark -> EnumTheme.THEME_LIGHT_T
            teal && dark -> EnumTheme.THEME_DARK_T
            else -> EnumTheme.THEME_LIGHT
        }
        mActivity.setDesignFromPref(theme)
    }

    override fun onResume() {
        super.onResume()
        this.preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(prefListener)
    }

    override fun onPause() {
        super.onPause()
        this.preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(prefListener)
    }
}