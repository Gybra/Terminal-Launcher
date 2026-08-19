package com.gybra.terminallauncher.launcher

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

class AppLauncher(
    private val context: Context,
) {
    fun launch(app: InstalledApp): Boolean {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                ?: return false
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
