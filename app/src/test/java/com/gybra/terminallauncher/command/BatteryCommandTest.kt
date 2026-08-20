package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.launcher.BatteryStatus
import com.gybra.terminallauncher.launcher.FakeBatteryRepository
import com.gybra.terminallauncher.shell.ShellProfile
import com.gybra.terminallauncher.shell.dos.DosShellProfile
import com.gybra.terminallauncher.shell.unix.UnixShellProfile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryCommandTest {
    @Test
    fun `reports a level the device is discharging`() = runTest {
        val command = BatteryCommand(
            FakeBatteryRepository(BatteryStatus(percentage = 42, charging = false)),
        )

        val result = command.execute(contextFor(UnixShellProfile))

        assertEquals(CommandResult.Output(listOf("battery 42% on battery")), result)
    }

    @Test
    fun `reports a level the device is charging`() = runTest {
        val command = BatteryCommand(
            FakeBatteryRepository(BatteryStatus(percentage = 100, charging = true)),
        )

        val result = command.execute(contextFor(UnixShellProfile))

        assertEquals(CommandResult.Output(listOf("battery 100% charging")), result)
    }

    @Test
    fun `reports a device that publishes no level`() = runTest {
        val command = BatteryCommand(FakeBatteryRepository(status = null))

        val result = command.execute(contextFor(UnixShellProfile))

        assertEquals(CommandResult.Output(listOf("battery level unavailable")), result)
    }

    @Test
    fun `writes its report the DOS way`() = runTest {
        val command = BatteryCommand(FakeBatteryRepository())

        val result = command.execute(contextFor(DosShellProfile))

        assertEquals(CommandResult.Output(listOf("BATTERY 42% ON BATTERY")), result)
    }

    private fun contextFor(shellProfile: ShellProfile): CommandContext = CommandContext(
        arguments = emptyList(),
        shellProfile = shellProfile,
        installedApps = emptyList(),
        registeredCommands = emptyList(),
    )
}
