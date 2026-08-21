package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.shell.ShellProfile
import com.gybra.terminallauncher.shell.dos.DosShellProfile
import com.gybra.terminallauncher.shell.unix.UnixShellProfile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HelpCommandTest {
    @Test
    fun `describes every registered command with its Unix alias`() = runTest {
        val result = HelpCommand.execute(contextFor(UnixShellProfile))

        assertEquals(
            CommandResult.Output(
                listOf(
                    "ls        List installed apps",
                    "help      Show available commands",
                ),
            ),
            result,
        )
    }

    @Test
    fun `hides the optional DOS aliases Unix only accepts for compatibility`() = runTest {
        val lines = outputLines(HelpCommand.execute(contextFor(UnixShellProfile)))

        assertTrue(lines.none { line -> line.startsWith("dir") })
    }

    @Test
    fun `describes every registered command with its DOS alias and a command count`() = runTest {
        val result = HelpCommand.execute(contextFor(DosShellProfile))

        assertEquals(
            CommandResult.Output(
                listOf(
                    "DIR       List installed apps",
                    "HELP      Show available commands",
                    "",
                    "2 Command(s)",
                ),
            ),
            result,
        )
    }

    @Test
    fun `answers to the help identifier`() {
        assertEquals(Command.HELP, HelpCommand.id)
        assertEquals("Show available commands", HelpCommand.description)
    }

    private fun contextFor(shellProfile: ShellProfile): CommandContext = CommandContext(
        arguments = emptyList(),
        shellProfile = shellProfile,
        installedApps = emptyList(),
        registeredCommands = listOf(
            CommandSummary(id = Command.LIST_APPS, description = "List installed apps"),
            CommandSummary(id = Command.HELP, description = "Show available commands"),
        ),
    )

    private fun outputLines(result: CommandResult): List<String> =
        (result as CommandResult.Output).lines
}
