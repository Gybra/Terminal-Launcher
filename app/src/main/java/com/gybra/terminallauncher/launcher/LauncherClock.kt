package com.gybra.terminallauncher.launcher

import kotlinx.coroutines.flow.Flow

public interface LauncherClock {
    public fun observeTime(): Flow<String>
}
