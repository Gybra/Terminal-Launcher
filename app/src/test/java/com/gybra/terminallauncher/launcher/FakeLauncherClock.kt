package com.gybra.terminallauncher.launcher

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** A [LauncherClock] stuck at a fixed time, so tests stay deterministic. */
class FakeLauncherClock(
    private val time: String = "22:10",
    private val epochMillis: Long = 1_700_000_000_000L,
) : LauncherClock {
    override fun observeTime(): Flow<String> = flowOf(time)

    override fun now(): Long = epochMillis
}
