package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.launcher.Torch
import com.gybra.terminallauncher.launcher.TorchState

/** Turns the torch on or off, and says so when the device has none to turn on. */
public class TorchCommand(
    private val torch: Torch,
) : LauncherCommand {
    override val id: Command = Command.TORCH

    override val group: CommandGroup = CommandGroup.DEVICE

    override val description: String = "Toggle the torch"

    override suspend fun execute(context: CommandContext): CommandResult = when (torch.toggle()) {
        TorchState.ON -> context.message("torch on")
        TorchState.OFF -> context.message("torch off")
        TorchState.UNAVAILABLE -> context.message("torch unavailable")
    }
}
