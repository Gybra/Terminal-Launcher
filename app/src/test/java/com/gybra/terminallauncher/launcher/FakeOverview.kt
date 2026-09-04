package com.gybra.terminallauncher.launcher

/** An [Overview] that records every open and reports the answer it was given. */
class FakeOverview(
    private val opens: Boolean = true,
) : Overview {
    var openCount: Int = 0
        private set

    override fun open(): Boolean {
        openCount += 1
        return opens
    }
}
