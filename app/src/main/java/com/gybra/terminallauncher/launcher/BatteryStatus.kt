package com.gybra.terminallauncher.launcher

/** What the device reports about its battery: a [percentage] of 0 to 100, and whether it charges. */
public data class BatteryStatus(
    public val percentage: Int,
    public val charging: Boolean,
)
