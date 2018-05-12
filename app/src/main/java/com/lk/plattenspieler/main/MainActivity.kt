package com.lk.plattenspieler.main

import android.Manifest
import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.*
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.browse.MediaBrowser
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.FragmentActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast

import com.lk.plattenspieler.R
import com.lk.plattenspieler.background.MusicService
import com.lk.plattenspieler.database.*
import com.lk.plattenspieler.fragments.*
import com.lk.plattenspieler.models.*
import com.lk.plattenspieler.utils.ThemeChanger
import kotlinx.android.synthetic.main.activity_main.*
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.mp3.MP3File
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.mp4.Mp4FieldKey
import org.jaudiotagger.tag.mp4.Mp4Tag
import java.io.File
import java.util.*

class MainActivity : FragmentActivity(),
        Observer,
        AlbumFragment.OnClick,
        AlbumDetailsFragment.OnClick,
		LyricsAddingDialog.OnSaveLyrics {

    companion object {
        const val PREF_PLAYING = "playing"
        const val PREF_DESIGN = "design"
        const val PREF_SHUFFLE = "shuffle"
    }

 	// IDEA_ -- Log in eine Datei schreiben für bessere Fehlersuche

    private val TAG = "com.lk.pl-MainActivity"
    private val PERMISSION_REQUEST = 8009
    private val connectionCallback = BrowserConnectionCallback(this)
    val controllerCallback = MusicControllerCallback()
    val subscriptionCallback = MusicSubscriptionCallback()
    private var medialist = MusicList()
    private var playingQueue = MusicList()
    private var design = ThemeChanger.THEME_LIGHT      // 0 -> hell, 1 -> dunkel
    private var menu: Menu? = null
    private var shuffleOn = false

    lateinit var mbrowser: MediaBrowser
    private lateinit var tvTitle: TextView
    private lateinit var tvInterpret: TextView
    private lateinit var ibState: ImageButton
    private lateinit var updateInterface: CallbackPlaying
    private lateinit var metadata: MusicMetadata
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var queueModel: QueueViewModel

    // Interface zur Kommunikation mit der Wiedergabeansicht
    interface CallbackPlaying{
        fun updateMetadata(data: MusicMetadata, queue: String)
        fun updateShuffleMode(mode: Boolean)
    }

    // ------------ Create MainActivity --------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PlaybackObservable.addObserver(this)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        design = PreferenceManager.getDefaultSharedPreferences(this).getInt(PREF_DESIGN, 0)
        ThemeChanger.onActivityCreateSetTheme(this, design)
        setContentView(R.layout.activity_main)
        // ViewModel
        queueModel = ViewModelProviders.of(this).get(QueueViewModel::class.java)
        // onClickListener für die Wiedergabe und Views initiallisieren
        val pf = PlayingFragment()
        updateInterface = pf
        rl_playing.setOnClickListener {
            // Bundle mit den Wiedergabedaten erstellen
            if(!pf.isVisible){
                val args = prepareBundle()
                if(args != null)
                {
                    pf.arguments = args
                    // Fragment ersetzen
                    supportFragmentManager.beginTransaction()
                            .addToBackStack(null)
                            .replace(R.id.frame_layout, pf, "TAG_PLAYING")
                            .commit()
                }
            }

        }
        tvTitle = this.findViewById(R.id.tv_main_title) as TextView
        tvInterpret = this.findViewById(R.id.tv_main_interpret) as TextView
        ibState = this.findViewById(R.id.ib_main_play) as ImageButton
        // Audiotyp für die Lautstärkekontrolle auf Musik setzen
        this.volumeControlStream = AudioManager.STREAM_MUSIC
        // Permission abfragen, braucht die Berechtigung den Speicher zu lesen
        if(checkReadPermission()) {
            completeSetup()
        }
    }

    private fun prepareBundle(): Bundle?{
        val args = Bundle()
        val meta = mediaController.metadata
        if(meta != null && meta.getString(MediaMetadata.METADATA_KEY_MEDIA_ID) != null) {
            /*queueModel.getQueue().observe(this, android.arch.lifecycle.Observer {
                list -> Log.d(TAG, "Länge: " + list?.countItems())
            })*/
            // TESTING_ NullPointerEx nach Wiedergabe löschen?
            val queue = mediaController.queue
            var items = ""
            var i = 0
            // IDEA_ Auslagern
            if (queue != null && queue.size > 0) {
                while (i < queue.size && i < 30) {    // Länge der Queue auf 30 begrenzen
                    val array = queue[i].description.description.split("__")
                    items = items + array[2] + "\n - " + queue[i].description.subtitle + "__"
                    i++
                }
                items = items.substring(0, items.length - 2)
                Log.d(TAG, "Anzahl Queueitems prepareBundle() $i")
            }
            Log.d(TAG, "PrepareBundle, shuffle ist $shuffleOn")
            args.putString("Q", items)
            args.putParcelable("T", MusicMetadata.createFromMediaMetadata(meta))
            args.putInt("S", mediaController.playbackState.state)
            args.putBoolean("shuffle", shuffleOn)
            return args
        }
        return null
    }
    private fun completeSetup(){
        // Setup MediaBrowser
        val c = ComponentName(applicationContext, MusicService::class.java)
        mbrowser = MediaBrowser(this, c, connectionCallback, null)
        mbrowser.connect()
    }
    // IDEA_ Auslagern
    fun setupUI(){
        // Listener
        ib_main_play.setOnClickListener { onClickPlay() }
        ib_main_next.setOnClickListener { onClickNext() }
        ib_main_previous.setOnClickListener { onClickPrevious() }
        val mc = mediaController
        // update Bar mit aktuellen Infos
        if(mc.playbackState.state == PlaybackState.STATE_PLAYING){
            ibState.background = resources.getDrawable(R.mipmap.ic_pause, theme)
        } else {
            ibState.background = resources.getDrawable(R.mipmap.ic_play, theme)
        }
        if(design == ThemeChanger.THEME_LIGHT || design == ThemeChanger.THEME_LIGHT_T){
            ibState.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.black, theme))
            ibState.backgroundTintMode = PorterDuff.Mode.SRC_ATOP
            ib_main_next.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.black, theme))
            ib_main_next.backgroundTintMode = PorterDuff.Mode.SRC_ATOP
            ib_main_previous.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.black, theme))
            ib_main_previous.backgroundTintMode = PorterDuff.Mode.SRC_ATOP
        }
        //Log.d(TAG, "SetupUI Metadaten .${mc.metadata.description.title}.")
        if(mc.metadata.description.mediaId != null) {
            tvTitle.text = mc.metadata.description.title
            tvInterpret.text = mc.metadata.description.subtitle
        } else {
            tvTitle.text = resources.getString(R.string.no_title)
            tvInterpret.text = ""
        }
        // register Callback
        mc.registerCallback(controllerCallback)
        if(mc.playbackState.extras?.getBoolean("shuffle") != null){
            shuffleOn = mc.playbackState.extras.getBoolean("shuffle")
        }
    }
    private fun checkReadPermission(): Boolean{
        return if(this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            this.requestPermissions(Array(1, { _ -> Manifest.permission.READ_EXTERNAL_STORAGE}), PERMISSION_REQUEST)
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == PERMISSION_REQUEST){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                completeSetup()
            } else {
                Toast.makeText(this, R.string.toast_no_permission, Toast.LENGTH_LONG).show()
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        // Alte Dateien löschen und die aktuelle Wiedergabe in die Datenbank schreiben
        savePlayingQueue()
        // Callbacks deaktivieren
        if(mediaController != null){
            //Log.d(TAG, "mediacontroller noch aktiv")
            mediaController.unregisterCallback(controllerCallback)
        }
        mbrowser.disconnect()
    }

    override fun update(o: Observable?, arg: Any?) {
        when(arg){
            is MusicMetadata -> {
                Log.d(TAG, "Update Metadata")
            }
            is MusicList -> {
                Log.d(TAG, "Update queue")
            }
            is MusicPlaybackState -> {
                Log.d(TAG, "update playbackstate")
            }
            else -> Log.d(TAG, "unknown observable update: ${arg.toString()}")
        }
    }


    // ---------------- Listener und zugehörige Methoden ----------
    override fun onClickAlbum(albumid: String) {
        val mediaid = "ALBUM-$albumid"
        mbrowser.subscribe(mediaid, subscriptionCallback)
    }
    // IDEA_ Auslagern
    override fun onClickTitle(titleid: String) {
        val mc = mediaController
        var args = Bundle()
        args.putInt("I", 1)
        mc.transportControls.playFromMediaId(titleid, args)
        // Stelle in der Medialiste suchen und alle ab da an die Queue anhängen
        var indexInMedialist = -1
        for(item in medialist){
            if(item.id == titleid){
                indexInMedialist = medialist.indexOf(item)
                break
            }
        }
        if(indexInMedialist != -1){
            var i = indexInMedialist + 1
            while(i < medialist.countItems()){
				args = Bundle()
                args.putParcelable("S", medialist.getItemAt(i))
                mc.sendCommand("add", args, null)
                i++
            }
        }
        shuffleOn = false
        this.updateInterface.updateShuffleMode(shuffleOn)
    }
	// TESTING_ // -- shuffle scheint manchmal nicht alle Titel des Albums abzuspielen, beobachten
    // IDEA_ Auslagern (Shuffle aus Liederliste erstellen -> Zugriff vom Service möglich?)
    override fun onShuffleClick(ptitleid: String) {
        var titleid = ptitleid
        val mc = mediaController
        // Alles Items in listSongs kopieren, damit keine Operationen auf medialist stattfinden
        val listSongs = MusicList()
        for(item in medialist){
            listSongs.addItem(item)
        }
        val random = Random()
        // ERSTEN Titel zufällig auswählen und ABSPIELEN, ABER NICHT anhängen, weil sich das sonst doppelt
        var i = random.nextInt(listSongs.countItems())
        if(!listSongs.getItemAt(i).id.isEmpty()){
            titleid = listSongs.getItemAt(i).id
        }
        var args = Bundle()
        args.putInt("I", 1)
        mc.transportControls.playFromMediaId(titleid, args)
        listSongs.removeItemAt(i)
        // zufällige Liste erstellen und an die QUEUE hängen, ersten Titel aus der Queue abspielen
        while(!listSongs.isEmpty()){
            i = random.nextInt(listSongs.countItems())
			args = Bundle()
            args.putParcelable("S", listSongs.getItemAt(i))
            mc.sendCommand("add", args, null)
            listSongs.removeItemAt(i)
        }
        mc.sendCommand("shuffle", null, null)
        shuffleOn = true
        this.updateInterface.updateShuffleMode(shuffleOn)
    }
	// IDEA_ Auslagern
    override fun onSaveLyrics(lyrics: String) {
		Log.d(TAG, "Lyrics schreiben")
		val datapath = metadata.path
		if(datapath != ""){
			Log.d(TAG, datapath)
			if(datapath.contains("mp3")){
				// Mp3 Datei
				val mp3File = AudioFileIO.read(File(datapath)) as MP3File
				if(mp3File.hasID3v2Tag()) {
					mp3File.iD3v2TagAsv24.setField(FieldKey.LYRICS, lyrics)
				} else {
					Log.d(TAG, "Kein ID3v2 Tag vorhanden, keine Lyrics geschrieben.")
				}
				AudioFileIO.write(mp3File)
			} else {
				// m4a Datei
				val m4aTag = AudioFileIO.read(File(datapath)).tag as Mp4Tag
				m4aTag.setField(Mp4FieldKey.LYRICS, lyrics)
			}
		}
	}

    private fun onClickPlay() {
        val mc = mediaController
        if(mc.playbackState.state == PlaybackState.STATE_PLAYING){
            mc.transportControls.pause()
        } else {
            mc.transportControls.play()
        }
    }
    private fun onClickNext() {
        mediaController.transportControls.skipToNext()
    }
    private fun onClickPrevious(){
        mediaController.transportControls.skipToPrevious()
    }

    // ---------------- Callback-Methoden -----------------
    fun showAlbums(){
        val af = AlbumFragment(this)
        val extras = Bundle()
        extras.putParcelable("Liste", medialist)
        af.arguments = extras
        fragmentManager.beginTransaction().replace(R.id.frame_layout, af).commit()
    }
    fun showTitles(){
        val adf = AlbumDetailsFragment(this)
        val extras = Bundle()
        extras.putParcelable("Liste", medialist)
        Log.d(TAG, "showTitles")
        adf.arguments = extras
        fragmentManager.beginTransaction()
                .addToBackStack(null)
                .replace(R.id.frame_layout, adf).commit()
    }
    fun setData(pdata: MusicMetadata, queue: MusicList){
        // TODO -- ordentlich Aufräumen nachdem die Wiedergabeliste gelöscht wurde -> Sinn?
        var items = ""
        var i = 0
        if(queue.countItems() > 0) {
            while (i < queue.countItems() && i < 30) {		// Länge der Liste auf 30 begrenzen
                items = items + queue.getItemAt(i).title + "\n - " + queue.getItemAt(i).artist + "__"
                i++
            }
			//Log.d(TAG, "Länge Queue setData() $i")
            items = items.substring(0, items.length - 2)
        }
		if(pdata.id != "") {
            queueModel.setQueue(queue)
			updateInterface.updateMetadata(pdata, items)
			//Log.d(TAG, "setData Metadaten: .${pdata.description.mediaId}.")
			metadata = pdata
			tvTitle.text = pdata.title
			tvInterpret.text = pdata.artist
		} else {
			// falls keine Wiedergabe stattfindet oder die Wiedergabe zu Ende / beendet wurde
			Log.d(TAG, "pdata = null IS" + (pdata.isEmpty()) + ", mediaId = null IS " + (pdata.id))
			tvTitle.text = resources.getString(R.string.no_title)
			tvInterpret.text = ""
		}
    }

    // ------------- Menü -------------
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        this.menu = menu
        val design = sharedPreferences.getInt(PREF_DESIGN, 0)
        var optionDesign = ""
        // Designoptionene dynamisch je nach Design anpassen
        when(design){
            ThemeChanger.THEME_LIGHT, ThemeChanger.THEME_DARK ->
                optionDesign = resources.getString(R.string.menu_teal)
            ThemeChanger.THEME_LIGHT_T, ThemeChanger.THEME_DARK_T ->
                optionDesign = resources.getString(R.string.menu_pink)
        }
        menu?.findItem(R.id.menu_change_design)?.title = optionDesign
        return true
    }
    override fun onCreateOptionsMenu(pmenu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options_menu_main, pmenu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.menu_change_design -> changeDesign()
            R.id.menu_remove_playing -> stopAndRemovePlaying()
            R.id.menu_dark_light -> changeLightDark()
            R.id.menu_shuffle_all -> shuffleAll()
        /*R.id.menu_add_lyrics -> addLyrics()*/
        /*R.id.menu_delete_database -> {
            // DEBUGGING: Alte Dateien löschen und die aktuelle Wiedergabe in die Datenbank schreiben
            val number = contentResolver.delete(SongContentProvider.CONTENT_URI, null, null)
            Log.d(TAG, "Anzahl der gelöschten Zeilen: " + number)
        }*/
        }
        return super.onOptionsItemSelected(item)
    }

    private fun changeDesign(){
        var design = sharedPreferences.getInt(PREF_DESIGN, 0)
        Log.d(TAG, "Farbe: $design")
        when(design){
            ThemeChanger.THEME_LIGHT -> design = ThemeChanger.THEME_LIGHT_T
            ThemeChanger.THEME_DARK -> design = ThemeChanger.THEME_DARK_T
            ThemeChanger.THEME_LIGHT_T -> design = ThemeChanger.THEME_LIGHT
            ThemeChanger.THEME_DARK_T -> design = ThemeChanger.THEME_DARK
        }
        sharedPreferences.edit().putInt(PREF_DESIGN, design).apply()
        savePlayingQueue()  // zur sicherheit falls vorher noch eine andere Liste gespeichert ist
        ThemeChanger.changeToTheme(this)
    }
    private fun changeLightDark(){
        var design = sharedPreferences.getInt(PREF_DESIGN, 0)
        Log.d(TAG, "Hell/Dunkel: $design")
        when(design){
            ThemeChanger.THEME_LIGHT -> design = ThemeChanger.THEME_DARK
            ThemeChanger.THEME_DARK -> design = ThemeChanger.THEME_LIGHT
            ThemeChanger.THEME_LIGHT_T -> design = ThemeChanger.THEME_DARK_T
            ThemeChanger.THEME_DARK_T -> design = ThemeChanger.THEME_LIGHT_T
        }
        sharedPreferences.edit().putInt(PREF_DESIGN, design).apply()
        savePlayingQueue()  // zur sicherheit falls vorher noch eine andere Liste gespeichert ist
        ThemeChanger.changeToTheme(this)
    }
    private fun stopAndRemovePlaying(){
        // Wiedergabe stoppen, falls nötig; löscht automatisch die Queue und den Player etc
        val mc = mediaController
        if(mc != null){
            if(mc.playbackState.state != PlaybackState.STATE_STOPPED){
                mc.transportControls.stop()
            }
        }
        // Datenbank löschen
        contentResolver.delete(SongContentProvider.CONTENT_URI, null, null)
        sharedPreferences.edit().putBoolean(PREF_PLAYING, false).apply()
    }
	private fun shuffleAll(){
		mediaController.sendCommand("addAll", null, null)
		shuffleOn = true
		this.updateInterface.updateShuffleMode(shuffleOn)
	}
	private fun addLyrics(){
		val dialog = LyricsAddingDialog()
		dialog.show(fragmentManager, "LyricsAddingDialog")
	}

    // ------------------ Datenbank ------------------
    // PROBLEM_ hat Schlange nach Pausieren und dann Neustart nicht gespeichert (oder mind. nicht wiederhergestellt)
    private fun savePlayingQueue(){
        if(mediaController.playbackState.state != PlaybackState.STATE_PLAYING){
            SongDBAccess.savePlayingQueue(contentResolver, playingQueue, metadata)
        }
        sharedPreferences.edit().putBoolean(MainActivity.PREF_PLAYING, true)
                .putBoolean(MainActivity.PREF_SHUFFLE,shuffleOn).apply()
    }
    fun restorePlayingQueue(){
        Log.d(TAG, "Einstellung Queue vorhanden: " + sharedPreferences.getBoolean(PREF_PLAYING, false))
        /* Datenbank im Background handeln? 300 Songs brauchen doch etwas Zeit -> von außen sichtbar -> JA!
           IDEA_ die Queue an sich im Hintergrund wiederherstellen -> dauert je nach Länge etwas
        */
        if(sharedPreferences.getBoolean(PREF_PLAYING, false)){
            val queue = SongDBAccess.restorePlayingQueue(contentResolver, mediaController)
            if(queue == null){
                Log.d(TAG, "Cursor ist null oder leer")
                sharedPreferences.edit().putBoolean(PREF_PLAYING, false).apply()
            } else {
                val music = queue.removeItemAt(0)
                setData(music, queue)
            }
            // shuffle auslesen
            shuffleOn = sharedPreferences.getBoolean(PREF_SHUFFLE,false)
            if(shuffleOn){
                mediaController.sendCommand("shuffle", null, null)
            }
        }
    }

    // ---------------- Callbacks vom MusicService und PlayingFragment -----------
    inner class BrowserConnectionCallback(private val activity: Activity): MediaBrowser.ConnectionCallback(){

        override fun onConnected() {
            val token = mbrowser.sessionToken
            val musicController = MediaController(activity, token)
            mediaController = musicController
            // Daten abfragen
            mbrowser.subscribe(mbrowser.root, subscriptionCallback)
            // Playlist wiederherstellen und UI Kontrolle herstellen
            restorePlayingQueue()
            setupUI()
        }

        override fun onConnectionFailed() {
            Log.e(TAG, "Connection to service failed")
        }

        override fun onConnectionSuspended() {
            if(activity.mediaController != null){
                activity.mediaController.unregisterCallback(controllerCallback)
                activity.mediaController = null
            }
        }
    }
    inner class MusicSubscriptionCallback: MediaBrowser.SubscriptionCallback(){

        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowser.MediaItem>) {
            medialist = MusicList()
            if(parentId == mbrowser.root){
                medialist.addFlag(MediaBrowser.MediaItem.FLAG_BROWSABLE)
                // Basisabfrage auf die Alben
                for(i in children){
                    medialist.addItem(MusicMetadata.createFromMediaDescription(i.description))
                }
                showAlbums()
            } else if(parentId.contains("ALBUM-")){
                // ein Album wurde abgefragt
                medialist.addFlag(MediaBrowser.MediaItem.FLAG_PLAYABLE)
                for(i in children){
                    medialist.addItem(MusicMetadata.createFromMediaDescription(i.description))
                }
                showTitles()
            }
            mbrowser.unsubscribe(parentId)
        }
    }
    inner class MusicControllerCallback: MediaController.Callback(){

        override fun onQueueChanged(queue: MutableList<MediaSession.QueueItem>) {
            super.onQueueChanged(queue)
            playingQueue = MusicList.createListFromQueue(queue)
            queueModel.setQueue(MusicList.createListFromQueue(queue))
        }

        override fun onMetadataChanged(metadata: MediaMetadata) {
            super.onMetadataChanged(metadata)
            // PROBLEM_ -- Das Interface erhält teilweise kein Update, Ausnahmefälle
            //Log.d(TAG,"setData, Aufruf für die Textleiste")
			setData(MusicMetadata.createFromMediaMetadata(metadata), MusicList.createListFromQueue(mediaController.queue))
        }
        override fun onPlaybackStateChanged(state: PlaybackState) {
            super.onPlaybackStateChanged(state)
            // update Bar
            if(state.state == PlaybackState.STATE_PLAYING){
                ibState.background = resources.getDrawable(R.mipmap.ic_pause, theme)
            } else {
                ibState.background = resources.getDrawable(R.mipmap.ic_play, theme)
            }
            if(design == 0){
                ibState.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.black, theme))
                ibState.backgroundTintMode = PorterDuff.Mode.SRC_ATOP
                ib_main_next.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.black, theme))
                ib_main_next.backgroundTintMode = PorterDuff.Mode.SRC_ATOP
            }
			if(state.extras != null){
				shuffleOn = state.extras.getBoolean("shuffle")
			}
            updateInterface.updateShuffleMode(shuffleOn)
        }
    }
}
