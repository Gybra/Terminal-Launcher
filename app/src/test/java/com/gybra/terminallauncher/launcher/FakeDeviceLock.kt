package com.gybra.terminallauncher.launcher

/** A [DeviceLock] recording every call, so tests never touch device policy. */
class FakeDeviceLock(
    override var enabled: Boolean = false,
    private val requestGranted: Boolean = true,
) : DeviceLock {
    val calls: MutableList<String> = mutableListOf()

    override fun lock(): Boolean = enabled

    override fun requestEnable(): Boolean {
        calls += "requestEnable"
        enabled = requestGranted

        return requestGranted
    }

    override fun disable() {
        calls += "disable"
        enabled = false
    }
}
