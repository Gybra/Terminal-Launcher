package com.gybra.terminallauncher.shell.dos

import com.gybra.terminallauncher.command.Command
import com.gybra.terminallauncher.command.CommandSummary
import com.gybra.terminallauncher.launcher.BatteryStatus
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.launcher.PinnedShortcut
import com.gybra.terminallauncher.shell.DosDrive
import com.gybra.terminallauncher.shell.LauncherLocation
import com.gybra.terminallauncher.shell.PromptSymbol
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
        assertEquals("C:\\HOME", profile.formatPath(contextAt(LauncherLocation.HOME)))
        assertEquals("C:\\APPS", profile.formatPath(contextAt(LauncherLocation.APPS)))
    }

    @Test
    fun `formats DOS paths on the chosen drive`() {
        val context = contextAt(LauncherLocation.HOME).copy(dosDrive = DosDrive.D)

        assertEquals("D:\\HOME>", profile.prompt(context))
        assertEquals("D:\\HOME", profile.formatPath(context))
    }

    @Test
    fun `shortens the DOS path to the drive root when the path is hidden`() {
        val context = contextAt(LauncherLocation.APPS).copy(showPath = false)

        assertEquals("C:\\>", profile.prompt(context))
        assertEquals("C:\\", profile.formatPath(context))
    }

    @Test
    fun `ignores the Unix prompt symbol`() {
        val context = contextAt(LauncherLocation.HOME).copy(promptSymbol = PromptSymbol.PERCENT)

        assertEquals("C:\\HOME>", profile.prompt(context))
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
    fun `writes command messages in uppercase`() {
        assertEquals("PINNED MAIL.EXE", profile.formatMessage("pinned mail.exe"))
        assertEquals("", profile.formatMessage(""))
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

    @Test
    fun `writes a pinned shortcut as a DOS link file`() {
        val shortcut = PinnedShortcut(
            packageName = "org.example.browser",
            id = "new-tab",
            label = "New Tab",
        )

        assertEquals("NEW TAB.LNK", profile.formatShortcutName(shortcut))
    }

    @Test
    fun `writes the battery part of the status line in upper case`() {
        val battery = BatteryStatus(percentage = 42, charging = true)

        assertEquals("42% CHARGING", profile.formatBattery(battery))
        assertEquals("42%", profile.formatBattery(battery.copy(charging = false)))
    }
}
