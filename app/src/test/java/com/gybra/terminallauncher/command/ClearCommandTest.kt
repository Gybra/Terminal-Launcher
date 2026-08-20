package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.shell.unix.UnixShellProfile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ClearCommandTest {
    @Test
    fun `asks the launcher to erase the terminal history`() = runTest {
        val result = ClearCommand.execute(
            CommandContext(
                arguments = emptyList(),
                shellProfile = UnixShellProfile,
                installedApps = listOf(InstalledApp(packageName = "com.example.mail", label = "Mail")),
                registeredCommands = emptyList(),
            ),
        )

        assertSame(CommandResult.ClearHistory, result)
    }

    @Test
    fun `answers to the clear identifier`() {
        assertEquals(Command.CLEAR, ClearCommand.id)
        assertEquals("Clear the terminal history", ClearCommand.description)
    }
}
