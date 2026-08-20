package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.preferences.PreferencesRepository
import com.gybra.terminallauncher.shell.ShellProfiles
import com.gybra.terminallauncher.shell.ShellType
import java.util.Locale

/**
 * Names an application so submitting that name launches it. A registered command always wins over
 * an alias, so a name any shell already uses for a command is refused rather than shadowing it.
 */
public class AliasCommand(
    private val preferencesRepository: PreferencesRepository,
) : LauncherCommand {
    override val id: Command = Command.ALIAS

    override val description: String = "Name an application"

    override suspend fun execute(context: CommandContext): CommandResult {
        val name = context.arguments.firstOrNull()?.lowercase(Locale.ROOT)
        val query = context.arguments.drop(1).joinToString(separator = " ")
        if (name.isNullOrBlank() || query.isBlank()) {
            return context.message("usage: ${context.shellProfile.aliasFor(id)} <name> <application>")
        }
        if (context.isCommandName(name)) {
            return context.message("$name is a command name")
        }

        return context.withResolvedApp(query) { app ->
            preferencesRepository.setAlias(name = name, packageName = app.packageName)
            context.message("aliased $name to ${context.shellProfile.formatAppName(app)}")
        }
    }

    private fun CommandContext.isCommandName(name: String): Boolean = ShellType.entries
        .map(ShellProfiles::forType)
        .flatMap { profile -> registeredCommands.flatMap { command -> profile.aliasesFor(command.id) } }
        .any { alias -> alias.equals(name, ignoreCase = true) }
}
