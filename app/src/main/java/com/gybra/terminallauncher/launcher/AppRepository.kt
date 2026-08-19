package com.gybra.terminallauncher.launcher

import kotlinx.coroutines.flow.Flow

interface AppRepository {
    suspend fun getInstalledApps(): List<InstalledApp>

    suspend fun findApp(query: String): InstalledApp?

    fun observeInstalledApps(): Flow<List<InstalledApp>>
}

