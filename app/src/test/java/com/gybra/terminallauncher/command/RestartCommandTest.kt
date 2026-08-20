package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.shell.unix.UnixShellProfile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RestartCommandTest {
    @Test
    fun `asks the launcher to start again`() = runTest {
        val result = RestartCommand.execute(
            CommandContext(
                arguments = emptyList(),
                shellProfile = UnixShellProfile,
                installedApps = emptyList(),
                registeredCommands = emptyList(),
            ),
        )

        assertEquals(CommandResult.RestartLauncher, result)
    }
}
