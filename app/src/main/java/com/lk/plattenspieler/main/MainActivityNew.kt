package com.lk.plattenspieler.main

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.media.AudioManager
import android.media.browse.MediaBrowser
import android.media.session.*
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast

import com.lk.plattenspieler.R
import com.lk.plattenspieler.fragments.*
import com.lk.plattenspieler.models.*
import com.lk.plattenspieler.utils.*
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.util.*

/**
 * Erstellt von Lena am 12.05.18.
 * Hauptklasse, verwaltet Menü, Observer, Berechtigungen und den MusicClient
 */
class MainActivityNew : AppCompatActivity(),
        Observer,
        AlbumFragment.OnClick,
        AlbumDetailsFragment.OnClick,
		LyricsAddingDialog.OnSaveLyrics {

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
        // Observer und MusicClient
        PlaybackObservable.addObserver { o, arg ->
            when (arg) {
                is MusicMetadata -> {
                    if(!arg.isEmpty()) {
                        Log.v(TAG, "PB: Update Metadata mit Titel: " + arg.title)
                        tv_main_title.text = arg.title
                        tv_main_interpret.text = arg.artist
                    } else {
                        tv_main_title.text = resources.getString(R.string.no_title)
                        tv_main_interpret.text = ""
                    }
                }
                is MusicList -> {
                    if(arg.getFlag() == 0){
                        Log.v(TAG, "PB: Update queue")
                    }
                }
                is MusicPlaybackState -> {
                    Log.v(TAG, "PB: update playbackstate")
                    updatePlaybackState(arg)
                }
                else -> Log.e(TAG, "PB: unknown observable update from " +
                        "${o?.javaClass?.canonicalName}: ${arg}")
            }
        }
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
        // onClickListener für die Wiedergabe und Views initiallisieren
        val pf = PlayingFragment()
        rl_playing.setOnClickListener {
            if(!pf.isVisible){
                supportFragmentManager.beginTransaction()
                        .addToBackStack(null)
                        .replace(R.id.frame_layout, pf, "TAG_PLAYING")
                        .commit()
            }
        }
        // Listener
        ib_main_play.setOnClickListener { musicClient?.play() }
        ib_main_next.setOnClickListener { musicClient?.next() }
        ib_main_previous.setOnClickListener { musicClient?.previous() }
        // Audiotyp für die Lautstärkekontrolle auf Musik setzen
        this.volumeControlStream = AudioManager.STREAM_MUSIC
        // Permission abfragen, braucht die Berechtigung den Speicher zu lesen
        if(checkReadPermission()) {
            setupUI()
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

    private fun setupUI(){
        // Aktuelle Farben setzen
        tv_main_title.text = resources.getString(R.string.no_title)
        tv_main_interpret.text = ""
        updatePlaybackState(MusicPlaybackState())  // default ->  (false, stopped)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.v(TAG, "onDestroy")
        musicClient?.clear()
        musicClient = null
        PlaybackObservable.deleteObservers()
    }

    private fun checkLineageSDK() : Boolean{
        try {
            val process = Runtime.getRuntime().exec("getprop ro.lineage.build.version.plat.sdk")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line = reader.readLine()
            while (line != null) {
                output.append(line)
                line = reader.readLine()
            }
            val result = output.toString()
            Log.d(TAG, "Lineage SDK Version: " + result.toInt())
            if(result.toInt()==9){
                return true
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }

    // ------------------ Observer -------------------
    override fun update(observable: Observable?, arg: Any?) { }
    private fun updatePlaybackState(state: MusicPlaybackState){
        if (state.state == PlaybackState.STATE_PLAYING) {
            ib_main_play.background = resources.getDrawable(R.mipmap.ic_pause, theme)
        } else {
            ib_main_play.background = resources.getDrawable(R.mipmap.ic_play, theme)
        }
        if (design == EnumTheme.THEME_LIGHT || design == EnumTheme.THEME_LIGHT_T) {
            ib_main_play.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.black, theme))
            ib_main_play.backgroundTintMode = PorterDuff.Mode.SRC_ATOP
            ib_main_next.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.black, theme))
            ib_main_next.backgroundTintMode = PorterDuff.Mode.SRC_ATOP
            ib_main_previous.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.black, theme))
            ib_main_previous.backgroundTintMode = PorterDuff.Mode.SRC_ATOP
        }
    }
    private fun showAlbums(){
        //Log.i(TAG, "savestate: " + fragmentManager.isStateSaved)
        if(resumed) {
            Log.i(TAG, "Transaction")
            supportFragmentManager.beginTransaction()
                    .replace(R.id.frame_layout, AlbumFragment())
                    .commit()
        }
    }
    private fun showTitles(){
        //Log.i(TAG, "savestate: " + fragmentManager.isStateSaved)
        if(resumed) {
            supportFragmentManager.beginTransaction()
                    .addToBackStack(null)
                    .replace(R.id.frame_layout, AlbumDetailsFragment()).commit()
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
        // TESTING_ // -- shuffle scheint manchmal nicht alle Titel des Albums abzuspielen, beobachten
        musicClient?.shuffleTitles()
    }
    override fun onSaveLyrics(lyrics: String) {
		Log.v(TAG, "Lyrics schreiben, noch nicht korrekt implementiert")
		//LyricsAccess.writeLyrics(lyrics, PlaybackObservable.getMetadata().path)
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
