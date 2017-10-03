package com.lk.plattenspieler.main

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.media.AudioManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
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

class MainActivity : Activity(), AlbumFragment.onClick, AlbumDetailsFragment.onClick {

    val TAG = "MainActivity"
    val PREF_PLAYING = "playing"
    val PREF_DESIGN = "design"
    val PERMISSION_REQUEST = 8009
    val connectionCallback = BrowserConnectionCallback(this)
    val controllerCallback = MusicControllerCallback(this)
    val subscriptionCallback = MusicSubscriptionCallback()

    var medialist = ArrayList<MediaBrowserCompat.MediaItem>()
    var playingQueue = mutableListOf<MediaSessionCompat.QueueItem>()
    var design = 0      // 0 -> hell, 1 -> dunkel

    lateinit var mbrowser: MediaBrowserCompat
    lateinit var tv_title: TextView
    lateinit var tv_interpret: TextView
    lateinit var ib_state: ImageButton
    lateinit var updateInterface: CallbackPlaying
    lateinit var metadata: MediaMetadataCompat

    // Interface zur Kommunikation mit der Wiedergabeansicht
    interface CallbackPlaying{
        fun updateMetadata(data: MediaMetadataCompat, queue: String)
        fun updateShuffleMode(mode: Boolean)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        tv_title = this.findViewById(R.id.tv_main_title) as TextView
        tv_interpret = this.findViewById(R.id.tv_main_interpret) as TextView
        ib_state = this.findViewById(R.id.ib_main_play) as ImageButton
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
        val number = contentResolver.delete(SongContentProvider.CONTENT_URI, null, null)
        savePlayingQueue()
        // Callbacks deaktivieren
        if(MediaControllerCompat.getMediaController(this) != null){
            MediaControllerCompat.getMediaController(this).unregisterCallback(controllerCallback)
        }
        mbrowser.disconnect()
        Log.d(TAG, "onDestroy")
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options_menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.menu_change_design -> {
                changeDesign()
            }
           /* R.id.menu_delete_database -> {
                // DEBUGGING: Alte Dateien löschen und die aktuelle Wiedergabe in die Datenbank schreiben
                val number = contentResolver.delete(SongContentProvider.CONTENT_URI, null, null)
                Log.d(TAG, "Anzahl der gelöschten Zeilen: " + number)
            }*/
        }
        return super.onOptionsItemSelected(item)
    }

    // Listener von AlbumFragment und AlbumDetailsFragment
    override fun onClickAlbum(albumid: String) {
        val mediaid = "ALBUM-" + albumid
        mbrowser.subscribe(mediaid, subscriptionCallback)
    }
    override fun onClickTitle(titleid: String) {
        val mc = MediaControllerCompat.getMediaController(this)
        mc.transportControls.playFromMediaId(titleid, null)
        mc.transportControls.setShuffleModeEnabled(false)
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
                mc.addQueueItem(medialist[i].description)
                i++
            }
        }
    }
    // TODO shuffle scheint manchmal nicht alle Titel des Albums abzuspielen, beobachten
    override fun onShuffleClick(ptitleid: String) {
        var titleid = ptitleid
        val mc = MediaControllerCompat.getMediaController(this)
        mc.transportControls.setShuffleModeEnabled(true)
        // Alles Items in listSongs kopieren, damit keine Operationen auf medialist stattfinden
        val listSongs = ArrayList<MediaBrowserCompat.MediaItem>()
        for(item in medialist){
            listSongs.add(item)
        }
        val random = Random()
        // ERSTEN Titel zufällig auswählen und ABSPIELEN, ABER NICHT anhängen, weil sich das sonst doppelt
        var i = random.nextInt(listSongs.size)
        if(!listSongs[i].description.mediaId.isNullOrEmpty()){
            titleid = listSongs[i].description.mediaId.toString()
        }
        mc.transportControls.playFromMediaId(titleid, null)
        listSongs.removeAt(i)
        // zufällige Liste erstellen und an die QUEUE hängen, ersten Titel aus der Queue abspielen
        while(!listSongs.isEmpty()){
            i = random.nextInt(listSongs.size)
            mc.addQueueItem(listSongs[i].description)
            listSongs.removeAt(i)
        }
    }

    // Methoden, die aus den inneren Klassen aufgerufen werden (Zugriff auf Attribute von MainActivity)
    // UI, Musikkontrolle oder Anzeigen anderer Fragments (Alben etc)
    fun setupUI(){
        // Listener
        ib_main_play.setOnClickListener { onClickPlay() }
        ib_main_next.setOnClickListener { onClickNext() }
        val mc = MediaControllerCompat.getMediaController(this)
        // update Bar mit aktuellen Infos
        if(mc.playbackState.state == PlaybackStateCompat.STATE_PLAYING){
            ib_state.background = resources.getDrawable(R.mipmap.ic_pause)
        } else {
            ib_state.background = resources.getDrawable(R.mipmap.ic_play)
        }
        if(design == 0){
            ib_state.backgroundTintList = ColorStateList.valueOf(R.color.black)
            ib_state.backgroundTintMode = PorterDuff.Mode.SRC_ATOP
            ib_main_next.backgroundTintList = ColorStateList.valueOf(R.color.black)
            ib_main_next.backgroundTintMode = PorterDuff.Mode.SRC_ATOP
        }
        tv_title.text = mc.metadata.description.title
        tv_interpret.text = mc.metadata.description.subtitle
        // register Callback
        mc.registerCallback(controllerCallback)
    }
    fun setData(pdata: MediaMetadataCompat, queue: MutableList<MediaSessionCompat.QueueItem>){
        var items = ""
        var i = 0
        while (i < queue.size){
            items = items + queue[i].description.title + "\n - " + queue[i].description.subtitle + "__"
            i++
        }
        items = items.substring(0, items.length - 2)
        updateInterface.updateMetadata(pdata, items)
        metadata = pdata
        tv_title.text = pdata.description.title
        tv_interpret.text = pdata.description.subtitle
    }
    fun onClickPlay() {
        val mc = MediaControllerCompat.getMediaController(this)
        if(mc.playbackState.state == PlaybackStateCompat.STATE_PLAYING){
            mc.transportControls.pause()
        } else {
            mc.transportControls.play()
        }
    }
    fun onClickNext() {
        MediaControllerCompat.getMediaController(this).transportControls.skipToNext()
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
        adf.arguments = extras
        fragmentManager.beginTransaction()
                .addToBackStack(null)
                .replace(R.id.frame_layout, adf).commit()
    }

    // Bundle für das PlayingFragment bereitstellen
    private fun prepareBundle(): Bundle{
        val args = Bundle()
        val meta = MediaControllerCompat.getMediaController(this).metadata
        val data = meta.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID) + "__" +
                meta.getString(MediaMetadataCompat.METADATA_KEY_TITLE) + "__" +
                meta.getString(MediaMetadataCompat.METADATA_KEY_ARTIST) + "__" +
                meta.getString(MediaMetadataCompat.METADATA_KEY_ALBUM) + "__" +
                meta.getLong(MediaMetadataCompat.METADATA_KEY_DURATION) + "__" +
                meta.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI) + "__" +
                meta.getLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS)
        val queue = MediaControllerCompat.getMediaController(this).queue
        var items = ""
        var i = 0
        while (i < queue.size){
            items = items + queue[i].description.title + "\n - " + queue[i].description.subtitle + "__"
            i++
        }
        items = items.substring(0, items.length - 2)
        args.putString("Q", items)
        args.putString("T", data)
        args.putInt("S", MediaControllerCompat.getMediaController(this).playbackState.state)
        return args
    }
    // Setup beenden, wenn der Zugriff erlaubt wurde
    fun completeSetup(){
        // Setup MediaBrowser
        val c = ComponentName(applicationContext, MusicService::class.java)
        mbrowser = MediaBrowserCompat(this, c, connectionCallback, null)
        mbrowser.connect()
        // Broadcast für die Musikkontrolle setzen
        val ifilter = IntentFilter()
        ifilter.addAction("PLAY")
        ifilter.addAction("NEXT")
    }
    // Berechtigung abfragen (Marshmallow+ )
    fun checkReadPermission(): Boolean{
        if(this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            this.requestPermissions(Array(1, { _ -> Manifest.permission.READ_EXTERNAL_STORAGE}), PERMISSION_REQUEST)
            return false
        } else {
            return true
        }
    }
    fun changeDesign(){
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        var design = sp.getInt(PREF_DESIGN, 0)
        Log.d(TAG, "Design: " + design)
        if(design == 0) design = 1
        else design = 0
        sp.edit().putInt(PREF_DESIGN, design).commit()
        ThemeChanger().changeToTheme(this)
    }

    // Datenbank
    fun savePlayingQueue(){
        var i = 0
        // wenn die Wartschlange etwas enthält, muss es auch aktuelle Metadaten geben und nur wenn
        // nicht abgespielt wird
        if(playingQueue.size > 0 && MediaControllerCompat.getMediaController(this).playbackState.state != PlaybackStateCompat.STATE_PLAYING){
            Log.d(TAG, "save Queue")
            // aktuelle Metadaten sichern
            var values = ContentValues()
            values.put(SongDB.COLUMN_ID, metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID))
            values.put(SongDB.COLUMN_TITLE, metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
            values.put(SongDB.COLUMN_ARTIST, metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
            values.put(SongDB.COLUMN_ALBUM, metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM))
            values.put(SongDB.COLUMN_COVER_URI, metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI))
            values.put(SongDB.COLUMN_DURATION, metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION).toString())
            values.put(SongDB.COLUMN_NUMTRACKS, metadata.getLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS).toString())
            contentResolver.insert(SongContentProvider.CONTENT_URI, values)
            // Warteschlange sichern
            Log.d(TAG, "Länge der Schlange: " + playingQueue.size)
            /*  TODO Manchmal Error: Unique constraint failed (_id ist nicht eindeutig(Primärschlüssel));
                TODO wenn er versucht eine Queue erneut einzufügen, obwohl die zur aktuellen wiedergabe gehört */
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
                contentResolver.insert(SongContentProvider.CONTENT_URI, values)
                i++
            }
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(PREF_PLAYING, true).apply()
        }
    }
    fun restorePlayingQueue(){
        if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(PREF_PLAYING, false)){
            val projection = getProjection()
            val c = contentResolver.query(SongContentProvider.CONTENT_URI, projection, null, null, null)
            if(c != null && c.count != 0) {
                c.moveToFirst()
                var i: Long = 0
                val mc = MediaControllerCompat.getMediaController(this)
                // ersten Datensatz in die aktuelle Wiedergabe schreiben und an den Service weitergeben
                val builder = MediaMetadataCompat.Builder()
                builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, c.getString(c.getColumnIndex(SongDB.COLUMN_ID)))
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, c.getString(c.getColumnIndex(SongDB.COLUMN_TITLE)))
                        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, c.getString(c.getColumnIndex(SongDB.COLUMN_ARTIST)))
                        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, c.getString(c.getColumnIndex(SongDB.COLUMN_ALBUM)))
                        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, c.getString(c.getColumnIndex(SongDB.COLUMN_COVER_URI)))
                        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, c.getString(c.getColumnIndex(SongDB.COLUMN_DURATION)).toLong())
                        .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, c.getString(c.getColumnIndex(SongDB.COLUMN_NUMTRACKS)).toLong())
                mc.transportControls.prepareFromMediaId(builder.build().getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID), null)
                c.moveToNext()
                // Metadata zusammenstellen und an den Service weitergeben
                while(!c.isAfterLast){
                    val description = MediaDescriptionCompat.Builder()
                    description.setMediaId(c.getString(c.getColumnIndex(SongDB.COLUMN_ID)))
                            .setTitle(c.getString(c.getColumnIndex(SongDB.COLUMN_TITLE)))
                            .setSubtitle(c.getString(c.getColumnIndex(SongDB.COLUMN_ARTIST)))
                    val des = c.getString(c.getColumnIndex(SongDB.COLUMN_ALBUM)) + "__" +
                            c.getString(c.getColumnIndex(SongDB.COLUMN_COVER_URI))
                    description.setDescription(des)
                    playingQueue.add(MediaSessionCompat.QueueItem(description.build(), i++))
                    mc.addQueueItem(description.build())
                    c.moveToNext()
                }
                setData(builder.build(), playingQueue)
            }
            c.close()
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
    inner class BrowserConnectionCallback(act: Activity): MediaBrowserCompat.ConnectionCallback(){

        val activity: Activity

        init { activity = act }

        override fun onConnected() {
            val token = mbrowser.sessionToken
            val musicController = MediaControllerCompat(activity, token)
            MediaControllerCompat.setMediaController(activity, musicController)
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
            if(MediaControllerCompat.getMediaController(activity) != null){
                MediaControllerCompat.getMediaController(activity).unregisterCallback(controllerCallback)
                MediaControllerCompat.setMediaController(activity, null)
            }
        }
    }
    inner class MusicSubscriptionCallback: MediaBrowserCompat.SubscriptionCallback(){

        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
            medialist = ArrayList<MediaBrowserCompat.MediaItem>()
            if(parentId == mbrowser.root){
                // Basisabfrage auf die Alben
                var c = 0
                for(i in children){
                    medialist.add(children[c])
                    c++
                }
                showAlbums()
            } else if(parentId.contains("ALBUM-")){
                // ein Album wurde abgefragt
                var c = 0
                for(i in children){
                    medialist.add(children[c])
                    c++
                }
                showTitles()
            }
        }
    }
    inner class MusicControllerCallback(act: Activity): MediaControllerCompat.Callback(){

        val a: Activity
        init{ a = act }

        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>) {
            super.onQueueChanged(queue)
            playingQueue = queue
        }
        override fun onShuffleModeChanged(enabled: Boolean) {
            updateInterface.updateShuffleMode(enabled)
        }
        override fun onMetadataChanged(metadata: MediaMetadataCompat) {
            // update Bar mit aktuellen Infos
            // TODO Das Interface erhält teilweise kein Update, Ausnahmefälle
            setData(metadata, MediaControllerCompat.getMediaController(this@MainActivity).queue)
        }
        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            // update Bar
            if(state.state == PlaybackStateCompat.STATE_PLAYING){
                ib_state.background = resources.getDrawable(R.mipmap.ic_pause)
            } else {
                ib_state.background = resources.getDrawable(R.mipmap.ic_play)
            }
            if(design == 0){
                ib_state.backgroundTintList = ColorStateList.valueOf(R.color.black)
                ib_state.backgroundTintMode = PorterDuff.Mode.SRC_ATOP
                ib_main_next.backgroundTintList = ColorStateList.valueOf(R.color.black)
                ib_main_next.backgroundTintMode = PorterDuff.Mode.SRC_ATOP
            }
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
