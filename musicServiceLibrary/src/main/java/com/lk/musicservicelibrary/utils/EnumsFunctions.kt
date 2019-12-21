package com.lk.musicservicelibrary.utils

/**
 * Erstellt von Lena am 02.09.18.
 */
typealias audioFocusChanged = (old: EnumAudioFocus, new: EnumAudioFocus) -> Unit

enum class QueueType {
    QUEUE_ORDERED,
    QUEUE_SHUFFLE,
    QUEUE_ALL_SHUFFLE
}

enum class EnumAudioFocus {
    AUDIO_FOCUS,
    AUDIO_LOSS,
    AUDIO_DUCK,
    AUDIO_PAUSE_PLAYING
}

object Commands {
    const val ADD_QUEUE: String = "addQueue"
    const val ADD_ALL = "addAll"
    const val RESTORE_QUEUE = "restoreQueue"
}
