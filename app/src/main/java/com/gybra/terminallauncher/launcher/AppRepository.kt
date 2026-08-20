package com.gybra.terminallauncher.launcher

import kotlinx.coroutines.flow.Flow

public interface AppRepository {
    public suspend fun getInstalledApps(): List<InstalledApp>

    public fun observeInstalledApps(): Flow<List<InstalledApp>>
}
