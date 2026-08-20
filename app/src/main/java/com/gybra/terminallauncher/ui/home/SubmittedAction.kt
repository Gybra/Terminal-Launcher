package com.gybra.terminallauncher.ui.home

import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.launcher.SystemScreen

/** What the launcher must do once a submitted prompt line has been consumed. */
public sealed interface SubmittedAction {
    /** Launch [app], which an unambiguous query resolved to. */
    public data class LaunchApp(public val app: InstalledApp) : SubmittedAction

    /** Open the settings destination. */
    public object OpenSettings : SubmittedAction

    /** Ask Android to open [screen]. */
    public data class OpenSystemScreen(public val screen: SystemScreen) : SubmittedAction

    /** Start the launcher again, so Home is rebuilt from the stored preferences. */
    public object RestartLauncher : SubmittedAction
}
