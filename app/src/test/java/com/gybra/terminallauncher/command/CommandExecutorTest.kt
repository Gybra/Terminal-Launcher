package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.shell.dos.DosShellProfile
import com.gybra.terminallauncher.shell.unix.UnixShellProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class CommandExecutorTest {
    @Test
    fun `runs the command registered for the typed alias`() {
        val listApps = RecordingCommand(id = Command.LIST_APPS)
        val executor = CommandExecutor(CommandRegistry(commands = listOf(listApps)))

        val result = executor.execute(
            input = "  ls  ",
            shellProfile = UnixShellProfile,
            installedApps = apps,
        )

        assertEquals(CommandResult.Output(emptyList()), result)
        assertEquals(1, listApps.executions)
    }

    @Test
    fun `gives the running command its arguments, shell, applications, and command set`() {
        val pin = RecordingCommand(id = Command.PIN)
        val executor = CommandExecutor(CommandRegistry(commands = listOf(pin)))

        executor.execute(
            input = "PIN com.example.mail extra",
            shellProfile = DosShellProfile,
            installedApps = apps,
        )

        assertEquals(
            CommandContext(
                arguments = listOf("com.example.mail", "extra"),
                shellProfile = DosShellProfile,
                installedApps = apps,
                registeredCommands = listOf(
                    CommandSummary(id = Command.PIN, description = "Recorded command"),
                ),
            ),
            pin.lastContext,
        )
    }

    @Test
    fun `falls back to search for an unregistered command`() {
        val listApps = RecordingCommand(id = Command.LIST_APPS)
        val executor = CommandExecutor(CommandRegistry(commands = listOf(listApps)))

        val result = executor.execute(
            input = "telegram",
            shellProfile = UnixShellProfile,
            installedApps = apps,
        )

        assertSame(CommandResult.Search, result)
        assertEquals(0, listApps.executions)
    }

    @Test
    fun `falls back to search for blank input`() {
        val executor = CommandExecutor(CommandRegistry(commands = emptyList()))

        assertSame(
            CommandResult.Search,
            executor.execute(input = "   ", shellProfile = UnixShellProfile, installedApps = apps),
        )
    }

    @Test
    fun `returns the result the command produced`() {
        val help = RecordingCommand(
            id = Command.HELP,
            result = CommandResult.Output(listOf("help")),
        )
        val executor = CommandExecutor(CommandRegistry(commands = listOf(help)))

        assertEquals(
            CommandResult.Output(listOf("help")),
            executor.execute(input = "help", shellProfile = UnixShellProfile, installedApps = apps),
        )
    }

    private val apps = listOf(InstalledApp(packageName = "com.example.mail", label = "Mail"))
}
