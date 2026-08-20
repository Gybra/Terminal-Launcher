package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.shell.dos.DosShellProfile
import com.gybra.terminallauncher.shell.unix.UnixShellProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class CommandRegistryTest {
    @Test
    fun `resolves the Unix alias of a registered command`() {
        val listApps = RecordingCommand(id = Command.LIST_APPS)
        val registry = CommandRegistry(commands = listOf(listApps))

        assertSame(listApps, registry.resolve(name = "ls", shellProfile = UnixShellProfile))
    }

    @Test
    fun `resolves the DOS alias of a registered command`() {
        val listApps = RecordingCommand(id = Command.LIST_APPS)
        val registry = CommandRegistry(commands = listOf(listApps))

        assertSame(listApps, registry.resolve(name = "DIR", shellProfile = DosShellProfile))
    }

    @Test
    fun `resolves aliases ignoring case`() {
        val clear = RecordingCommand(id = Command.CLEAR)
        val registry = CommandRegistry(commands = listOf(clear))

        assertSame(clear, registry.resolve(name = "CLS", shellProfile = UnixShellProfile))
        assertSame(clear, registry.resolve(name = "cls", shellProfile = DosShellProfile))
    }

    @Test
    fun `resolves nothing for an alias of another shell`() {
        val registry = CommandRegistry(commands = listOf(RecordingCommand(id = Command.LIST_APPS)))

        assertNull(registry.resolve(name = "ls", shellProfile = DosShellProfile))
    }

    @Test
    fun `resolves nothing for an unregistered command`() {
        val registry = CommandRegistry(commands = listOf(RecordingCommand(id = Command.LIST_APPS)))

        assertNull(registry.resolve(name = "help", shellProfile = UnixShellProfile))
    }

    @Test
    fun `resolves nothing when no command is registered`() {
        assertNull(
            CommandRegistry(commands = emptyList())
                .resolve(name = "ls", shellProfile = UnixShellProfile),
        )
    }

    @Test
    fun `summarizes registered commands in registration order`() {
        val registry = CommandRegistry(
            commands = listOf(
                RecordingCommand(id = Command.LIST_APPS, description = "List installed applications"),
                RecordingCommand(id = Command.HELP, description = "Show available commands"),
            ),
        )

        assertEquals(
            listOf(
                CommandSummary(id = Command.LIST_APPS, description = "List installed applications"),
                CommandSummary(id = Command.HELP, description = "Show available commands"),
            ),
            registry.summaries,
        )
    }

    @Test
    fun `summarizes nothing when no command is registered`() {
        assertEquals(emptyList<CommandSummary>(), CommandRegistry(commands = emptyList()).summaries)
    }

    @Test
    fun `rejects two commands sharing an identifier`() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            CommandRegistry(
                commands = listOf(
                    RecordingCommand(id = Command.HELP),
                    RecordingCommand(id = Command.HELP),
                ),
            )
        }

        assertEquals("Duplicate command registration: HELP", failure.message)
    }
}
