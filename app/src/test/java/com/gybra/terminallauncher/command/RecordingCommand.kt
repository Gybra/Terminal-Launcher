package com.gybra.terminallauncher.command

/** A [LauncherCommand] that records how the engine invoked it. */
class RecordingCommand(
    override val id: Command,
    override val description: String = "Recorded command",
    private val result: CommandResult = CommandResult.Output(emptyList()),
) : LauncherCommand {
    var executions: Int = 0
        private set
    var lastContext: CommandContext? = null
        private set

    override suspend fun execute(context: CommandContext): CommandResult {
        executions += 1
        lastContext = context
        return result
    }
}
