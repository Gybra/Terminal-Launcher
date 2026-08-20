package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.shell.ShellProfile
import com.gybra.terminallauncher.shell.dos.DosShellProfile
import com.gybra.terminallauncher.shell.unix.UnixShellProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class ListAppsCommandTest {
    @Test
    fun `lists installed applications the Unix way`() {
        val result = ListAppsCommand.execute(contextFor(UnixShellProfile, apps))

        assertEquals(CommandResult.Output(listOf("camera", "telegram")), result)
    }

    @Test
    fun `lists installed applications the DOS way`() {
        val result = ListAppsCommand.execute(contextFor(DosShellProfile, apps))

        assertEquals(
            CommandResult.Output(listOf("CAMERA.EXE", "TELEGRAM.EXE", "", "2 File(s)")),
            result,
        )
    }

    @Test
    fun `lists nothing when no application is installed`() {
        assertEquals(
            CommandResult.Output(emptyList()),
            ListAppsCommand.execute(contextFor(UnixShellProfile, emptyList())),
        )
    }

    @Test
    fun `ignores arguments so listing stays predictable`() {
        val context = contextFor(UnixShellProfile, apps).copy(arguments = listOf("-la"))

        assertEquals(CommandResult.Output(listOf("camera", "telegram")), ListAppsCommand.execute(context))
    }

    @Test
    fun `answers to the list applications identifier`() {
        assertEquals(Command.LIST_APPS, ListAppsCommand.id)
        assertEquals("List installed applications", ListAppsCommand.description)
    }

    private val apps = listOf(
        InstalledApp(packageName = "org.example.camera", label = "Camera"),
        InstalledApp(packageName = "org.example.telegram", label = "Telegram"),
    )

    private fun contextFor(
        shellProfile: ShellProfile,
        installedApps: List<InstalledApp>,
    ): CommandContext = CommandContext(
        arguments = emptyList(),
        shellProfile = shellProfile,
        installedApps = installedApps,
        registeredCommands = emptyList(),
    )
}
