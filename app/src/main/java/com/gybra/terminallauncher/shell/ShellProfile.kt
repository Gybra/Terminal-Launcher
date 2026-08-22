package com.gybra.terminallauncher.shell

import com.gybra.terminallauncher.command.Command
import com.gybra.terminallauncher.command.CommandGroup
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
     * Writes the lines that frame the applications and shortcuts pinned to Home, the way this
     * shell announces a directory it is listing. [items] counts them together, since a listing
     * counts the entries it writes rather than what kind each one is.
     */
    public fun formatPinnedSection(context: ShellContext, items: Int): SectionLines =
        SectionLines(above = listOf("${formatPath(context)}/$PINNED_DIRECTORY:"))

    /**
     * Writes the lines that frame what the typed line matches, announcing how many were found and
     * saying so when there were none, since a search with no result must not read like a search
     * that never ran.
     */
    public fun formatSearchSection(matches: Int): SectionLines =
        SectionLines(above = listOf(formatMessage(countMatches(matches))))

    private fun countMatches(matches: Int): String = when (matches) {
        0 -> "no matches"
        1 -> "1 match:"
        else -> "$matches matches:"
    }

    /**
     * Describes [commands] with the primary alias of this shell, so optional aliases accepted only
     * for compatibility stay out of the help output. Commands are written under the name of the
     * group they belong to, in the order the groups are declared, separated by a blank line, and a
     * command taking arguments is followed by the ways it is invoked, indented under it.
     */
    public fun formatHelp(commands: List<CommandSummary>): List<String> =
        CommandGroup.entries
            .mapNotNull { group -> describeGroup(group, commands.filter { it.group == group }) }
            .reduceOrNull { written, group -> written + "" + group }
            .orEmpty()

    /** Writes [group] and what it holds, or nothing at all when it holds nothing. */
    private fun describeGroup(group: CommandGroup, commands: List<CommandSummary>): List<String>? =
        if (commands.isEmpty()) {
            null
        } else {
            listOf(formatMessage(group.label)) + commands.flatMap(::describeCommand)
        }

    private fun describeCommand(command: CommandSummary): List<String> =
        listOf(aliasFor(command.id).padEnd(HELP_ALIAS_COLUMN_WIDTH) + command.description) +
            command.usage.map { form -> USAGE_INDENT + formatInvocation(command.id, form) }

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
     * Writes how [command] is invoked on [name], with [keyword] between them when the command
     * takes one, ready to be read and edited at the prompt. The alias and the keyword are written
     * in the case of this shell, while [name] is left as it is, since the parser resolves the name
     * an application carries rather than the decorated one a list writes.
     */
    public fun formatCommandLine(command: Command, keyword: String? = null, name: String): String =
        listOfNotNull(aliasFor(command), keyword?.let(::formatMessage), name)
            .joinToString(separator = " ")

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

/** The directory both shells name the pinned block after, written in the case of each. */
internal const val PINNED_DIRECTORY: String = "pinned"

private const val HELP_ALIAS_COLUMN_WIDTH = 10

/** What an invocation form is indented by under the command it belongs to. */
private const val USAGE_INDENT = "  "

private const val USAGE_PREFIX = "usage: "
