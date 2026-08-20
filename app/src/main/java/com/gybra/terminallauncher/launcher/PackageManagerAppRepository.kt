package com.gybra.terminallauncher.launcher

import android.content.Intent
import android.content.pm.PackageManager
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

public class PackageManagerAppRepository(
    private val packageManager: PackageManager,
    private val launcherPackageName: String,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AppRepository {
    override suspend fun getInstalledApps(): List<InstalledApp> =
        withContext(backgroundDispatcher) {
            queryLaunchableApps()
        }

    override fun observeInstalledApps(): Flow<List<InstalledApp>> = flow {
        emit(getInstalledApps())
    }

    @Suppress("DEPRECATION")
    private fun queryLaunchableApps(): List<InstalledApp> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        return packageManager
            .queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
            .mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                val label = resolveInfo.loadLabel(packageManager).toString().trim()

                if (packageName.isBlank() || packageName == launcherPackageName || label.isBlank()) {
                    null
                } else {
                    InstalledApp(packageName = packageName, label = label)
                }
            }
            .distinctBy(InstalledApp::packageName)
            .sortedWith(
                compareBy<InstalledApp> { app -> app.label.lowercase(Locale.ROOT) }
                    .thenBy(InstalledApp::packageName),
            )
    }
}
