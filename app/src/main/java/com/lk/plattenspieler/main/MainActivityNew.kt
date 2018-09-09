package com.lk.plattenspieler.main

import android.app.ActionBar
import android.content.*
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.media.AudioManager
import android.media.browse.MediaBrowser
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentActivity
import com.lk.plattenspieler.observables.MedialistsObservable
import com.lk.musicservicelibrary.models.MusicList

import com.lk.plattenspieler.R
import com.lk.plattenspieler.fragments.*
import com.lk.plattenspieler.observables.PlaybackObservable
import com.lk.plattenspieler.utils.*
import java.util.*
import kotlinx.android.synthetic.main.activity_main.*

/**
 * Erstellt von Lena am 12.05.18.
 * Hauptklasse, verwaltet Menü, Observer, Berechtigungen und den MusicClient
 */
class MainActivityNew : FragmentActivity(),
        Observer,
        AlbumFragment.OnClick,
        AlbumDetailsFragment.OnClick,
		LyricsAddingDialog.OnSaveLyrics,
        MusicBarFragment.OnClick{

    companion object {
        const val PREF_PLAYING = "playing"
        const val PREF_DESIGN = "design"
        const val PREF_SHUFFLE = "shuffle"
        const val PREF_LYRICS = "lyrics"

        fun isVersionGreaterThan(versionCode: Int): Boolean
            = Build.VERSION.SDK_INT > versionCode
    }

    private val TAG = "com.lk.pl-MainActNew"
    private var design = EnumTheme.THEME_LIGHT
    private var menu: Menu? = null
    private var musicClient: MusicClient? = null
    private var albumsSet = false
    private var resumed = false

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var permissionRequester: PermissionRequester

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
        MedialistsObservable.addObserver(this)
        PlaybackObservable.addObserver(this)
        musicClient = MusicClient(this)
        this.volumeControlStream = AudioManager.STREAM_MUSIC
        setupMusicBar()
    }

    private fun setupMusicBar(){
        hideBar()
        supportFragmentManager.beginTransaction()
                .replace(R.id.fl_main_bar, MusicBarFragment(), "MusicBarFragment")
                .commit()
        val pf = PlayingFragment()
        fl_main_bar.setOnClickListener { view ->
            if(!pf.isVisible){
                supportFragmentManager.beginTransaction()
                        .addToBackStack(null)
                        .replace(R.id.fl_main_content, pf, "TAG_PLAYING")
                        .commit()
            }
        }
    }

    fun showBar(){
        fl_main_bar.visibility = View.VISIBLE
    }

    fun hideBar(){
        fl_main_bar.visibility = View.GONE
    }

    private fun finishSetupIfPermissionGranted(){
        val hasQueueSaved = sharedPreferences.getBoolean(PREF_PLAYING, false)
        if(isVersionGreaterThan(Build.VERSION_CODES.M) && permissionRequester.checkReadPermission()) {
            musicClient?.completeSetup(hasQueueSaved)
        } else if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            musicClient?.completeSetup(hasQueueSaved)
        }
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
        musicClient?.clear()
        musicClient = null
        PlaybackObservable.deleteObserver(this)
    }

    // Permissions
    @RequiresApi(23)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            PermissionRequester.PERMISSION_REQUEST_READ -> {
                if(isPermissionGranted(grantResults))
                    musicClient?.completeSetup(sharedPreferences.getBoolean(PREF_PLAYING, false))
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
                    musicClient?.applyTheme(EnumTheme.THEME_LINEAGE)
                } else {
                    showToast(R.string.toast_no_permission_design)
                    musicClient?.applyTheme(EnumTheme.THEME_LIGHT)
                }
            }
        }
    }

    private fun isPermissionGranted(grantResults: IntArray): Boolean
            = (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)

    private fun showToast(stringResource: Int){
        Toast.makeText(this, stringResource, Toast.LENGTH_LONG).show()
    }

    // Observable
    override fun update(observable: Observable?, arg: Any?) {
        when {
            observable is MedialistsObservable && arg is MusicList -> {
                if (arg.getFlag() == MediaBrowser.MediaItem.FLAG_BROWSABLE) {
                    albumsSet = true
                    showAlbums()
                } else {
                    showTitles()
                }
            }
            arg is PlaybackState -> {
                if(arg.state == PlaybackState.STATE_STOPPED)
                    hideBar()
            }
        }
    }

    private fun showAlbums(){
        if(resumed) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.fl_main_content, AlbumFragment())
                    .commit()
        }
    }

    private fun showTitles(){
        if(resumed) {
            supportFragmentManager.beginTransaction()
                    .addToBackStack(null)
                    .replace(R.id.fl_main_content, AlbumDetailsFragment())
                    .commit()
        }
    }

    // TODO anderes Handling für die Listener (Observable?)
    override fun onClickAlbum(albumid: String) {
        musicClient?.showAlbumTitles(albumid)
    }
    override fun onClickTitle(titleid: String) {
        musicClient?.playFromTitle(titleid)
    }
    override fun onShuffleClick(ptitleid: String) {
        musicClient?.shuffleTitles()
    }
    override fun onSaveLyrics(lyrics: String) {
		Log.v(TAG, "Lyrics schreiben, noch nicht korrekt implementiert")
		LyricsAccess.writeLyrics(lyrics, PlaybackObservable.getMetadata().path)
	}
    override fun onClickPlay() {
        musicClient?.play()
    }
    override fun onClickNext() {
        musicClient?.next()
    }
    override fun onClickPrevious() {
        musicClient?.previous()
    }

    fun setDesignFromPref(design: EnumTheme){
        musicClient?.applyTheme(design)
    }

    // ------------- Menü -------------
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        this.menu = menu
        // TODO Lyrics hinzufügen nur anzeigen wenn PlayingFragment sichtbar ist
        //menu?.findItem(R.id.menu_add_lyrics)?.isVisible = false
        return true
    }

    override fun onCreateOptionsMenu(pmenu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options_menu_main, pmenu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.menu_settings){
            val pf = PrefFragment()
            pf.arguments = prepareBundleForPrefFragment()
            supportFragmentManager.beginTransaction()
                    .addToBackStack(null)
                    .replace(R.id.fl_main_content, pf, "PrefFragment")
                    .commit()
        } else {
            musicClient?.menu(item.itemId)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun prepareBundleForPrefFragment(): Bundle {
        val args = Bundle()
        if(isVersionGreaterThan(Build.VERSION_CODES.O) && checkLineageSDK()){
            args.putBoolean("LOS", true)
        } else {
            args.putBoolean("LOS", false)
        }
        return args
    }

    private fun checkLineageSDK() : Boolean =
            lineageos.os.Build.LINEAGE_VERSION.SDK_INT >= lineageos.os.Build.LINEAGE_VERSION_CODES.ILAMA
}
