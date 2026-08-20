package com.gybra.terminallauncher.shell.unix

import com.gybra.terminallauncher.command.Command
import com.gybra.terminallauncher.command.CommandSummary
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.shell.DosDrive
import com.gybra.terminallauncher.shell.LauncherLocation
import com.gybra.terminallauncher.shell.PromptSymbol
import com.gybra.terminallauncher.shell.ShellContext
import com.gybra.terminallauncher.shell.ShellType
import org.junit.Assert.assertEquals
import org.junit.Test

class UnixShellProfileTest {
    private val profile = UnixShellProfile

    @Test
    fun `formats Unix prompts using identity and location`() {
        assertEquals("oreste@android:~$", profile.prompt(contextAt(LauncherLocation.HOME)))
        assertEquals("oreste@android:~/apps$", profile.prompt(contextAt(LauncherLocation.APPS)))
    }

    @Test
    fun `formats Unix paths independently from prompts`() {
        assertEquals("~", profile.formatPath(contextAt(LauncherLocation.HOME)))
        assertEquals("~/apps", profile.formatPath(contextAt(LauncherLocation.APPS)))
    }

    @Test
    fun `ends the Unix prompt with the chosen symbol`() {
        PromptSymbol.entries.forEach { symbol ->
            assertEquals(
                "oreste@android:~${symbol.text}",
                profile.prompt(contextAt(LauncherLocation.HOME).copy(promptSymbol = symbol)),
            )
        }
    }

    @Test
    fun `drops the path from the Unix prompt when it is hidden`() {
        val context = contextAt(LauncherLocation.APPS).copy(showPath = false)

        assertEquals("oreste@android$", profile.prompt(context))
    }

    @Test
    fun `keeps the Unix path readable while the prompt hides it`() {
        val context = contextAt(LauncherLocation.APPS).copy(showPath = false)

        assertEquals("~/apps", profile.formatPath(context))
    }

    @Test
    fun `ignores the DOS drive`() {
        val context = contextAt(LauncherLocation.HOME).copy(dosDrive = DosDrive.D)

        assertEquals("oreste@android:~$", profile.prompt(context))
    }

    @Test
    fun `formats application labels as lowercase names`() {
        val app = InstalledApp(packageName = "org.example.telegram", label = "Telegram X")

        assertEquals("telegram x", profile.formatAppName(app))
    }

    @Test
    fun `formats application lists without DOS metadata`() {
        val apps = listOf(
            InstalledApp(packageName = "org.example.camera", label = "Camera"),
            InstalledApp(packageName = "org.example.telegram", label = "Telegram"),
        )

        assertEquals(listOf("camera", "telegram"), profile.formatAppList(apps))
        assertEquals(emptyList<String>(), profile.formatAppList(emptyList()))
    }

    @Test
    fun `writes command messages in lowercase`() {
        assertEquals("pinned mail", profile.formatMessage("Pinned Mail"))
        assertEquals("", profile.formatMessage(""))
    }

    @Test
    fun `formats help from command metadata using primary Unix aliases`() {
        val commands = listOf(
            CommandSummary(id = Command.LIST_APPS, description = "List installed applications"),
            CommandSummary(id = Command.HELP, description = "Show available commands"),
        )

        assertEquals(
            listOf(
                "ls        List installed applications",
                "help      Show available commands",
            ),
            profile.formatHelp(commands),
        )
        assertEquals(emptyList<String>(), profile.formatHelp(emptyList()))
    }

    @Test
    fun `exposes Unix aliases while accepting optional DOS aliases`() {
        val expectedAliases = mapOf(
            Command.LIST_APPS to "ls",
            Command.CLEAR to "clear",
            Command.HELP to "help",
            Command.PIN to "pin",
            Command.UNPIN to "unpin",
            Command.SETTINGS to "settings",
        )

        assertEquals(ShellType.UNIX, profile.type)
        expectedAliases.forEach { (command, alias) ->
            assertEquals(alias, profile.aliasFor(command))
        }
        assertEquals(setOf("ls", "dir"), profile.aliasesFor(Command.LIST_APPS))
        assertEquals(setOf("clear", "cls"), profile.aliasesFor(Command.CLEAR))
        assertEquals(setOf("help"), profile.aliasesFor(Command.HELP))
    }

    private fun contextAt(location: LauncherLocation): ShellContext = ShellContext(
        username = "oreste",
        hostname = "android",
        location = location,
    )
}
