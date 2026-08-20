package com.gybra.terminallauncher.launcher

import kotlinx.coroutines.flow.Flow

public interface PackageMonitor {
    public fun observeChanges(): Flow<PackageChange>
}
