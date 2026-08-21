package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.launcher.FakeBatteryRepository
import com.gybra.terminallauncher.launcher.FakeShortcutRepository
import com.gybra.terminallauncher.launcher.FakeTorch
import com.gybra.terminallauncher.launcher.TorchState
import com.gybra.terminallauncher.preferences.RecordingPreferencesRepository
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherCommandsTest {
    @Test
    fun `keeps every command description inside a phone line`() {
        registeredCommands().forEach { command ->
            assertTrue(
                "${command.id} is described in ${command.description.length} characters",
                command.description.length <= MAX_DESCRIPTION_LENGTH,
            )
        }
    }

    private fun registeredCommands(): List<LauncherCommand> = launcherCommands(
        preferencesRepository = RecordingPreferencesRepository(),
        batteryRepository = FakeBatteryRepository(),
        shortcutRepository = FakeShortcutRepository(),
        torch = FakeTorch(TorchState.ON),
    )
}

/**
 * How long a description may be. A phone line holds around thirty-three monospace characters at
 * the size Home writes, and `help` indents every description by ten, so what is left is what fits
 * before `BasicText` wraps the line and breaks the two columns apart.
 */
private const val MAX_DESCRIPTION_LENGTH = 23
