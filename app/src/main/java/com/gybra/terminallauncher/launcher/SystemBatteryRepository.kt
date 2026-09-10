package com.gybra.terminallauncher.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Reads the battery through `BatteryManager`, which needs no permission. A device without the
 * service, or reporting a level outside 0 to 100, reads as unavailable rather than as a number
 * the launcher made up.
 */
public class SystemBatteryRepository(
    private val context: Context,
    private val readContext: CoroutineContext = Dispatchers.IO,
) : BatteryRepository {
    override suspend fun readStatus(): BatteryStatus? = withContext(readContext) {
        val batteryManager = context.getSystemService(BatteryManager::class.java)
            ?: return@withContext null
        val percentage = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        if (percentage !in FULL_RANGE) {
            return@withContext null
        }

        BatteryStatus(percentage = percentage, charging = batteryManager.isCharging)
    }

    /**
     * Reads the battery once when collection starts, then from each battery broadcast's extras.
     * A broadcast without extras falls back to [readStatus]. Equal readings are reported once.
     */
    override fun observeStatus(): Flow<BatteryStatus?> = callbackFlow<Intent?> {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(intent)
            }
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        trySend(null)
        awaitClose { context.unregisterReceiver(receiver) }
    }
        .map { intent -> intent?.toStatus() ?: readStatus() }
        .distinctUntilChanged()

    private fun Intent.toStatus(): BatteryStatus? {
        val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = getIntExtra(BatteryManager.EXTRA_SCALE, 0)
        if (level < 0 || scale <= 0) {
            return null
        }
        val percentage = (level * 100) / scale
        if (percentage !in FULL_RANGE) {
            return null
        }
        val charging = getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
        return BatteryStatus(percentage = percentage, charging = charging)
    }

    private companion object {
        val FULL_RANGE = 0..100
    }
}
