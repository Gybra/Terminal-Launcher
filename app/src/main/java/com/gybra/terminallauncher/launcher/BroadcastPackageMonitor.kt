package com.gybra.terminallauncher.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Reports package changes from the system broadcasts. The receiver is registered while the flow is
 * collected and unregistered as soon as collection ends, so it never outlives its collector.
 */
public class BroadcastPackageMonitor(
    private val context: Context,
) : PackageMonitor {
    override fun observeChanges(): Flow<PackageChange> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                intent.toPackageChange()?.let(::trySend)
            }
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            packageFilter(),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        awaitClose { context.unregisterReceiver(receiver) }
    }

    private fun packageFilter(): IntentFilter = IntentFilter().apply {
        addAction(Intent.ACTION_PACKAGE_ADDED)
        addAction(Intent.ACTION_PACKAGE_REMOVED)
        addAction(Intent.ACTION_PACKAGE_CHANGED)
        addDataScheme("package")
    }

    private fun Intent.toPackageChange(): PackageChange? {
        val packageName = data?.schemeSpecificPart?.takeIf(String::isNotBlank) ?: return null

        return PackageChange(
            packageName = packageName,
            removed = action == Intent.ACTION_PACKAGE_REMOVED &&
                !getBooleanExtra(Intent.EXTRA_REPLACING, false),
        )
    }
}
