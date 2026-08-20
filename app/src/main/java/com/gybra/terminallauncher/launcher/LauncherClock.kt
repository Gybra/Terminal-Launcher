package com.gybra.terminallauncher.launcher

import kotlinx.coroutines.flow.Flow

public interface LauncherClock {
    public fun observeTime(): Flow<String>

    /** Epoch milliseconds, used to stamp the moment an application is launched. */
    public fun now(): Long
}
