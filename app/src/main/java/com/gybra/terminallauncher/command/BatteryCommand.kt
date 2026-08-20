package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.launcher.BatteryRepository

/** Reports the battery level Android publishes, without asking for any permission. */
public class BatteryCommand(
    private val batteryRepository: BatteryRepository,
) : LauncherCommand {
    override val id: Command = Command.BATTERY

    override val description: String = "Report the battery level"

    override suspend fun execute(context: CommandContext): CommandResult {
        val status = batteryRepository.readStatus()
            ?: return context.message("battery level unavailable")
        val state = if (status.charging) "charging" else "on battery"

        return context.message("battery ${status.percentage}% $state")
    }
}
