package com.gybra.terminallauncher.launcher

/**
 * A system destination the launcher can ask Android to open. Every entry names one documented
 * Android action, so no Intent is ever built from prompt input, and the two entries carrying a
 * package name only ever carry one discovered through `PackageManager`.
 */
public sealed interface SystemScreen {
    /** The Android settings. */
    public object AndroidSettings : SystemScreen

    /** The Wi-Fi settings. */
    public object WifiSettings : SystemScreen

    /** The Bluetooth settings. */
    public object BluetoothSettings : SystemScreen

    /** The Android details of [packageName], where its permissions and storage live. */
    public data class ApplicationDetails(public val packageName: String) : SystemScreen

    /** The Android uninstall confirmation for [packageName]. Android asks, the launcher never does. */
    public data class Uninstall(public val packageName: String) : SystemScreen
}
