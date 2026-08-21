package com.gybra.terminallauncher.shell

import com.gybra.terminallauncher.command.Command
import com.gybra.terminallauncher.command.CommandSummary
import com.gybra.terminallauncher.launcher.BatteryStatus
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.launcher.AppShortcut

public interface ShellProfile {
    public val type: ShellType

    /** The shape this shell writes its prompt cursor as. */
    public val cursor: PromptCursor

    public fun prompt(context: ShellContext): String

    public fun formatAppName(app: InstalledApp): String

    /** Writes the name of a shortcut pinned to Home, which no application list ever contains. */
    public fun formatShortcutName(shortcut: AppShortcut): String

    /** Writes the location [context] points at, whether or not the prompt shows it. */
    public fun formatPath(context: ShellContext): String

    public fun aliasFor(command: Command): String

    /** Writes a one-line command message, such as a confirmation or an error, in this shell style. */
    public fun formatMessage(message: String): String

    public fun formatAppList(apps: List<InstalledApp>): List<String> = apps.map(::formatAppName)

    /**
     * Describes [commands] with the primary alias of this shell, so optional aliases accepted only
     * for compatibility stay out of the help output. A command taking arguments is followed by the
     * ways it is invoked, indented under its description.
     */
    public fun formatHelp(commands: List<CommandSummary>): List<String> =
        commands.flatMap { command ->
            listOf(aliasFor(command.id).padEnd(HELP_ALIAS_COLUMN_WIDTH) + command.description) +
                command.usage.map { form ->
                    " ".repeat(HELP_ALIAS_COLUMN_WIDTH) + formatInvocation(command.id, form)
                }
        }

    /**
     * Writes how [command] is invoked, one line per accepted form, the first one prefixed the way
     * a shell answers a wrong invocation.
     */
    public fun formatUsage(command: Command, forms: List<String>): List<String> =
        forms.mapIndexed { index, form ->
            val prefix = if (index == 0) USAGE_PREFIX else " ".repeat(USAGE_PREFIX.length)
            formatMessage(prefix) + formatInvocation(command, form)
        }

    private fun formatInvocation(command: Command, form: String): String =
        formatMessage("${aliasFor(command)} $form")

    public fun aliasesFor(command: Command): Set<String> = setOf(aliasFor(command))

    /**
     * Writes the line an empty Home reads, naming the command that lists the others, so a Home
     * with nothing pinned and nothing printed still says that commands exist.
     */
    public fun formatHelpInvitation(): String =
        formatMessage("type ${aliasFor(Command.HELP)} to list the commands")

    /** Writes the battery part of the line Home keeps above everything else. */
    public fun formatBattery(battery: BatteryStatus): String = formatMessage(
        "${battery.percentage}%" + if (battery.charging) " charging" else "",
    )
}

private const val HELP_ALIAS_COLUMN_WIDTH = 10

private const val USAGE_PREFIX = "usage: "
