package com.gybra.terminallauncher.shell.dos

import com.gybra.terminallauncher.command.Command
import com.gybra.terminallauncher.command.CommandSummary
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.shell.LauncherLocation
import com.gybra.terminallauncher.shell.ShellContext
import com.gybra.terminallauncher.shell.ShellType
import org.junit.Assert.assertEquals
import org.junit.Test

class DosShellProfileTest {
    private val profile = DosShellProfile

    @Test
    fun `formats DOS prompts for every location`() {
        assertEquals("C:\\HOME>", profile.prompt(contextAt(LauncherLocation.HOME)))
        assertEquals("C:\\APPS>", profile.prompt(contextAt(LauncherLocation.APPS)))
    }

    @Test
    fun `formats DOS paths independently from prompts`() {
        assertEquals("C:\\HOME", profile.formatPath(LauncherLocation.HOME))
        assertEquals("C:\\APPS", profile.formatPath(LauncherLocation.APPS))
    }

    @Test
    fun `formats application labels as decorative executables`() {
        val app = InstalledApp(packageName = "org.example.telegram", label = "Telegram")

        assertEquals("TELEGRAM.EXE", profile.formatAppName(app))
    }

    @Test
    fun `formats application lists with a DOS file count`() {
        val apps = listOf(
            InstalledApp(packageName = "org.example.camera", label = "Camera"),
            InstalledApp(packageName = "org.example.telegram", label = "Telegram"),
        )

        assertEquals(
            listOf("CAMERA.EXE", "TELEGRAM.EXE", "", "2 File(s)"),
            profile.formatAppList(apps),
        )
        assertEquals(listOf("", "0 File(s)"), profile.formatAppList(emptyList()))
    }

    @Test
    fun `formats help from command metadata with a DOS command count`() {
        val commands = listOf(
            CommandSummary(id = Command.LIST_APPS, description = "List installed applications"),
            CommandSummary(id = Command.HELP, description = "Show available commands"),
        )

        assertEquals(
            listOf(
                "DIR       List installed applications",
                "HELP      Show available commands",
                "",
                "2 Command(s)",
            ),
            profile.formatHelp(commands),
        )
        assertEquals(listOf("", "0 Command(s)"), profile.formatHelp(emptyList()))
    }

    @Test
    fun `exposes DOS command aliases`() {
        val expectedAliases = mapOf(
            Command.LIST_APPS to "DIR",
            Command.CLEAR to "CLS",
            Command.HELP to "HELP",
            Command.PIN to "PIN",
            Command.UNPIN to "UNPIN",
            Command.SETTINGS to "SETTINGS",
        )

        assertEquals(ShellType.DOS, profile.type)
        expectedAliases.forEach { (command, alias) ->
            assertEquals(alias, profile.aliasFor(command))
            assertEquals(setOf(alias), profile.aliasesFor(command))
        }
    }

    private fun contextAt(location: LauncherLocation): ShellContext = ShellContext(
        username = "ignored",
        hostname = "ignored",
        location = location,
    )
}
