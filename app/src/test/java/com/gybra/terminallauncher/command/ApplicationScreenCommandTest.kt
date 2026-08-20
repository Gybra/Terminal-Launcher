package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.launcher.SystemScreen
import com.gybra.terminallauncher.shell.ShellProfile
import com.gybra.terminallauncher.shell.dos.DosShellProfile
import com.gybra.terminallauncher.shell.unix.UnixShellProfile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** The two commands that resolve an application and hand it to a system destination. */
class ApplicationScreenCommandTest {
    @Test
    fun `opens the Android details of the resolved application`() = runTest {
        val result = AppInfoCommand.execute(contextFor(UnixShellProfile, listOf("camera")))

        assertEquals(
            CommandResult.OpenSystemScreen(
                SystemScreen.ApplicationDetails("org.example.camera"),
            ),
            result,
        )
    }

    @Test
    fun `asks Android to uninstall the resolved application`() = runTest {
        val result = UninstallCommand.execute(contextFor(UnixShellProfile, listOf("camera")))

        assertEquals(
            CommandResult.OpenSystemScreen(SystemScreen.Uninstall("org.example.camera")),
            result,
        )
    }

    @Test
    fun `lists ambiguous matches instead of uninstalling one of them`() = runTest {
        val result = UninstallCommand.execute(contextFor(UnixShellProfile, listOf("mail")))

        assertEquals(
            CommandResult.Output(
                listOf("mail matches more than one application", "mail archive", "mail assistant"),
            ),
            result,
        )
    }

    @Test
    fun `reports an argument matching no application`() = runTest {
        val result = AppInfoCommand.execute(contextFor(UnixShellProfile, listOf("telegram")))

        assertEquals(CommandResult.Output(listOf("no application matches telegram")), result)
    }

    @Test
    fun `reports its usage when no argument is given`() = runTest {
        assertEquals(
            CommandResult.Output(listOf("usage: info <application>")),
            AppInfoCommand.execute(contextFor(UnixShellProfile, emptyList())),
        )
        assertEquals(
            CommandResult.Output(listOf("USAGE: UNINSTALL <APPLICATION>")),
            UninstallCommand.execute(contextFor(DosShellProfile, listOf("  "))),
        )
    }

    @Test
    fun `describes itself for help`() {
        assertEquals(Command.APP_INFO, AppInfoCommand.id)
        assertEquals("Open the Android details of an application", AppInfoCommand.description)
        assertEquals(Command.UNINSTALL, UninstallCommand.id)
        assertEquals("Ask Android to uninstall an application", UninstallCommand.description)
    }

    private fun contextFor(
        shellProfile: ShellProfile,
        arguments: List<String>,
    ): CommandContext = CommandContext(
        arguments = arguments,
        shellProfile = shellProfile,
        installedApps = listOf(
            InstalledApp(packageName = "org.example.camera", label = "Camera"),
            InstalledApp(packageName = "org.example.archive", label = "Mail Archive"),
            InstalledApp(packageName = "org.example.assistant", label = "Mail Assistant"),
        ),
        registeredCommands = emptyList(),
    )
}
