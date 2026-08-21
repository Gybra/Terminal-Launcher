package com.gybra.terminallauncher.launcher

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/**
 * The accessibility service the launcher needs to lock the screen the way the power button does.
 * It reads no accessibility event and performs no gesture: locking is all it is for.
 */
public class TerminalAccessibilityService : AccessibilityService() {
    /** Android connects the service once the user turns it on, and never before. */
    public override fun onServiceConnected() {
        super.onServiceConnected()
        connected = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        connected = null

        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?): Unit = Unit

    override fun onInterrupt(): Unit = Unit

    internal companion object {
        /**
         * The connected service, which Android alone creates and destroys, so this is the only
         * handle the launcher can lock the screen through. `null` means the user keeps it off.
         */
        @Volatile
        internal var connected: TerminalAccessibilityService? = null
    }
}
