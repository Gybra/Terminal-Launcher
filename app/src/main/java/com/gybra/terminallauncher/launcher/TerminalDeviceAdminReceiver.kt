package com.gybra.terminallauncher.launcher

import android.app.admin.DeviceAdminReceiver

/**
 * The device admin the launcher registers to lock the screen. It carries no behavior of its own:
 * Android needs a receiver to name in the policy, and the policy declares locking the screen and
 * nothing else.
 */
public class TerminalDeviceAdminReceiver : DeviceAdminReceiver()
