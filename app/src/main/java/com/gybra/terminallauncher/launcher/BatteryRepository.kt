package com.gybra.terminallauncher.launcher

public interface BatteryRepository {
    /** Reads the current battery status, or `null` on a device that does not report one. */
    public suspend fun readStatus(): BatteryStatus?
}
