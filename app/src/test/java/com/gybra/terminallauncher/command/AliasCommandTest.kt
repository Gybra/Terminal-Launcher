package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.preferences.RecordingPreferencesRepository
import com.gybra.terminallauncher.shell.ShellProfile
import com.gybra.terminallauncher.shell.dos.DosShellProfile
import com.gybra.terminallauncher.shell.unix.UnixShellProfile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AliasCommandTest {
    @Test
    fun `names an application so the prompt can launch it`() = runTest {
        val preferencesRepository = RecordingPreferencesRepository()
        val command = AliasCommand(preferencesRepository)

        val result = command.execute(contextFor(UnixShellProfile, listOf("browser", "firefox")))

        assertEquals(CommandResult.Output(listOf("aliased browser to firefox")), result)
        assertEquals(listOf("setAlias(browser, org.example.firefox)"), preferencesRepository.writes)
    }

    @Test
    fun `keeps alias names independent from how they were typed`() = runTest {
        val preferencesRepository = RecordingPreferencesRepository()
        val command = AliasCommand(preferencesRepository)

        command.execute(contextFor(UnixShellProfile, listOf("Browser", "firefox")))

        assertEquals(listOf("setAlias(browser, org.example.firefox)"), preferencesRepository.writes)
    }

    @Test
    fun `names an application whose name takes several words`() = runTest {
        val preferencesRepository = RecordingPreferencesRepository()
        val command = AliasCommand(preferencesRepository)

        val result = command.execute(contextFor(UnixShellProfile, listOf("mail", "mail", "archive")))

        assertEquals(CommandResult.Output(listOf("aliased mail to mail archive")), result)
        assertEquals(listOf("setAlias(mail, org.example.archive)"), preferencesRepository.writes)
    }

    @Test
    fun `refuses to shadow a command name of any shell`() = runTest {
        val preferencesRepository = RecordingPreferencesRepository()
        val command = AliasCommand(preferencesRepository)

        assertEquals(
            CommandResult.Output(listOf("ls is a command name")),
            command.execute(contextFor(UnixShellProfile, listOf("ls", "firefox"))),
        )
        assertEquals(
            CommandResult.Output(listOf("cls is a command name")),
            command.execute(contextFor(UnixShellProfile, listOf("CLS", "firefox"))),
        )
        assertEquals(
            CommandResult.Output(listOf("DIR IS A COMMAND NAME")),
            command.execute(contextFor(DosShellProfile, listOf("dir", "firefox"))),
        )
        assertEquals(emptyList<String>(), preferencesRepository.writes)
    }

    @Test
    fun `reports its usage when a name or an application is missing`() = runTest {
        val preferencesRepository = RecordingPreferencesRepository()
        val command = AliasCommand(preferencesRepository)

        assertEquals(
            CommandResult.Output(listOf("usage: alias <name> <application>")),
            command.execute(contextFor(UnixShellProfile, emptyList())),
        )
        assertEquals(
            CommandResult.Output(listOf("usage: alias <name> <application>")),
            command.execute(contextFor(UnixShellProfile, listOf("browser"))),
        )
        assertEquals(
            CommandResult.Output(listOf("USAGE: ALIAS <NAME> <APPLICATION>")),
            command.execute(contextFor(DosShellProfile, emptyList())),
        )
        assertEquals(emptyList<String>(), preferencesRepository.writes)
    }

    @Test
    fun `lists ambiguous applications instead of naming one of them`() = runTest {
        val preferencesRepository = RecordingPreferencesRepository()
        val command = AliasCommand(preferencesRepository)

        val result = command.execute(contextFor(UnixShellProfile, listOf("m", "mail a")))

        assertEquals(
            CommandResult.Listing(
                lines = listOf("mail a matches more than one application"),
                apps = listOf(mailArchive, mailAssistant),
            ),
            result,
        )
        assertEquals(emptyList<String>(), preferencesRepository.writes)
    }

    @Test
    fun `reports an application it cannot find`() = runTest {
        val preferencesRepository = RecordingPreferencesRepository()
        val command = AliasCommand(preferencesRepository)

        val result = command.execute(contextFor(UnixShellProfile, listOf("browser", "telegram")))

        assertEquals(
            CommandResult.Listing(
                lines = listOf("no application matches telegram"),
                apps = emptyList(),
            ),
            result,
        )
        assertEquals(emptyList<String>(), preferencesRepository.writes)
    }

    @Test
    fun `answers to the alias identifier`() {
        val command = AliasCommand(RecordingPreferencesRepository())

        assertEquals(Command.ALIAS, command.id)
        assertEquals("Name an application", command.description)
    }

    private val mailArchive =
        InstalledApp(packageName = "org.example.archive", label = "Mail Archive")
    private val mailAssistant =
        InstalledApp(packageName = "org.example.assistant", label = "Mail Assistant")

    private fun contextFor(
        shellProfile: ShellProfile,
        arguments: List<String>,
    ): CommandContext = CommandContext(
        arguments = arguments,
        shellProfile = shellProfile,
        installedApps = listOf(
            InstalledApp(packageName = "org.example.firefox", label = "Firefox"),
            InstalledApp(packageName = "org.example.mail", label = "Mail"),
            mailArchive,
            mailAssistant,
        ),
        registeredCommands = Command.entries.map { command ->
            CommandSummary(id = command, description = "Registered command")
        },
    )
}
