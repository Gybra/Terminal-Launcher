package com.gybra.terminallauncher.launcher

import kotlinx.coroutines.flow.Flow

public interface AppRepository {
    public suspend fun getInstalledApps(): List<InstalledApp>

    public suspend fun findApp(query: String): InstalledApp?

    public fun observeInstalledApps(): Flow<List<InstalledApp>>
}
