package com.gybra.terminallauncher.command

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

        val result = executor.execute(input = "  ls  ", shellProfile = UnixShellProfile)

        assertSame(CommandResult.Handled, result)
        assertEquals(1, listApps.executions)
        assertEquals(emptyList<String>(), listApps.lastArguments)
    }

    @Test
    fun `passes the remaining tokens to the command as arguments`() {
        val pin = RecordingCommand(id = Command.PIN)
        val executor = CommandExecutor(CommandRegistry(commands = listOf(pin)))

        executor.execute(input = "PIN com.example.mail extra", shellProfile = DosShellProfile)

        assertEquals(listOf("com.example.mail", "extra"), pin.lastArguments)
    }

    @Test
    fun `falls back to search for an unregistered command`() {
        val listApps = RecordingCommand(id = Command.LIST_APPS)
        val executor = CommandExecutor(CommandRegistry(commands = listOf(listApps)))

        val result = executor.execute(input = "telegram", shellProfile = UnixShellProfile)

        assertSame(CommandResult.Search, result)
        assertEquals(0, listApps.executions)
    }

    @Test
    fun `falls back to search for blank input`() {
        val executor = CommandExecutor(CommandRegistry(commands = emptyList()))

        assertSame(CommandResult.Search, executor.execute(input = "   ", shellProfile = UnixShellProfile))
    }

    @Test
    fun `returns the result the command produced`() {
        val clear = RecordingCommand(id = Command.CLEAR, result = CommandResult.Search)
        val executor = CommandExecutor(CommandRegistry(commands = listOf(clear)))

        assertSame(CommandResult.Search, executor.execute(input = "clear", shellProfile = UnixShellProfile))
    }
}
