package com.gybra.terminallauncher.ui

import androidx.compose.ui.platform.SoftwareKeyboardController

/** The keyboard a test reads in place of the system one, so a screen can be asked whether it is up. */
internal class RecordingKeyboardController : SoftwareKeyboardController {
    var visible: Boolean = false
        private set

    override fun show() {
        visible = true
    }

    override fun hide() {
        visible = false
    }
}
