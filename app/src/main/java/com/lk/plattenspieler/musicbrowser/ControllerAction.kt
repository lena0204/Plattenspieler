package com.lk.plattenspieler.musicbrowser

import android.os.Bundle

/**
 * Erstellt von Lena am 09.09.18.
 */
class ControllerAction (
        val action: EnumActions,
        val titleId: String = "",
        val args: Bundle = Bundle()
)