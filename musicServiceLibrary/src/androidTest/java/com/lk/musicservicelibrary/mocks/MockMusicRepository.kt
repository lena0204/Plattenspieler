package com.lk.musicservicelibrary.mocks

import com.lk.musicservicelibrary.models.MusicList
import com.lk.musicservicelibrary.models.MusicMetadata
import com.lk.musicservicelibrary.system.MusicDataRepository
import java.lang.UnsupportedOperationException

/**
 * Erstellt von Lena am 2019-08-18.
 */
class MockMusicRepository: MusicDataRepository {

    private val TAG = "MockMusicRepository"

    override fun queryFirstTitle(): MusicMetadata {
        throw UnsupportedOperationException("$TAG: Tests can't query")
    }

    override fun queryTitles(playingTitleId: String): MusicList {
        throw UnsupportedOperationException("$TAG: Tests can't query")
    }

    override fun queryTitlesByAlbumID(albumId: String): MusicList {
        throw UnsupportedOperationException("$TAG: Tests can't query")
    }

    override fun queryAlbums(): MusicList {
        throw UnsupportedOperationException("$TAG: Tests can't query")
    }

}