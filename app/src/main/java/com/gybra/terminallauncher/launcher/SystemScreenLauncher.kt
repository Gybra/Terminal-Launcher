package com.gybra.terminallauncher.launcher

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Opens the system destinations the launcher knows by name. Each one maps to a documented Android
 * action, and a device without that destination is left alone rather than crashed.
 */
public class SystemScreenLauncher(
    private val context: Context,
) {
    public fun open(screen: SystemScreen) {
        try {
            context.startActivity(screen.toIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            // A device without this destination keeps the prompt working.
        } catch (_: SecurityException) {
            // Android may refuse the destination; refusing is not a crash.
        }
    }

    private fun SystemScreen.toIntent(): Intent = when (this) {
        SystemScreen.AndroidSettings -> Intent(Settings.ACTION_SETTINGS)
        SystemScreen.WifiSettings -> Intent(Settings.ACTION_WIFI_SETTINGS)
        SystemScreen.BluetoothSettings -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        is SystemScreen.ApplicationDetails -> Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            packageName.toPackageUri(),
        )
        is SystemScreen.Uninstall -> Intent(Intent.ACTION_DELETE, packageName.toPackageUri())
    }

    private fun String.toPackageUri(): Uri = Uri.fromParts("package", this, null)
}
