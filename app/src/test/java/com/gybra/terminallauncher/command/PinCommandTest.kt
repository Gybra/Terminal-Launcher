package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.preferences.LauncherPreferences
import com.gybra.terminallauncher.preferences.RecordingPreferencesRepository
import com.gybra.terminallauncher.search.AppSearchEngine
import com.gybra.terminallauncher.shell.ShellProfile
import com.gybra.terminallauncher.shell.dos.DosShellProfile
import com.gybra.terminallauncher.shell.unix.UnixShellProfile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PinCommandTest {
    @Test
    fun `pins the only application matching the argument`() = runTest {
        val preferencesRepository = RecordingPreferencesRepository()
        val command = PinCommand(preferencesRepository)

        val result = command.execute(contextFor(UnixShellProfile, listOf("camera")))

        assertEquals(CommandResult.Output(listOf("pinned camera")), result)
        assertEquals(listOf("pinPackage(org.example.camera)"), preferencesRepository.writes)
    }

    @Test
    fun `pins the exact match when several applications share a prefix`() = runTest {
        val preferencesRepository = RecordingPreferencesRepository()
        val command = PinCommand(preferencesRepository)

        val result = command.execute(contextFor(UnixShellProfile, listOf("mail")))

        assertEquals(CommandResult.Output(listOf("pinned mail")), result)
        assertEquals(listOf("pinPackage(org.example.mail)"), preferencesRepository.writes)
    }

    @Test
    fun `joins multi-word arguments into a single application name`() = runTest {
        val preferencesRepository = RecordingPreferencesRepository()
        val command = PinCommand(preferencesRepository)

        val result = command.execute(contextFor(UnixShellProfile, listOf("mail", "archive")))

        assertEquals(CommandResult.Output(listOf("pinned mail archive")), result)
        assertEquals(listOf("pinPackage(org.example.archive)"), preferencesRepository.writes)
    }

    @Test
    fun `lists ambiguous matches instead of pinning one of them`() = runTest {
        val preferencesRepository = RecordingPreferencesRepository()
        val command = PinCommand(preferencesRepository)

        val result = command.execute(contextFor(UnixShellProfile, listOf("mail a")))

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
    fun `lists the best candidates when more applications match than the engine returns`() = runTest {
        val preferencesRepository = RecordingPreferencesRepository()
        val command = PinCommand(preferencesRepository)
        val manyApps = (1..6).map { index ->
            InstalledApp(packageName = "org.example.mail$index", label = "Mail $index")
        }

        val result = command.execute(
            CommandContext(
                arguments = listOf("mail"),
                shellProfile = UnixShellProfile,
                installedApps = manyApps,
                registeredCommands = emptyList(),
            ),
        )

        assertEquals(
            CommandResult.Listing(
                lines = listOf("mail matches more than one application"),
                apps = manyApps.take(AppSearchEngine.MAX_RESULTS),
            ),
            result,
        )
        assertEquals(emptyList<String>(), preferencesRepository.writes)
    }

    @Test
    fun `reports an argument matching no application`() = runTest {
        val preferencesRepository = RecordingPreferencesRepository()
        val command = PinCommand(preferencesRepository)

        val result = command.execute(contextFor(UnixShellProfile, listOf("telegram")))

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
    fun `reports its usage when no argument is given`() = runTest {
        val preferencesRepository = RecordingPreferencesRepository()
        val command = PinCommand(preferencesRepository)

        val result = command.execute(contextFor(UnixShellProfile, emptyList()))

        assertEquals(CommandResult.Output(listOf("usage: pin <application>")), result)
        assertEquals(emptyList<String>(), preferencesRepository.writes)
    }

    @Test
    fun `writes success, failure, and usage output the DOS way`() = runTest {
        val command = PinCommand(RecordingPreferencesRepository())

        assertEquals(
            CommandResult.Output(listOf("PINNED CAMERA.EXE")),
            command.execute(contextFor(DosShellProfile, listOf("camera"))),
        )
        assertEquals(
            CommandResult.Listing(
                lines = listOf("NO APPLICATION MATCHES TELEGRAM"),
                apps = emptyList(),
            ),
            command.execute(contextFor(DosShellProfile, listOf("telegram"))),
        )
        assertEquals(
            CommandResult.Output(listOf("USAGE: PIN <APPLICATION>")),
            command.execute(contextFor(DosShellProfile, emptyList())),
        )
        assertEquals(
            CommandResult.Listing(
                lines = listOf("MAIL A MATCHES MORE THAN ONE APPLICATION"),
                apps = listOf(mailArchive, mailAssistant),
            ),
            command.execute(contextFor(DosShellProfile, listOf("mail a"))),
        )
    }

    @Test
    fun `keeps pinning an application that is already pinned`() = runTest {
        val preferencesRepository = RecordingPreferencesRepository(
            initialPreferences = LauncherPreferences(pinnedPackages = setOf("org.example.camera")),
        )
        val command = PinCommand(preferencesRepository)

        val result = command.execute(contextFor(UnixShellProfile, listOf("camera")))

        assertEquals(CommandResult.Output(listOf("pinned camera")), result)
        assertEquals(listOf("pinPackage(org.example.camera)"), preferencesRepository.writes)
    }

    @Test
    fun `answers to the pin identifier`() {
        val command = PinCommand(RecordingPreferencesRepository())

        assertEquals(Command.PIN, command.id)
        assertEquals("Pin an application to Home", command.description)
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
            InstalledApp(packageName = "org.example.camera", label = "Camera"),
            InstalledApp(packageName = "org.example.mail", label = "Mail"),
            mailArchive,
            mailAssistant,
        ),
        registeredCommands = emptyList(),
    )
}
