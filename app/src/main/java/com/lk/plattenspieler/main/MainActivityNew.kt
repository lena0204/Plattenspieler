package com.lk.plattenspieler.main

import android.app.ActionBar
import android.content.*
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.media.AudioManager
import android.media.browse.MediaBrowser
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.transaction
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.lk.musicservicelibrary.models.MusicList

import com.lk.plattenspieler.R
import com.lk.plattenspieler.fragments.*
import com.lk.plattenspieler.musicbrowser.*
import com.lk.plattenspieler.observables.*
import com.lk.plattenspieler.utils.*
import kotlinx.android.synthetic.main.activity_main.*

/**
 * Erstellt von Lena am 12.05.18.
 * Hauptklasse, verwaltet Menü, Observer, Berechtigungen und den MusicClient
 */
class MainActivityNew : FragmentActivity(), Observer<MusicList>, LyricsAddingDialog.OnSaveLyrics {

    // TODO Funktionen durchtesten, refactoring nötig

    companion object {
        const val PREF_PLAYING = "playing"
        const val PREF_DESIGN = "design"
        const val PREF_SHUFFLE = "shuffle"
        // not used currently -- const val PREF_LYRICS = "lyrics"
        fun isVersionGreaterThan(versionCode: Int): Boolean
            = Build.VERSION.SDK_INT > versionCode
    }

    private val TAG = "com.lk.pl-MainActNew"
    private var design = EnumTheme.THEME_LIGHT
    private var menu: Menu? = null
    private var controllerAccess: ControllerAccess? = null
    private var albumsSet = false
    private var resumed = false

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var permissionRequester: PermissionRequester
    private lateinit var mediaViewModel: MediaViewModel
    private lateinit var playbackViewModel: PlaybackViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionRequester = PermissionRequester(this)
        changeDesign()
        setContentView(R.layout.activity_main)
        setupForMusicHandling()
        finishSetupIfPermissionGranted()
    }

    private fun changeDesign(){
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        design = ThemeChanger.readThemeFromPreferences(sharedPreferences)
        ThemeChanger.setThemeAfterCreatingActivity(this, design)
        if(canMakeAdaptionsToLineageDesign(design)) {
            changeActionbarTitleColor()
        }
    }

    private fun canMakeAdaptionsToLineageDesign(design: EnumTheme): Boolean
            = (design == EnumTheme.THEME_LINEAGE
            && isVersionGreaterThan(Build.VERSION_CODES.M)
            && permissionRequester.requestDesignReadPermission())

    private fun changeActionbarTitleColor(){
        val tv = View.inflate(this, R.layout.action_bar_custom, null) as TextView
        val color = ThemeChanger.getAccentColorLinage(this)
        if (color != 0) {
            tv.setTextColor(ColorStateList.valueOf(color))
        }
        actionBar?.customView = tv
        actionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
    }

    private fun setupForMusicHandling(){
        mediaViewModel = ViewModelProviders.of(this).get(MediaViewModel::class.java)
        mediaViewModel.setObserversToAll(this, this)
        playbackViewModel = ViewModelProviders.of(this).get(PlaybackViewModel::class.java)
        controllerAccess = ControllerAccess(this)
        this.volumeControlStream = AudioManager.STREAM_MUSIC
        setupMusicBar()
    }

    private fun setupMusicBar(){
        hideBar()
        supportFragmentManager.transaction {
            replace(R.id.fl_main_bar, MusicBarFragment(), "MusicBarFragment")
        }
        val pf = PlayingFragment()
        fl_main_bar.setOnClickListener { _ ->
            if(!pf.isVisible){
                supportFragmentManager.transaction {
                    addToBackStack(null)
                    replace(R.id.fl_main_content, pf, "TAG_PLAYING")
                }
            }
        }
    }

    fun showBar() { fl_main_bar.visibility = View.VISIBLE }

    fun hideBar() { fl_main_bar.visibility = View.GONE }

    private fun finishSetupIfPermissionGranted(){
        if(isVersionGreaterThan(Build.VERSION_CODES.M) && permissionRequester.checkReadPermission()) {
            controllerAccess?.completeSetup()
        } else if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            controllerAccess?.completeSetup()
        }
    }

    override fun onChanged(liste: MusicList?){
        when(liste?.getFlag()){
            MediaBrowser.MediaItem.FLAG_BROWSABLE -> showAlbums()
            MediaBrowser.MediaItem.FLAG_PLAYABLE -> showTitles()
        }
    }

    private fun showAlbums(){
        // if(resumed) {
        supportFragmentManager.transaction { replace(R.id.fl_main_content, AlbumFragment()) }
        // }
    }

    private fun showTitles(){
        // if(resumed) {
        supportFragmentManager.transaction {
            addToBackStack(null)
            replace(R.id.fl_main_content, AlbumDetailsFragment())
        }
        // }
    }

    override fun onSaveLyrics(lyrics: String) {
        Log.v(TAG, "Lyrics schreiben, noch nicht korrekt implementiert")
        LyricsAccess.writeLyrics(lyrics, playbackViewModel.metadata.value!!.path)
    }

    fun setDesignFromPref(design: EnumTheme){
        controllerAccess?.applyTheme(design)
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        if(albumsSet)
            showAlbums()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        resumed = false
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy")
        controllerAccess?.clear()
        controllerAccess = null
    }

    // Permissions
    @RequiresApi(23)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            PermissionRequester.PERMISSION_REQUEST_READ -> {
                if(isPermissionGranted(grantResults))
                    controllerAccess?.completeSetup()
                else
                    showToast(R.string.toast_no_permission)
            }
            PermissionRequester.PERMISSION_REQUEST_WRITE -> {
                if(isPermissionGranted(grantResults))
                    // TODO weitermachen mit dialog anzeigen für lyrics
                else
                    showToast(R.string.toast_no_permission_write)
            }
            PermissionRequester.PERMISSION_REQUEST_DESIGN -> {
                if(isPermissionGranted(grantResults)){
                    controllerAccess?.applyTheme(EnumTheme.THEME_LINEAGE)
                } else {
                    showToast(R.string.toast_no_permission_design)
                    controllerAccess?.applyTheme(EnumTheme.THEME_LIGHT)
                }
            }
        }
    }

    private fun isPermissionGranted(grantResults: IntArray): Boolean
            = (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)

    private fun showToast(stringResource: Int) {
        Toast.makeText(this, stringResource, Toast.LENGTH_LONG).show()
    }

    // ------------- Menü -------------
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        this.menu = menu
        // TODO Lyrics hinzufügen nur anzeigen wenn PlayingFragment sichtbar ist
        menu?.findItem(R.id.menu_add_lyrics)?.isVisible = false
        return true
    }

    override fun onCreateOptionsMenu(pmenu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options_menu_main, pmenu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.menu_settings -> launchPreferences()
            R.id.menu_shuffle_all -> {
                val action = ControllerAction(EnumActions.SHUFFLE_ALL)
                playbackViewModel.controllerAction.value = action
            }
            R.id.menu_remove_playing -> {
                val action = ControllerAction(EnumActions.STOP)
                playbackViewModel.controllerAction.value = action
            }
            R.id.menu_add_lyrics -> controllerAccess?.addLyrics()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun launchPreferences(){
        val pf = PrefFragment()
        pf.arguments = prepareBundleForPrefFragment()
        supportFragmentManager.transaction {
            addToBackStack(null)
            replace(R.id.fl_main_content, pf, "PrefFragment")
        }
    }

    private fun prepareBundleForPrefFragment(): Bundle {
        val losSupport = isVersionGreaterThan(Build.VERSION_CODES.O) && checkLineageSDK()
        return bundleOf("LOS" to losSupport)
    }

    private fun checkLineageSDK() : Boolean =
            lineageos.os.Build.LINEAGE_VERSION.SDK_INT >= lineageos.os.Build.LINEAGE_VERSION_CODES.ILAMA
}
