package com.lk.plattenspieler.main

import android.Manifest
import android.app.ActionBar
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.AudioManager
import android.media.browse.MediaBrowser
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.annotation.RequiresApi
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import com.lk.music_service_library.models.*

import com.lk.plattenspieler.R
import com.lk.plattenspieler.fragments.*
import com.lk.plattenspieler.utils.*
import java.util.*
import kotlinx.android.synthetic.main.activity_main.*

/**
 * Erstellt von Lena am 12.05.18.
 * Hauptklasse, verwaltet Menü, Observer, Berechtigungen und den MusicClient
 */
class MainActivityNew : Activity(),
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
    }

 	// IDEA_ -- Log in eine Datei schreiben für bessere Fehlersuche

    private val TAG = "com.lk.pl-MainActNew"
    private val PERMISSION_REQUEST = 8009
    private var design = EnumTheme.THEME_LIGHT
    private var menu: Menu? = null
    private var musicClient: MusicClient? = null
    private var albumsSet = false
    private var resumed = false

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "oncreate")
        changeDesign()
        setContentView(R.layout.activity_main)
        MedialistObservable.addObserver { o, arg ->
            when (arg) {
                is MusicList -> {
                    Log.v(TAG, "Update medialist")
                    if (arg.getFlag() == MediaBrowser.MediaItem.FLAG_BROWSABLE) {
                        albumsSet = true
                        showAlbums()
                    } else {
                        showTitles()
                    }
                }
                else -> Log.e(TAG, "ML: unknown observable update from " +
                        "${o?.javaClass?.canonicalName}: $arg")
            }
        }
        musicClient = MusicClient(this)
        // Bar und onClickListener setzen
        fl_main_bar.visibility = View.GONE
        fragmentManager.beginTransaction()
                .replace(R.id.fl_main_bar, MusicBarFragment(), "MusicBarFragment").commit()
        val pf = PlayingFragment()
        fl_main_bar.setOnClickListener {
            if(!pf.isVisible){
            fragmentManager.beginTransaction()
                    .addToBackStack(null)
                    .replace(R.id.fl_main_content, pf, "TAG_PLAYING")
                    .commit()
        } }
        // Audiotyp für die Lautstärkekontrolle auf Musik setzen
        this.volumeControlStream = AudioManager.STREAM_MUSIC
        // Permission abfragen, braucht die Berechtigung den Speicher zu lesen
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkReadPermission()) {
            musicClient?.completeSetup(sharedPreferences.getBoolean(PREF_PLAYING, false))
        } else if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            musicClient?.completeSetup(sharedPreferences.getBoolean(PREF_PLAYING, false))
        }
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        if(albumsSet){
            showAlbums()
        }
        Log.i(TAG, "onResume")
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        resumed = false
        Log.i(TAG, "onSavedIntancestate")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.v(TAG, "onDestroy")
        musicClient?.clear()
        musicClient = null
        PlaybackObservable.deleteObservers()
    }

    // ----------------- Permission Handling -----------------
    @RequiresApi(23)
    private fun checkWritePermission(): Boolean{
        val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return requestPermission(permissions, PERMISSION_REQUEST+1)
    }
    @RequiresApi(23)
    private fun checkReadPermission(): Boolean{
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        return requestPermission(permissions, PERMISSION_REQUEST)
    }
    @RequiresApi(23)
    fun requestDesignReadPermission(): Boolean{
        val permissions = arrayOf(lineageos.platform.Manifest.permission.CHANGE_STYLE)
        return requestPermission(permissions, PERMISSION_REQUEST + 2)
    }
    @RequiresApi(23)
    private fun requestPermission(perm: Array<String>, requestCode: Int): Boolean{
        return if(this.checkSelfPermission(perm[0]) != PackageManager.PERMISSION_GRANTED){
            this.requestPermissions(perm, requestCode)
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            PERMISSION_REQUEST -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    musicClient?.completeSetup(sharedPreferences.getBoolean(PREF_PLAYING, false))
                } else {
                    Toast.makeText(this, R.string.toast_no_permission, Toast.LENGTH_LONG).show()
                }
            }
            PERMISSION_REQUEST+1 -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    // weitermachen mit dialog anzeigen für lyrics
                } else {
                    Toast.makeText(this, R.string.toast_no_permission_write, Toast.LENGTH_LONG).show()
                }
            }
            PERMISSION_REQUEST+2 -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    musicClient?.applyTheme(EnumTheme.THEME_LINEAGE)
                } else {
                    Toast.makeText(this, R.string.toast_no_permission, Toast.LENGTH_LONG).show()
                    musicClient?.applyTheme(EnumTheme.THEME_LIGHT)
                }
            }
        }
    }

    // ------------------ Layout und Design ------------------
    fun showBar(){
        fl_main_bar.visibility = View.VISIBLE
    }
    fun hideBar(){
        fl_main_bar.visibility = View.GONE
    }

    private fun changeDesign(){
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        design = ThemeChanger.readThemeFromPreferences(sharedPreferences)
        ThemeChanger.onActivityCreateSetTheme(this, design)
        if(design == EnumTheme.THEME_LINEAGE) {
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M && requestDesignReadPermission())
                completeLineageDesign()
            else
                completeLineageDesign()
        }
    }
    private fun completeLineageDesign(){
        // Textfarbe der Actionbar ändern
        val tv = View.inflate(this, R.layout.action_bar_custom, null) as TextView
        val color = ThemeChanger.getAccentColorLinage(this)
        if (color != 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            tv.setTextColor(ColorStateList.valueOf(color))
            Log.d(TAG, "Farbe wurde geändert: " + Color.valueOf(color))
        }
        actionBar.customView = tv
        actionBar.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
    }

    private fun checkLineageSDK() : Boolean =
            lineageos.os.Build.LINEAGE_VERSION.SDK_INT >= lineageos.os.Build.LINEAGE_VERSION_CODES.ILAMA

    // ------------------ Observer -------------------
    override fun update(observable: Observable?, arg: Any?) { }
    private fun showAlbums(){
        //Log.i(TAG, "savestate: " + fragmentManager.isStateSaved)
        if(resumed) {
            Log.i(TAG, "Transaction")
            fragmentManager.beginTransaction()
                    .replace(R.id.fl_main_content, AlbumFragment())
                    .commit()
        }
    }
    private fun showTitles(){
        //Log.i(TAG, "savestate: " + fragmentManager.isStateSaved)
        if(resumed) {
            fragmentManager.beginTransaction()
                    .addToBackStack(null)
                    .replace(R.id.fl_main_content, AlbumDetailsFragment()).commit()
        }
    }

    // ---------------- Listener und zugehörige Methoden ----------
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

    // ------------- Menü -------------
    fun setDesignFromPref(design: EnumTheme){
        musicClient?.applyTheme(design)
    }
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
            // pref anzeigen und los mitgeben
            val pf = PrefFragment()
            val args = Bundle()
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.O && !checkLineageSDK()){
                args.putBoolean("LOS", true)
            } else {
                args.putBoolean("LOS", false)
            }
            pf.arguments = args
            fragmentManager.beginTransaction()
                    .addToBackStack(null)
                    .replace(R.id.fl_main_content, pf, "PrefFragment")
                    .commit()
        } else {
            musicClient?.menu(item.itemId)
        }
        return super.onOptionsItemSelected(item)
    }
}
