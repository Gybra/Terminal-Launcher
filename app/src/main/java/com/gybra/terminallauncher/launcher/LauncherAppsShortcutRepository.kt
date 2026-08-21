package com.gybra.terminallauncher.launcher

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Process
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads the shortcuts an application publishes through `LauncherApps`, which answers only while
 * this launcher is the Home application, so a refused question is reported instead of crashing.
 */
public class LauncherAppsShortcutRepository(
    private val context: Context,
    private val readContext: CoroutineContext = Dispatchers.IO,
) : ShortcutRepository {
    override suspend fun publishedBy(packageName: String): PublishedShortcuts =
        withContext(readContext) {
            val launcherApps = context.getSystemService(LauncherApps::class.java)
                ?: return@withContext PublishedShortcuts.Refused

            try {
                PublishedShortcuts.Available(
                    launcherApps.getShortcuts(queryFor(packageName), Process.myUserHandle())
                        .orEmpty()
                        .map { shortcut -> shortcut.toAppShortcut(packageName) },
                )
            } catch (_: SecurityException) {
                PublishedShortcuts.Refused
            } catch (_: IllegalStateException) {
                PublishedShortcuts.Refused
            }
        }

    private fun queryFor(packageName: String): LauncherApps.ShortcutQuery =
        LauncherApps.ShortcutQuery()
            .setPackage(packageName)
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST,
            )
}

/** Keeps the identifier and the name Android publishes, which is all the launcher stores. */
private fun ShortcutInfo.toAppShortcut(packageName: String): AppShortcut = AppShortcut(
    packageName = packageName,
    id = id,
    label = (shortLabel ?: longLabel ?: id).toString(),
)
