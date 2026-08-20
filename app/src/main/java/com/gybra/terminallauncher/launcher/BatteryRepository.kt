package com.gybra.terminallauncher.launcher

import kotlinx.coroutines.flow.Flow

public interface BatteryRepository {
    /** Reads the current battery status, or `null` on a device that does not report one. */
    public suspend fun readStatus(): BatteryStatus?

    /**
     * Reports the battery status and every change of it, so Home follows the device instead of
     * reading it once. A device reporting no status is reported as `null`, like [readStatus].
     */
    public fun observeStatus(): Flow<BatteryStatus?>
}
