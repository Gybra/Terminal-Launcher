package com.gybra.terminallauncher.launcher

import android.content.Context
import android.os.BatteryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads the battery through `BatteryManager`, which needs no permission. A device without the
 * service, or reporting a level outside 0 to 100, reads as unavailable rather than as a number
 * the launcher made up.
 */
public class SystemBatteryRepository(
    private val context: Context,
) : BatteryRepository {
    override suspend fun readStatus(): BatteryStatus? = withContext(Dispatchers.IO) {
        val batteryManager = context.getSystemService(BatteryManager::class.java)
            ?: return@withContext null
        val percentage = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        if (percentage !in FULL_RANGE) {
            return@withContext null
        }

        BatteryStatus(percentage = percentage, charging = batteryManager.isCharging)
    }

    private companion object {
        val FULL_RANGE = 0..100
    }
}
