package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.shell.unix.UnixShellProfile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class SettingsCommandTest {
    @Test
    fun `asks the launcher to open the settings destination`() = runTest {
        val result = SettingsCommand.execute(
            CommandContext(
                arguments = emptyList(),
                shellProfile = UnixShellProfile,
                installedApps = emptyList(),
                registeredCommands = emptyList(),
            ),
        )

        assertSame(CommandResult.OpenSettings, result)
    }

    @Test
    fun `answers to the settings identifier`() {
        assertEquals(Command.SETTINGS, SettingsCommand.id)
        assertEquals("Open launcher settings", SettingsCommand.description)
    }
}
