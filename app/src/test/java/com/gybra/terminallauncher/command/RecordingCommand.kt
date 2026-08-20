package com.gybra.terminallauncher.command

/** A [LauncherCommand] that records how the engine invoked it. */
class RecordingCommand(
    override val id: Command,
    private val result: CommandResult = CommandResult.Handled,
) : LauncherCommand {
    var executions: Int = 0
        private set
    var lastArguments: List<String> = emptyList()
        private set

    override fun execute(context: CommandContext): CommandResult {
        executions += 1
        lastArguments = context.arguments
        return result
    }
}
