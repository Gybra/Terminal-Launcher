package com.gybra.terminallauncher.launcher

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/** A [PackageMonitor] the test drives, reporting only the changes it emits. */
class FakePackageMonitor : PackageMonitor {
    private val changes = MutableSharedFlow<PackageChange>()

    override fun observeChanges(): Flow<PackageChange> = changes

    suspend fun report(change: PackageChange) {
        changes.emit(change)
    }
}
