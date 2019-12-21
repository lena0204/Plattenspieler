package com.lk.musicservicelibrary.mocks

import com.lk.musicservicelibrary.database.PlaylistRepository
import com.lk.musicservicelibrary.models.MusicList
import com.lk.musicservicelibrary.models.MusicMetadata
import java.lang.UnsupportedOperationException

/**
 * Erstellt von Lena am 2019-11-16.
 */
class MockPlaylistRepository: PlaylistRepository {

    private val TAG = "MockPlaylistRepository"

    override fun savePlayingQueue(playingQueue: MusicList, playingMetadata: MusicMetadata) {
        throw UnsupportedOperationException("No query in tests")
    }

    override fun restoreFirstItem(): MusicMetadata? {
        throw UnsupportedOperationException("No query in tests")
    }

    override fun restorePlayingQueue(): MusicList? {
        throw UnsupportedOperationException("No query in tests")
    }

    override fun hasPlaylist(): Boolean {
        throw UnsupportedOperationException("No query in tests")
    }

    override fun deletePlaylist() {
        throw UnsupportedOperationException("No query in tests")
    }
}