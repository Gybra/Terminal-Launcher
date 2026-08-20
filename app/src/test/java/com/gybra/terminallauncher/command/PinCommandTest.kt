package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.preferences.LauncherPreferences
import com.gybra.terminallauncher.preferences.RecordingPreferencesRepository
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
            CommandResult.Output(
                listOf("mail a matches more than one application", "mail archive", "mail assistant"),
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
            CommandResult.Output(
                listOf(
                    "mail matches more than one application",
                    "mail 1",
                    "mail 2",
                    "mail 3",
                    "mail 4",
                    "mail 5",
                ),
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

        assertEquals(CommandResult.Output(listOf("no application matches telegram")), result)
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
            CommandResult.Output(listOf("NO APPLICATION MATCHES TELEGRAM")),
            command.execute(contextFor(DosShellProfile, listOf("telegram"))),
        )
        assertEquals(
            CommandResult.Output(listOf("USAGE: PIN <APPLICATION>")),
            command.execute(contextFor(DosShellProfile, emptyList())),
        )
        assertEquals(
            CommandResult.Output(
                listOf(
                    "MAIL A MATCHES MORE THAN ONE APPLICATION",
                    "MAIL ARCHIVE.EXE",
                    "MAIL ASSISTANT.EXE",
                    "",
                    "2 File(s)",
                ),
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

    private fun contextFor(
        shellProfile: ShellProfile,
        arguments: List<String>,
    ): CommandContext = CommandContext(
        arguments = arguments,
        shellProfile = shellProfile,
        installedApps = listOf(
            InstalledApp(packageName = "org.example.camera", label = "Camera"),
            InstalledApp(packageName = "org.example.mail", label = "Mail"),
            InstalledApp(packageName = "org.example.archive", label = "Mail Archive"),
            InstalledApp(packageName = "org.example.assistant", label = "Mail Assistant"),
        ),
        registeredCommands = emptyList(),
    )
}
