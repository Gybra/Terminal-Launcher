package com.gybra.terminallauncher.launcher

import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * Locks the screen through `DevicePolicyManager.lockNow()`, which Android grants only to an active
 * device admin. The user grants it and takes it back from the settings screen, so a launcher that
 * was never granted it, or a device offering no device policy at all, simply cannot lock.
 */
public class SystemDeviceLock(
    private val context: Context,
) : DeviceLock {
    private val admin = ComponentName(context, TerminalDeviceAdminReceiver::class.java)

    override val enabled: Boolean
        get() = policyManager()?.isAdminActive(admin) == true

    override fun lock(): Boolean {
        val policyManager = policyManager() ?: return false
        if (!policyManager.isAdminActive(admin)) {
            return false
        }

        return try {
            policyManager.lockNow()
            true
        } catch (_: SecurityException) {
            false
        }
    }

    override fun requestEnable(): Boolean {
        policyManager() ?: return false

        return try {
            context.startActivity(enableRequest())
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    override fun disable() {
        val policyManager = policyManager() ?: return
        if (policyManager.isAdminActive(admin)) {
            policyManager.removeActiveAdmin(admin)
        }
    }

    private fun enableRequest(): Intent =
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
            .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, EXPLANATION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun policyManager(): DevicePolicyManager? =
        context.getSystemService(DevicePolicyManager::class.java)

    private companion object {
        const val EXPLANATION =
            "Terminal Launcher locks the screen when you double tap Home. It asks for nothing else."
    }
}
