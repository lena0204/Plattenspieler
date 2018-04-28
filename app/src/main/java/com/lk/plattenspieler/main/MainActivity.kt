package com.lk.plattenspieler.main

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.media.AudioManager
import android.media.MediaDescription
import android.media.MediaMetadata
import android.media.browse.MediaBrowser
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast

import com.lk.plattenspieler.R
import com.lk.plattenspieler.background.MusicService
import com.lk.plattenspieler.database.SongContentProvider
import com.lk.plattenspieler.database.SongDB
import com.lk.plattenspieler.fragments.AlbumDetailsFragment
import com.lk.plattenspieler.fragments.AlbumFragment
import com.lk.plattenspieler.fragments.PlayingFragment
import com.lk.plattenspieler.utils.ThemeChanger
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : Activity(), AlbumFragment.OnClick, AlbumDetailsFragment.OnClick {

    companion object {
        const val PREF_PLAYING = "playing"
        const val PREF_DESIGN = "design"
        const val PREF_SHUFFLE = "shuffle"
    }

 	// IDEA_ -- Log in eine Datei schreiben für bessere Fehlersuche
    // IDEA_ Zufallswiedergabe aller Titel -> Queue async aufbauen

    private val TAG = "com.lk.pl-MainActivity"
    private val PERMISSION_REQUEST = 8009
    private val connectionCallback = BrowserConnectionCallback(this)
    val controllerCallback = MusicControllerCallback()
    val subscriptionCallback = MusicSubscriptionCallback()
    private var medialist = ArrayList<MediaBrowser.MediaItem>()
    private var playingQueue = mutableListOf<MediaSession.QueueItem>()
    private var design = 0      // 0 -> hell, 1 -> dunkel
    private var menu: Menu? = null
    private var shuffleOn = false

    lateinit var mbrowser: MediaBrowser
    private lateinit var tvTitle: TextView
    private lateinit var tvInterpret: TextView
    private lateinit var ibState: ImageButton
    private lateinit var updateInterface: CallbackPlaying
    private lateinit var metadata: MediaMetadata
    private lateinit var sharedPreferences: SharedPreferences

    // Interface zur Kommunikation mit der Wiedergabeansicht
    interface CallbackPlaying{
        fun updateMetadata(data: MediaMetadata, queue: String)
        fun updateShuffleMode(mode: Boolean)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        design = PreferenceManager.getDefaultSharedPreferences(this).getInt(PREF_DESIGN, 0)
        ThemeChanger().onActivityCreateSetTheme(this, design)
        setContentView(R.layout.activity_main)
        // onClickListener für die Wiedergabe und Views initiallisieren
        val pf = PlayingFragment()
        updateInterface = pf
        rl_playing.setOnClickListener {
            // Bundle mit den Wiedergabedaten erstellen
            if(!pf.isVisible){
                val args = prepareBundle()
                pf.arguments = args
                // Fragment ersetzen
                fragmentManager.beginTransaction()
                        .addToBackStack(null)
                        .replace(R.id.frame_layout, pf, "TAG_PLAYING")
                        .commit()
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
        // Alte Dateien löschen und die aktuelle Wiedergabe in die Datenbank schreiben
        contentResolver.delete(SongContentProvider.CONTENT_URI, null, null)
        savePlayingQueue()
        // Callbacks deaktivieren
        if(mediaController != null){
            mediaController.unregisterCallback(controllerCallback)
        }
        mbrowser.disconnect()
        Log.d(TAG, "onDestroy")
    }

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
            R.id.menu_change_design -> {
                changeDesign()
            }
            R.id.menu_remove_playing -> {
                stopAndRemovePlaying()
            }
            R.id.menu_dark_light -> {
                changeLightDark()
            }
            /*R.id.menu_delete_database -> {
                // DEBUGGING: Alte Dateien löschen und die aktuelle Wiedergabe in die Datenbank schreiben
                val number = contentResolver.delete(SongContentProvider.CONTENT_URI, null, null)
                Log.d(TAG, "Anzahl der gelöschten Zeilen: " + number)
            }*/
        }
        return super.onOptionsItemSelected(item)
    }

    // Listener von AlbumFragment und AlbumDetailsFragment
    override fun onClickAlbum(albumid: String) {
        val mediaid = "ALBUM-$albumid"
        /*
        * Beim zweiten Anklicken eines Albums im Fragment kommt er bis hier her, aber subscribe kommt
        * nicht in den Callback vom Service (onLoadChildren);
        * eine Möglichkeit wäre unsubscribe aufzurufen, wenn alle Daten im Callback angekommen sind;
        * evtl behebt das den Fehler -> ja tut es
        * */
        mbrowser.subscribe(mediaid, subscriptionCallback)
    }
    override fun onClickTitle(titleid: String) {
        val mc = mediaController
        var args = Bundle()
        args.putInt("I", 1)
        mc.transportControls.playFromMediaId(titleid, args)
        // Stelle in der Medialiste suchen und alle ab da an die Queue anhängen
        var indexInMedialist = -1
        for(item in medialist){
            if(item.mediaId == titleid){
                indexInMedialist = medialist.indexOf(item)
                break
            }
        }
        if(indexInMedialist != -1){
            var i = indexInMedialist + 1
            while(i < medialist.size){
				args = Bundle()
                args.putParcelable("S", medialist[i].description)
                mc.sendCommand("add", args, null)
                i++
            }
        }
        shuffleOn = false
        this.updateInterface.updateShuffleMode(shuffleOn)
    }
	// PROBLEM_  // -- shuffle scheint manchmal nicht alle Titel des Albums abzuspielen, beobachten
    override fun onShuffleClick(ptitleid: String) {
        var log = ""
        var titleid = ptitleid
        val mc = mediaController
        // Alles Items in listSongs kopieren, damit keine Operationen auf medialist stattfinden
        val listSongs = ArrayList<MediaBrowser.MediaItem>()
        for(item in medialist){
            listSongs.add(item)
        }
        val random = Random()
        // ERSTEN Titel zufällig auswählen und ABSPIELEN, ABER NICHT anhängen, weil sich das sonst doppelt
        var i = random.nextInt(listSongs.size)
        if(!listSongs[i].description.mediaId.isNullOrEmpty()){
            titleid = listSongs[i].description.mediaId.toString()
        }
        var args = Bundle()
        args.putInt("I", 1)
        mc.transportControls.playFromMediaId(titleid, args)
        listSongs.removeAt(i)
        // zufällige Liste erstellen und an die QUEUE hängen, ersten Titel aus der Queue abspielen
        while(!listSongs.isEmpty()){
            i = random.nextInt(listSongs.size)
			args = Bundle()
            args.putParcelable("S", listSongs[i].description)
            mc.sendCommand("add", args, null)
            listSongs.removeAt(i)
        }
        mc.sendCommand("shuffle", null, null)
        shuffleOn = true
        this.updateInterface.updateShuffleMode(shuffleOn)
    }

    // Methoden, die aus den inneren Klassen aufgerufen werden (Zugriff auf Attribute von MainActivity)
    // UI, Musikkontrolle oder Anzeigen anderer Fragments (Alben etc)
    fun setupUI(){
        // Listener
        ib_main_play.setOnClickListener { onClickPlay() }
        ib_main_next.setOnClickListener { onClickNext() }
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
	// FIXME untere Leiste auch löschen, wenn die Wiedergabeliste gelöscht wird (Update der Metadaten)
    fun setData(pdata: MediaMetadata?, queue: MutableList<MediaSession.QueueItem>?){
        // TODO -- ordentlich Aufräumen nachdem die Wiedergabeliste gelöscht wurde -> Sinn?
        var items = ""
        var i = 0
        if(queue != null && queue.size > 0) {
            while (i < queue.size) {
                items = items + queue[i].description.title + "\n - " + queue[i].description.subtitle + "__"
                i++
            }
            items = items.substring(0, items.length - 2)
        }
		if(pdata != null && pdata.description.mediaId != null) {
			updateInterface.updateMetadata(pdata, items)
			//Log.d(TAG, "setData Metadaten: .${pdata.description.mediaId}.")
			metadata = pdata
			tvTitle.text = pdata.description.title
			tvInterpret.text = pdata.description.subtitle
		} else {
			// falls keine Wiedergabe stattfindet oder die Wiedergabe zu Ende / beendet wurde
			Log.d(TAG, "pdata = null IS" + (pdata == null) + ", mediaId = null IS " + (pdata?.description?.mediaId))
			tvTitle.text = resources.getString(R.string.no_title)
			tvInterpret.text = ""
		}
    }
    fun onClickPlay() {
        val mc = mediaController
        if(mc.playbackState.state == PlaybackState.STATE_PLAYING){
            mc.transportControls.pause()
        } else {
            mc.transportControls.play()
        }
    }
    fun onClickNext() {
        mediaController.transportControls.skipToNext()
    }
    fun showAlbums(){
        val af = AlbumFragment(this)
        val extras = Bundle()
        extras.putParcelableArrayList("Liste", medialist)
        af.arguments = extras
        fragmentManager.beginTransaction().replace(R.id.frame_layout, af).commit()
    }
    fun showTitles(){
        val adf = AlbumDetailsFragment(this)
        val extras = Bundle()
        extras.putParcelableArrayList("Liste", medialist)
        Log.d(TAG, "showTitles")
        adf.arguments = extras
        fragmentManager.beginTransaction()
                .addToBackStack(null)
                .replace(R.id.frame_layout, adf).commit()
    }

    // Bundle für das PlayingFragment bereitstellen
    private fun prepareBundle(): Bundle{
        val args = Bundle()
        val meta = mediaController.metadata
        val data = meta.getString(MediaMetadata.METADATA_KEY_MEDIA_ID) + "__" +
                meta.getString(MediaMetadata.METADATA_KEY_TITLE) + "__" +
                meta.getString(MediaMetadata.METADATA_KEY_ARTIST) + "__" +
                meta.getString(MediaMetadata.METADATA_KEY_ALBUM) + "__" +
                meta.getLong(MediaMetadata.METADATA_KEY_DURATION) + "__" +
                meta.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI) + "__" +
                meta.getLong(MediaMetadata.METADATA_KEY_NUM_TRACKS) + "__" +
                meta.getString(MediaMetadata.METADATA_KEY_WRITER)
        val queue = mediaController.queue
        var items = ""
        var i = 0
        if(queue != null && queue.size > 0) {
            while (i < queue.size) {
                items = items + queue[i].description.title + "\n - " + queue[i].description.subtitle + "__"
                i++
            }
            items = items.substring(0, items.length - 2)
        }
        Log.d(TAG, "PrepareBundle, shuffle ist $shuffleOn")
        args.putString("Q", items)
        args.putString("T", data)
        args.putInt("S", mediaController.playbackState.state)
        args.putBoolean("shuffle", shuffleOn)
        return args
    }
    // Setup beenden, wenn der Zugriff erlaubt wurde
    private fun completeSetup(){
        // Setup MediaBrowser
        val c = ComponentName(applicationContext, MusicService::class.java)
        mbrowser = MediaBrowser(this, c, connectionCallback, null)
        mbrowser.connect()
        // Broadcast für die Musikkontrolle setzen
        val ifilter = IntentFilter()
        ifilter.addAction("PLAY")
        ifilter.addAction("NEXT")
    }
    // Berechtigung abfragen (Marshmallow+ )
    private fun checkReadPermission(): Boolean{
        return if(this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            this.requestPermissions(Array(1, { _ -> Manifest.permission.READ_EXTERNAL_STORAGE}), PERMISSION_REQUEST)
			false
        } else {
			true
        }
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
        ThemeChanger().changeToTheme(this)
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
        ThemeChanger().changeToTheme(this)
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
    }

    // Datenbank
    // PROBLEM_ hat Schlange nach Pausieren und dann Neustart nicht gespeichert (oder mind. nicht wiederhergestellt)
    private fun savePlayingQueue(){
        var i = 0
        // wenn die Wartschlange etwas enthält, muss es auch aktuelle Metadaten geben und nur wenn
        // nicht abgespielt wird
        contentResolver.delete(SongContentProvider.CONTENT_URI, null, null)
        if(playingQueue.size > 0 && mediaController.playbackState.state != PlaybackState.STATE_PLAYING){
            // aktuelle Metadaten sichern
            var values = ContentValues()
            values.put(SongDB.COLUMN_ID, metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID))
            values.put(SongDB.COLUMN_TITLE, metadata.getString(MediaMetadata.METADATA_KEY_TITLE))
            values.put(SongDB.COLUMN_ARTIST, metadata.getString(MediaMetadata.METADATA_KEY_ARTIST))
            values.put(SongDB.COLUMN_ALBUM, metadata.getString(MediaMetadata.METADATA_KEY_ALBUM))
            values.put(SongDB.COLUMN_COVER_URI, metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI))
            values.put(SongDB.COLUMN_DURATION, metadata.getLong(MediaMetadata.METADATA_KEY_DURATION).toString())
            values.put(SongDB.COLUMN_NUMTRACKS, metadata.getLong(MediaMetadata.METADATA_KEY_NUM_TRACKS).toString())
            values.put(SongDB.COLUMN_FILE, metadata.getString(MediaMetadata.METADATA_KEY_WRITER))
			contentResolver.insert(SongContentProvider.CONTENT_URI, values)
            // Warteschlange sichern
            Log.d(TAG, "Länge der Schlange: " + playingQueue.size)
            //  PROBLEM_ -- Manchmal Error: Unique constraint failed (_id ist nicht eindeutig(Primärschlüssel));
            //  wenn er versucht eine Queue erneut einzufügen, obwohl die zur aktuellen wiedergabe gehört
            while(i < playingQueue.size){
                values = ContentValues()
                val media = playingQueue[i].description
                values.put(SongDB.COLUMN_ID, media.mediaId)
                values.put(SongDB.COLUMN_TITLE, media.title.toString())
                values.put(SongDB.COLUMN_ARTIST, media.subtitle.toString())
                val mediaArray = media.description.toString().split("__".toRegex())
                values.put(SongDB.COLUMN_ALBUM, mediaArray[0])
                values.put(SongDB.COLUMN_COVER_URI, mediaArray[1])
                values.put(SongDB.COLUMN_DURATION, "")
                values.put(SongDB.COLUMN_NUMTRACKS, "")
                values.put(SongDB.COLUMN_FILE, "")
                contentResolver.insert(SongContentProvider.CONTENT_URI, values)
                i++
            }
            sharedPreferences.edit().putBoolean(PREF_PLAYING, true)
                    .putBoolean(PREF_SHUFFLE,shuffleOn).apply()
        }
    }
    fun restorePlayingQueue(){
        if(sharedPreferences.getBoolean(PREF_PLAYING, false)){
            val projection = getProjection()
            val mc = mediaController
            val c = contentResolver.query(SongContentProvider.CONTENT_URI, projection, null, null, null)
            if(c != null && c.count != 0) {
                c.moveToFirst()
                var i: Long = 0
                // ersten Datensatz in die aktuelle Wiedergabe schreiben und an den Service weitergeben
                val builder = MediaMetadata.Builder()
                builder.putString(MediaMetadata.METADATA_KEY_MEDIA_ID, c.getString(c.getColumnIndex(SongDB.COLUMN_ID)))
                        .putString(MediaMetadata.METADATA_KEY_TITLE, c.getString(c.getColumnIndex(SongDB.COLUMN_TITLE)))
                        .putString(MediaMetadata.METADATA_KEY_ARTIST, c.getString(c.getColumnIndex(SongDB.COLUMN_ARTIST)))
                        .putString(MediaMetadata.METADATA_KEY_ALBUM, c.getString(c.getColumnIndex(SongDB.COLUMN_ALBUM)))
                        .putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, c.getString(c.getColumnIndex(SongDB.COLUMN_COVER_URI)))
                        .putLong(MediaMetadata.METADATA_KEY_DURATION, c.getString(c.getColumnIndex(SongDB.COLUMN_DURATION)).toLong())
                        .putLong(MediaMetadata.METADATA_KEY_NUM_TRACKS, c.getString(c.getColumnIndex(SongDB.COLUMN_NUMTRACKS)).toLong())
                mc.transportControls.playFromMediaId(builder.build().getString(MediaMetadata.METADATA_KEY_MEDIA_ID), Bundle())
                c.moveToNext()
                // Metadata zusammenstellen und an den Service weitergeben
                while(!c.isAfterLast){
                    val description = MediaDescription.Builder()
                    description.setMediaId(c.getString(c.getColumnIndex(SongDB.COLUMN_ID)))
                            .setTitle(c.getString(c.getColumnIndex(SongDB.COLUMN_TITLE)))
                            .setSubtitle(c.getString(c.getColumnIndex(SongDB.COLUMN_ARTIST)))
                    val des = c.getString(c.getColumnIndex(SongDB.COLUMN_ALBUM)) + "__" +
                            c.getString(c.getColumnIndex(SongDB.COLUMN_COVER_URI))
                    description.setDescription(des)
                    playingQueue.add(MediaSession.QueueItem(description.build(), i++))
                    val args = Bundle()
                    args.putParcelable("S", description.build())
                    mc.sendCommand("add", args, null)
                    c.moveToNext()
                }
                setData(builder.build(), playingQueue)
            }
            c.close()
            // shuffle auslesen
            shuffleOn = sharedPreferences.getBoolean(PREF_SHUFFLE,false)
            if(shuffleOn){
                mc.sendCommand("shuffle", null, null)
            }
        }
    }
    private fun getProjection(): Array<String>{
        val p = Array(7, { _ -> "" })
        p[0] = SongDB.COLUMN_ID
        p[1] = SongDB.COLUMN_TITLE
        p[2] = SongDB.COLUMN_ARTIST
        p[3] = SongDB.COLUMN_ALBUM
        p[4] = SongDB.COLUMN_COVER_URI
        p[5] = SongDB.COLUMN_DURATION
        p[6] = SongDB.COLUMN_NUMTRACKS
        return p
    }

    // Callbacks vom MusicService und PlayingFragment
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
            medialist = ArrayList()
            if(parentId == mbrowser.root){
                // Basisabfrage auf die Alben
                for(i in children){
                    medialist.add(i)
                }
                showAlbums()
            } else if(parentId.contains("ALBUM-")){
                // ein Album wurde abgefragt
                for(i in children){
                    medialist.add(i)
                }
                showTitles()
            }
            mbrowser.unsubscribe(parentId)
        }
    }
    inner class MusicControllerCallback: MediaController.Callback(){

        override fun onQueueChanged(queue: MutableList<MediaSession.QueueItem>) {
            super.onQueueChanged(queue)
            playingQueue = queue
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            super.onMetadataChanged(metadata)
            // update Bar mit aktuellen Infos
            // PROBLEM_ -- Das Interface erhält teilweise kein Update, Ausnahmefälle
            //Log.d(TAG,"setData, Aufruf für die Textleiste")
			setData(metadata, mediaController.queue)
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
    inner class BroadcastTransportControls: BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            if(intent?.action == "PLAY"){
                onClickPlay()
            } else if(intent?.action == "NEXT"){
                onClickNext()
            }
        }
    }
}
