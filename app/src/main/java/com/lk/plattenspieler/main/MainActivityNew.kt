package com.lk.plattenspieler.main

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.browse.MediaBrowser
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.*
import android.widget.Toast

import com.lk.plattenspieler.R
import com.lk.plattenspieler.fragments.*
import com.lk.plattenspieler.models.*
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
    private lateinit var sharedPreferences: SharedPreferences
    private var musicClient: MusicClient? = null
    private var albumsSet = false
    private var resumed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "oncreate")
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        design = ThemeChanger.readThemeFromPreferences(sharedPreferences)
        ThemeChanger.onActivityCreateSetTheme(this, design)
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
        fl_main_bar.visibility = View.GONE  // -> Farbe wird dynamisch gesetzt
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
        if(checkReadPermission()) {
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

    private fun checkReadPermission(): Boolean{
        val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
                /*,Manifest.permission.WRITE_EXTERNAL_STORAGE*/
        )
        return if(this.checkSelfPermission(permissions[0]) != PackageManager.PERMISSION_GRANTED
                /*&& this.checkSelfPermission(permissions[1]) != PackageManager.PERMISSION_GRANTED*/){
            this.requestPermissions(permissions, PERMISSION_REQUEST)
            false
        } else {
            true
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == PERMISSION_REQUEST){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                musicClient?.completeSetup(sharedPreferences.getBoolean(PREF_PLAYING, false))
            } else {
                Toast.makeText(this, R.string.toast_no_permission, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun showBar(){
        fl_main_bar.visibility = View.VISIBLE
    }

    fun hideBar(){
        fl_main_bar.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.v(TAG, "onDestroy")
        musicClient?.clear()
        musicClient = null
        PlaybackObservable.deleteObservers()
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
    override fun onShuffle(){
        musicClient?.shuffleTitles()
    }
    override fun onClickTitle(titleid: String) {
        musicClient?.playFromTitle(titleid)
    }
    override fun onShuffleClick(ptitleid: String) {
        // TESTING_ // -- shuffle scheint manchmal nicht alle Titel des Albums abzuspielen, beobachten
        musicClient?.shuffleAll()
    }
    override fun onSaveLyrics(lyrics: String) {
		Log.v(TAG, "Lyrics schreiben, noch nicht korrekt implementiert")
		//LyricsAccess.writeLyrics(lyrics, PlaybackObservable.getMetadata().path)
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
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        this.menu = menu
        val design = ThemeChanger.readThemeFromPreferences(sharedPreferences)
        // Designoptionene dynamisch je nach Design anpassen
        val optionDesign = when(design){
            EnumTheme.THEME_LIGHT, EnumTheme.THEME_DARK -> resources.getString(R.string.menu_teal)
            EnumTheme.THEME_LIGHT_T, EnumTheme.THEME_DARK_T, EnumTheme.THEME_LINEAGE -> resources.getString(R.string.menu_pink)
        }
        val lyrics = sharedPreferences.getInt(PREF_LYRICS, 0)
        val optionLyrics = if(lyrics == 0){
            resources.getString(R.string.menu_show_lyrics_no)
        } else {
            resources.getString(R.string.menu_show_lyrics)
        }
        menu?.findItem(R.id.menu_change_design)?.title = optionDesign
        menu?.findItem(R.id.menu_show_lyrics)?.title = optionLyrics
        if(!checkLineageSDK()){
            menu?.findItem(R.id.menu_theme_lineage)?.isVisible = false
        }
        return true
    }
    override fun onCreateOptionsMenu(pmenu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options_menu_main, pmenu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        musicClient?.menu(item.itemId)
        return super.onOptionsItemSelected(item)
    }
}
