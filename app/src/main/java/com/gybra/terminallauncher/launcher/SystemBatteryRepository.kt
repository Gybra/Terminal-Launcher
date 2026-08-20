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
     * Reads the battery once when collection starts and again on every battery broadcast. The
     * broadcast carries a reading of its own, but it is used as a signal only, so the level always
     * comes from the single reading in [readStatus]. The device publishes it far more often than
     * the level changes, so equal readings are reported once.
     */
    override fun observeStatus(): Flow<BatteryStatus?> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(Unit)
            }
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        trySend(Unit)
        awaitClose { context.unregisterReceiver(receiver) }
    }
        .map { readStatus() }
        .distinctUntilChanged()

    private companion object {
        val FULL_RANGE = 0..100
    }
}
