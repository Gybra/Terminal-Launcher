package com.gybra.terminallauncher.launcher

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** An [AppRepository] answering with the applications the test installed, or failing instead. */
class FakeAppRepository(
    private val apps: List<InstalledApp> = emptyList(),
    private val failure: Throwable? = null,
) : AppRepository {
    override suspend fun getInstalledApps(): List<InstalledApp> {
        failure?.let { repositoryFailure -> throw repositoryFailure }
        return apps
    }

    override fun observeInstalledApps(): Flow<List<InstalledApp>> = flow {
        failure?.let { repositoryFailure -> throw repositoryFailure }
        emit(apps)
    }
}
