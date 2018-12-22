package com.lk.musicservicelibrary.main

import android.media.*
import android.media.session.MediaSession
import android.os.*
import android.util.Log
import com.lk.musicservicelibrary.models.*
import com.lk.musicservicelibrary.system.AudioFocusRequester
import com.lk.musicservicelibrary.utils.*

/**
 * Erstellt von Lena am 02.09.18.
 */
class MusicActionsCallback internal constructor(
        private val service: MusicService,
        private val metadataRepo: MetadataRepository
    ): MediaSession.Callback() {

    private val TAG = MusicActionsCallback::class.java.simpleName
    private val serviceDataHandler = DelegatingFunctions<PlaybackData>()

    private var musicPlayer = MediaPlayer()
    private var currentPlaybackPosition = 0L
    private var musicPlayerCreated = false

    val audioFocusChanged = fun (old: EnumAudioFucos, new: EnumAudioFucos) {
        TODO ("changedHandler not implemented")
    }

    private val queueCreated = fun(data: PlaybackData) {
        serviceDataHandler.callWithParameter(data)
    }

    init {
        serviceDataHandler += service.onDataChanged
        metadataRepo.addQueueCreatedListener(this.queueCreated)
    }

    fun createInitialData(){
        serviceDataHandler.callWithParameter(metadataRepo.createInitialData())
    }

    override fun onPlayFromMediaId(mediaId: String, extras: Bundle?) {
        extras?.classLoader = this.javaClass.classLoader
        val shuffleOn = extras?.getBoolean("shuffle") ?: false
        if(shallPrepare(extras)){
            // Callback onPrepareFromId ist erst ab API 24 (aktuell 21) möglich
            prepareFromIdAfterRestart(mediaId, shuffleOn)
        } else {
            playFromId(mediaId, shuffleOn)
        }
    }

    private fun shallPrepare(extras: Bundle?): Boolean = extras != null && extras.containsKey("I")

    private fun playFromId(pId: String, shuffleOn: Boolean){
        Log.v(TAG, "playFromId")
        currentPlaybackPosition = 0
        serviceDataHandler.callWithParameter(metadataRepo.updatePlayFromId(pId, shuffleOn))
        onPlay()
    }

    private fun prepareFromIdAfterRestart(pId: String, shuffleOn: Boolean){
        Log.v(TAG, "prepareAfterRestart")
        currentPlaybackPosition = 0
        serviceDataHandler.callWithParameter(metadataRepo.updatePrepareFromId(pId, shuffleOn))
    }

    override fun onPlay() {
        super.onPlay()
        if(canPlay()) {
            service.startServiceIfNecessary()
            createMusicPlayerIfNecessary()
            musicPlayer.setDataSource(metadataRepo.getCurrentMusicPath())
            musicPlayer.prepareAsync()
        }
    }

    private fun canPlay(): Boolean {
        if(musicPlayerCreated && musicPlayer.isPlaying) {
            musicPlayer.stop()
        }
        musicPlayer.reset()
        if(metadataRepo.getCurrentMusicId() != "" && AudioFocusRequester.requestAudioFocus()){
            // audioFucosStatus = EnumAudioFucos.AUDIO_FOCUS
            return true
        }
        return false
    }

    private fun createMusicPlayerIfNecessary(){
        if(!musicPlayerCreated) {
            musicPlayer = MediaPlayer()
            musicPlayer.setOnPreparedListener { _ ->
                musicPlayer.seekTo(currentPlaybackPosition.toInt())
                musicPlayer.start()
                serviceDataHandler.callWithParameter(metadataRepo.updatePlay(currentPlaybackPosition))
            }
            musicPlayer.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MusicPlayerError: $what; $extra")
                false
            }
            musicPlayer.setOnCompletionListener { _ -> onSkipToNext() }
            musicPlayer.setAudioAttributes(AudioFocusRequester.audioAttr)
            musicPlayerCreated = true
        }
    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG, "handleOnPause")
        musicPlayer.pause()
        currentPlaybackPosition = musicPlayer.currentPosition.toLong()
        serviceDataHandler.callWithParameter(metadataRepo.updatePause(currentPlaybackPosition))
    }

    override fun onSkipToNext() {
        val data = metadataRepo.updateNext()
        if(data.queue.isEmpty()){
            onStop()
        } else {
            currentPlaybackPosition = 0
            onPlay()
        }
        serviceDataHandler.callWithParameter(data)
    }

    override fun onSkipToPrevious() {
        super.onSkipToPrevious()
        val position = musicPlayer.currentPosition.toLong()
        metadataRepo.updatePrevious(position)
        currentPlaybackPosition = 0L
        onPlay()
    }

    override fun onStop() {
        Log.d(TAG, "handleOnStop")
        currentPlaybackPosition = 0L
        serviceDataHandler.callWithParameter(metadataRepo.updateStop())
        AudioFocusRequester.releaseAudioFocus()
        // audioFucosStatus = EnumAudioFucos.AUDIO_LOSS
        musicPlayer.stop()
        musicPlayer.reset()
        musicPlayer.release()
        musicPlayerCreated = false
    }

    override fun onCommand(command: String, args: Bundle?, resultReceiver: ResultReceiver?) {
        when(command){
            "addQueue" -> addQueueToService(args, QueueType.QUEUE_ORDERED)
            "addRestoredQueue" -> addQueueToService(args, QueueType.QUEUE_RESTORED)
            "addRandomQueue" -> addQueueToService(args, QueueType.QUEUE_SHUFFLE)
            "addAll" -> addAllSongsToPlayingQueue()
        }
    }

    private fun addQueueToService(args: Bundle?, flag: QueueType){
        var mediaList = MusicList()
        if(args != null) {
            args.classLoader = this.javaClass.classLoader
            val list = args.getParcelable<MusicList>("L")
            if(list != null)
                mediaList = list
        }
        metadataRepo.createQueueFromMediaList(mediaList, metadataRepo.getCurrentMusicId(), flag)
    }

    private fun addAllSongsToPlayingQueue(){
        val playingID = metadataRepo.getFirstItemForShuffleAll()
        playFromId(playingID, true)
        metadataRepo.createQueueFromMediaList(MusicList(), playingID, QueueType.QUEUE_ALL_SHUFFLE)
    }
}