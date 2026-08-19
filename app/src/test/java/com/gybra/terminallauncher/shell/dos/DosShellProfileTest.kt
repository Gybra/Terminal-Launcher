package com.gybra.terminallauncher.shell.dos

import com.gybra.terminallauncher.command.Command
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.shell.LauncherLocation
import com.gybra.terminallauncher.shell.ShellContext
import com.gybra.terminallauncher.shell.ShellType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun `exposes DOS command aliases and resolves them case insensitively`() {
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
            assertEquals(command, profile.commandForAlias(alias.lowercase()))
        }
        assertNull(profile.commandForAlias("unknown"))
    }

    private fun contextAt(location: LauncherLocation): ShellContext = ShellContext(
        username = "ignored",
        hostname = "ignored",
        location = location,
    )
}
