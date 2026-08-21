package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.preferences.LauncherPreferences
import com.gybra.terminallauncher.preferences.RecordingPreferencesRepository
import com.gybra.terminallauncher.shell.dos.DosShellProfile
import com.gybra.terminallauncher.shell.unix.UnixShellProfile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UnpinCommandTest {
    @Test
    fun `unpins the only application matching the argument`() = runTest {
        val preferencesRepository = RecordingPreferencesRepository(
            initialPreferences = LauncherPreferences(pinnedPackages = setOf("org.example.camera")),
        )
        val command = UnpinCommand(preferencesRepository)

        val result = command.execute(contextFor(UnixShellProfile, listOf("camera")))

        assertEquals(CommandResult.Output(listOf("unpinned camera")), result)
        assertEquals(listOf("unpinPackage(org.example.camera)"), preferencesRepository.writes)
    }

    @Test
    fun `unpins an application that was not pinned without failing`() = runTest {
        val preferencesRepository = RecordingPreferencesRepository()
        val command = UnpinCommand(preferencesRepository)

        val result = command.execute(contextFor(UnixShellProfile, listOf("camera")))

        assertEquals(CommandResult.Output(listOf("unpinned camera")), result)
        assertEquals(listOf("unpinPackage(org.example.camera)"), preferencesRepository.writes)
    }

    @Test
    fun `reports its usage the DOS way when no argument is given`() = runTest {
        val preferencesRepository = RecordingPreferencesRepository()
        val command = UnpinCommand(preferencesRepository)

        val result = command.execute(contextFor(DosShellProfile, emptyList()))

        assertEquals(CommandResult.Output(listOf("USAGE: UNPIN <APPLICATION>")), result)
        assertEquals(emptyList<String>(), preferencesRepository.writes)
    }

    @Test
    fun `answers to the unpin identifier`() {
        val command = UnpinCommand(RecordingPreferencesRepository())

        assertEquals(Command.UNPIN, command.id)
        assertEquals("Remove an app from Home", command.description)
    }

    private fun contextFor(
        shellProfile: com.gybra.terminallauncher.shell.ShellProfile,
        arguments: List<String>,
    ): CommandContext = CommandContext(
        arguments = arguments,
        shellProfile = shellProfile,
        installedApps = listOf(InstalledApp(packageName = "org.example.camera", label = "Camera")),
        registeredCommands = emptyList(),
    )
}
