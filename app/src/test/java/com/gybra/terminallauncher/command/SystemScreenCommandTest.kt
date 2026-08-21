package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.launcher.SystemScreen
import com.gybra.terminallauncher.shell.unix.UnixShellProfile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SystemScreenCommandTest {
    @Test
    fun `opens the destination it was registered with, whatever the arguments say`() = runTest {
        val command = SystemScreenCommand(
            id = Command.WIFI_SETTINGS,
            description = "Open Wi-Fi settings",
            screen = SystemScreen.WifiSettings,
        )

        val result = command.execute(
            CommandContext(
                arguments = listOf("ignored"),
                shellProfile = UnixShellProfile,
                installedApps = emptyList(),
                registeredCommands = emptyList(),
            ),
        )

        assertEquals(CommandResult.OpenSystemScreen(SystemScreen.WifiSettings), result)
        assertEquals(Command.WIFI_SETTINGS, command.id)
        assertEquals("Open Wi-Fi settings", command.description)
    }
}
