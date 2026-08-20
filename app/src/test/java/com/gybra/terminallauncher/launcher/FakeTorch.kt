package com.gybra.terminallauncher.launcher

/** A [Torch] that records every toggle and reports the states it was given, in order. */
class FakeTorch(
    private vararg val states: TorchState,
) : Torch {
    var toggles: Int = 0
        private set

    override suspend fun toggle(): TorchState {
        val state = states.getOrElse(toggles) { states.lastOrNull() ?: TorchState.UNAVAILABLE }
        toggles += 1

        return state
    }
}
