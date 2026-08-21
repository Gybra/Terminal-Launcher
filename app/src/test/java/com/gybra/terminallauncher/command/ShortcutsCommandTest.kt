package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.launcher.AppShortcut
import com.gybra.terminallauncher.launcher.FakeShortcutRepository
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.launcher.PublishedShortcuts
import com.gybra.terminallauncher.preferences.LauncherPreferences
import com.gybra.terminallauncher.preferences.RecordingPreferencesRepository
import com.gybra.terminallauncher.shell.unix.UnixShellProfile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ShortcutsCommandTest {
    private val browser = InstalledApp(packageName = "org.example.browser", label = "Browser")
    private val newTab = AppShortcut(packageName = browser.packageName, id = "new-tab", label = "New tab")
    private val newWindow =
        AppShortcut(packageName = browser.packageName, id = "new-window", label = "New window")

    @Test
    fun `answers a missing argument with the usage of the running shell`() = runTest {
        val result = command().execute(context(arguments = emptyList()))

        assertEquals(
            CommandResult.Output(
                listOf(
                    "usage: shortcuts <application>",
                    "       shortcuts pin <application> <shortcut>",
                    "       shortcuts unpin <application> <shortcut>",
                ),
            ),
            result,
        )
    }

    @Test
    fun `lists the shortcuts an application publishes`() = runTest {
        val command = command(published = mapOf(browser.packageName to available(newTab, newWindow)))

        val result = command.execute(context(arguments = listOf("browser")))

        assertEquals(
            CommandResult.Listing(lines = emptyList(), shortcuts = listOf(newTab, newWindow)),
            result,
        )
    }

    @Test
    fun `reports an application publishing no shortcut`() = runTest {
        val result = command().execute(context(arguments = listOf("browser")))

        assertEquals(CommandResult.Output(listOf("browser publishes no shortcuts")), result)
    }

    @Test
    fun `reports Android refusing the shortcuts`() = runTest {
        val command = command(published = mapOf(browser.packageName to PublishedShortcuts.Refused))

        val result = command.execute(context(arguments = listOf("browser")))

        assertEquals(
            CommandResult.Output(
                listOf("android lists shortcuts only while terminal launcher is the home application"),
            ),
            result,
        )
    }

    @Test
    fun `answers an application matching nothing`() = runTest {
        val result = command().execute(context(arguments = listOf("ledger")))

        assertEquals(
            CommandResult.Listing(
                lines = listOf("no application matches ledger"),
                apps = emptyList(),
            ),
            result,
        )
    }

    @Test
    fun `pins the shortcut named after the application`() = runTest {
        val preferencesRepository = RecordingPreferencesRepository()
        val command = command(
            published = mapOf(browser.packageName to available(newTab, newWindow)),
            preferencesRepository = preferencesRepository,
        )

        val result = command.execute(context(arguments = listOf("pin", "browser", "new", "tab")))

        assertEquals(CommandResult.Output(listOf("pinned new tab")), result)
        assertEquals(
            listOf("pinShortcut(${newTab.packageName}, ${newTab.id})"),
            preferencesRepository.writes,
        )
    }

    @Test
    fun `answers a shortcut name matching more than one shortcut`() = runTest {
        val command = command(published = mapOf(browser.packageName to available(newTab, newWindow)))

        val result = command.execute(context(arguments = listOf("pin", "browser", "new")))

        assertEquals(
            CommandResult.Listing(
                lines = listOf("new matches more than one shortcut"),
                shortcuts = listOf(newTab, newWindow),
            ),
            result,
        )
    }

    @Test
    fun `answers a shortcut name matching nothing`() = runTest {
        val command = command(published = mapOf(browser.packageName to available(newTab)))

        val result = command.execute(context(arguments = listOf("pin", "browser", "downloads")))

        assertEquals(CommandResult.Output(listOf("no shortcut matches downloads")), result)
    }

    @Test
    fun `answers a pin without a shortcut name with the usage`() = runTest {
        val result = command().execute(context(arguments = listOf("pin", "browser")))

        assertEquals("usage: shortcuts <application>", outputLines(result).first())
    }

    @Test
    fun `reports Android refusing the shortcuts asked to pin one`() = runTest {
        val command = command(published = mapOf(browser.packageName to PublishedShortcuts.Refused))

        val result = command.execute(context(arguments = listOf("pin", "browser", "new tab")))

        assertEquals(
            CommandResult.Output(
                listOf("android lists shortcuts only while terminal launcher is the home application"),
            ),
            result,
        )
    }

    @Test
    fun `removes a pinned shortcut`() = runTest {
        val preferencesRepository = RecordingPreferencesRepository(
            LauncherPreferences(pinnedShortcuts = listOf(newTab, newWindow)),
        )
        val command = command(preferencesRepository = preferencesRepository)

        val result = command.execute(context(arguments = listOf("unpin", "browser", "new", "window")))

        assertEquals(CommandResult.Output(listOf("unpinned new window")), result)
        assertEquals(
            listOf("unpinShortcut(${newWindow.packageName}, ${newWindow.id})"),
            preferencesRepository.writes,
        )
    }

    @Test
    fun `answers a shortcut the application never pinned`() = runTest {
        val preferencesRepository = RecordingPreferencesRepository(
            LauncherPreferences(pinnedShortcuts = listOf(newTab)),
        )
        val command = command(preferencesRepository = preferencesRepository)

        val result = command.execute(context(arguments = listOf("unpin", "browser", "new window")))

        assertEquals(CommandResult.Output(listOf("no shortcut matches new window")), result)
        assertEquals(emptyList<String>(), preferencesRepository.writes)
    }

    @Test
    fun `answers an unpin without a shortcut name with the usage`() = runTest {
        val result = command().execute(context(arguments = listOf("unpin")))

        assertEquals("usage: shortcuts <application>", outputLines(result).first())
    }

    @Test
    fun `answers to the shortcuts identifier`() {
        assertEquals(Command.SHORTCUTS, command().id)
        assertEquals("Manage app shortcuts", command().description)
    }

    private fun available(vararg shortcuts: AppShortcut): PublishedShortcuts =
        PublishedShortcuts.Available(shortcuts.toList())

    private fun command(
        published: Map<String, PublishedShortcuts> = emptyMap(),
        preferencesRepository: RecordingPreferencesRepository = RecordingPreferencesRepository(),
    ): ShortcutsCommand = ShortcutsCommand(
        shortcutRepository = FakeShortcutRepository(published = published),
        preferencesRepository = preferencesRepository,
    )

    private fun context(arguments: List<String>): CommandContext = CommandContext(
        arguments = arguments,
        shellProfile = UnixShellProfile,
        installedApps = listOf(browser),
        registeredCommands = emptyList(),
    )

    private fun outputLines(result: CommandResult): List<String> =
        (result as CommandResult.Output).lines
}
