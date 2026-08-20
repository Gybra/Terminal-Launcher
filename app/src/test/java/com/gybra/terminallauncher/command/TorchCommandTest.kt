package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.launcher.FakeTorch
import com.gybra.terminallauncher.launcher.TorchState
import com.gybra.terminallauncher.shell.ShellProfile
import com.gybra.terminallauncher.shell.dos.DosShellProfile
import com.gybra.terminallauncher.shell.unix.UnixShellProfile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TorchCommandTest {
    @Test
    fun `reports the torch it turned on and the torch it turned off`() = runTest {
        val torch = FakeTorch(TorchState.ON, TorchState.OFF)
        val command = TorchCommand(torch)

        assertEquals(
            CommandResult.Output(listOf("torch on")),
            command.execute(contextFor(UnixShellProfile)),
        )
        assertEquals(
            CommandResult.Output(listOf("torch off")),
            command.execute(contextFor(UnixShellProfile)),
        )
        assertEquals(2, torch.toggles)
    }

    @Test
    fun `reports a device with no torch to turn on`() = runTest {
        val command = TorchCommand(FakeTorch(TorchState.UNAVAILABLE))

        val result = command.execute(contextFor(UnixShellProfile))

        assertEquals(CommandResult.Output(listOf("torch unavailable")), result)
    }

    @Test
    fun `writes its report the DOS way`() = runTest {
        val command = TorchCommand(FakeTorch(TorchState.ON))

        val result = command.execute(contextFor(DosShellProfile))

        assertEquals(CommandResult.Output(listOf("TORCH ON")), result)
    }

    private fun contextFor(shellProfile: ShellProfile): CommandContext = CommandContext(
        arguments = emptyList(),
        shellProfile = shellProfile,
        installedApps = emptyList(),
        registeredCommands = emptyList(),
    )
}
