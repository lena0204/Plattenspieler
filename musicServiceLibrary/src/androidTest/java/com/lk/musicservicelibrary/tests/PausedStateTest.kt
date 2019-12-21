package com.lk.musicservicelibrary.tests

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.lk.musicservicelibrary.helpers.AssertPlayback
import com.lk.musicservicelibrary.mocks.*
import com.lk.musicservicelibrary.models.MusicList
import com.lk.musicservicelibrary.models.MusicMetadata
import com.lk.musicservicelibrary.playback.PlaybackCallback
import com.lk.musicservicelibrary.playback.state.States
import org.junit.*
import java.lang.UnsupportedOperationException

/**
 * Erstellt von Lena am 2019-08-18.
 * WICHTIG: Handler in BasicState auskommentieren, verträgt sich nicht mit UnitTests
 */
class PausedStateTest {

    @get:Rule
    var rule: InstantTaskExecutorRule = InstantTaskExecutorRule()

    private val mockPlayer = MockMusicPlayer()
    private val mockRepo = MockMusicRepository()
    private val playlist = createPlaylist()
    private val mockPlaylistRepo = MockPlaylistRepository()

    private lateinit var playback: PlaybackCallback
    private lateinit var assertPlayback: AssertPlayback

    private fun createPlaylist(): MusicList {
        val musicList = MusicList()
        val data1 = MusicMetadata("1", "Gone", "Nena", path = "storage/music/gone/forever1")
        val data2 = MusicMetadata("2", "Gone", "Nena", path = "storage/music/gone/beyondBorders2")
        val data3 = MusicMetadata("3", "Gone", "Nena", path = "storage/music/gone/neverAgain3")
        musicList.addItem(data1)
        musicList.addItem(data2)
        musicList.addItem(data3)
        return musicList
    }

    @Before
    fun prepareBeforeEachTest() {
        prepareBeforeEachTest("1")
    }

    private fun prepareBeforeEachTest(mediaId: String) {
        playback = PlaybackCallback(mockRepo, mockPlaylistRepo)
        assertPlayback = AssertPlayback(playback)
        playback.setPlayer(mockPlayer)
        playback.setQueriedMediaList(playlist)
        // playbackstate auf paused setzen
        playlist.setCurrentPlaying(-1)
        playback.onPlayFromMediaId(mediaId, null)
        playback.onPause()
    }

    @Test
    fun testToPlay() {
        (playback.getPlayer() as MockMusicPlayer).setPositionTo(40000)
        playback.onPlay()
        assertPlayback.stateIs(States.PLAYING)
        assertPlayback.positionIs(40050)
    }

    @Test
    fun testToPause() {
        try {
            playback.onPause()
            Assert.fail("Hat keinen Fehler geworfen")
        } catch(e: UnsupportedOperationException) {
            Assert.assertTrue(true)
        }
    }

    @Test
    fun testToStop() {
        playback.onStop()
        assertPlayback.stateIs(States.STOPPED)
    }

    @Test
    fun testToSkipToNext_None() {
        prepareBeforeEachTest("3")
        playback.onSkipToNext()
        assertPlayback.stateIs(States.STOPPED)
        assertPlayback.mediaIdIs("")
    }

    @Test
    fun testToSkipToNext_Available() {
        playback.onSkipToNext()
        assertPlayback.stateIs(States.PAUSED)
        assertPlayback.positionIs(0)
        assertPlayback.mediaIdIs("2")
    }

    @Test
    fun testSkipToPrevious_None() {
        playback.onSkipToPrevious()
        assertPlayback.stateIs(States.PAUSED)
        assertPlayback.positionIs(0)
        assertPlayback.mediaIdIs("1")
    }

    @Test
    fun testSkipToPrevious_Less15s() {
        prepareBeforeEachTest("2")
        playback.onSkipToPrevious()
        assertPlayback.stateIs(States.PAUSED)
        assertPlayback.positionIs(0)
        assertPlayback.mediaIdIs("1")
    }

    @Test
    fun testSkipToPrevious_More15s() {
        prepareBeforeEachTest("2")
        (playback.getPlayer() as MockMusicPlayer).setPositionTo(40000)
        playback.onSkipToPrevious()
        assertPlayback.stateIs(States.PAUSED)
        assertPlayback.positionIs(0)
        assertPlayback.mediaIdIs("2")
    }

}