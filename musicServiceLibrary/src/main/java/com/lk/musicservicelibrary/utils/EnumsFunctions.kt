package com.lk.musicservicelibrary.utils

/**
 * Erstellt von Lena am 02.09.18.
 */
typealias audioFocusChanged = (old: EnumAudioFucos, new: EnumAudioFucos) -> Unit
typealias listenerFunction<parameterType> = (parameter: parameterType) -> Unit


enum class QueueType {
    QUEUE_ORDERED,
    QUEUE_SHUFFLE,
    QUEUE_ALL_SHUFFLE
}

enum class EnumAudioFucos {

    AUDIO_FOCUS,
    AUDIO_LOSS,
    AUDIO_DUCK,
    AUDIO_PAUSE_PLAYING
}
