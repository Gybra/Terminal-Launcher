package com.gybra.terminallauncher.launcher

public interface Torch {
    /** Turns the torch on when it is off and off when it is on, reporting where it ended up. */
    public suspend fun toggle(): TorchState
}

/** What the torch does after a toggle, including the device that cannot offer one. */
public enum class TorchState {
    ON,
    OFF,
    UNAVAILABLE,
}
