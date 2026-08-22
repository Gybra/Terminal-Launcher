package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.launcher.AppShortcut
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.launcher.PublishedShortcuts
import com.gybra.terminallauncher.launcher.ShortcutRepository
import com.gybra.terminallauncher.preferences.PreferencesRepository
import java.util.Locale
import kotlinx.coroutines.flow.first

/**
 * Lists the shortcuts an application publishes, pins one of them to Home, and removes a pinned
 * one. The application is named by its first argument and the shortcut by the rest of the line,
 * so a first argument reading `pin` or `unpin` asks for those instead of naming an application.
 */
public class ShortcutsCommand(
    private val shortcutRepository: ShortcutRepository,
    private val preferencesRepository: PreferencesRepository,
) : LauncherCommand {
    override val id: Command = Command.SHORTCUTS

    override val group: CommandGroup = CommandGroup.HOME

    override val description: String = "Manage app shortcuts"

    override val usage: List<String> = listOf(
        "<application>",
        "$PIN <application> <shortcut>",
        "$UNPIN <application> <shortcut>",
    )

    override suspend fun execute(context: CommandContext): CommandResult =
        when (context.arguments.firstOrNull()?.lowercase(Locale.ROOT)) {
            null -> context.answerUsage()
            PIN -> context.pin(context.arguments.drop(1))
            UNPIN -> context.unpin(context.arguments.drop(1))
            else -> context.list(context.arguments.joinToString(separator = " "))
        }

    private suspend fun CommandContext.list(query: String): CommandResult =
        withResolvedApp(query) { app ->
            withPublishedShortcuts(app) { shortcuts ->
                if (shortcuts.isEmpty()) {
                    message("${shellProfile.formatAppName(app)} publishes no shortcuts")
                } else {
                    CommandResult.Listing(lines = emptyList(), shortcuts = shortcuts)
                }
            }
        }

    private suspend fun CommandContext.pin(arguments: List<String>): CommandResult =
        withNamedShortcut(arguments) { app, name ->
            withPublishedShortcuts(app) { shortcuts ->
                withResolvedShortcut(shortcuts, name) { shortcut ->
                    preferencesRepository.pinShortcut(shortcut)
                    message("pinned ${shellProfile.formatShortcutName(shortcut)}")
                }
            }
        }

    private suspend fun CommandContext.unpin(arguments: List<String>): CommandResult =
        withNamedShortcut(arguments) { app, name ->
            val pinned = preferencesRepository.preferences.first().pinnedShortcuts
                .filter { shortcut -> shortcut.packageName == app.packageName }

            withResolvedShortcut(pinned, name) { shortcut ->
                preferencesRepository.unpinShortcut(shortcut)
                message("unpinned ${shellProfile.formatShortcutName(shortcut)}")
            }
        }

    /** Runs [action] with the application and the shortcut name the arguments carry, or answers. */
    private suspend fun CommandContext.withNamedShortcut(
        arguments: List<String>,
        action: suspend (InstalledApp, String) -> CommandResult,
    ): CommandResult {
        val appName = arguments.firstOrNull() ?: return answerUsage()
        val name = arguments.drop(1).joinToString(separator = " ")
        if (name.isBlank()) return answerUsage()

        return withResolvedApp(appName) { app -> action(app, name) }
    }

    /** Runs [action] with what [app] publishes, reporting a question Android refuses to answer. */
    private suspend fun CommandContext.withPublishedShortcuts(
        app: InstalledApp,
        action: suspend (List<AppShortcut>) -> CommandResult,
    ): CommandResult = when (val published = shortcutRepository.publishedBy(app.packageName)) {
        PublishedShortcuts.Refused -> message(REFUSED)
        is PublishedShortcuts.Available -> action(published.shortcuts)
    }

    private fun CommandContext.answerUsage(): CommandResult =
        CommandResult.Output(shellProfile.formatUsage(id, usage))

    public companion object {
        /** The keyword asking for the pinning form, which Home writes on a long press. */
        public const val PIN: String = "pin"

        /** The keyword asking for the removing form, which Home writes on a long press. */
        public const val UNPIN: String = "unpin"

        private const val REFUSED =
            "android lists shortcuts only while terminal launcher is the home application"
    }
}

/**
 * Runs [action] with the one shortcut [name] names among [shortcuts], preferring the shortcut
 * carrying exactly that name. Anything else is reported instead of guessed.
 */
private suspend fun CommandContext.withResolvedShortcut(
    shortcuts: List<AppShortcut>,
    name: String,
    action: suspend (AppShortcut) -> CommandResult,
): CommandResult {
    val matches = shortcuts
        .filter { shortcut -> shortcut.label.equals(name, ignoreCase = true) }
        .ifEmpty { shortcuts.filter { shortcut -> shortcut.label.contains(name, ignoreCase = true) } }

    return when (matches.size) {
        0 -> message("no shortcut matches $name")
        1 -> action(matches.first())
        else -> CommandResult.Listing(
            lines = listOf(shellProfile.formatMessage("$name matches more than one shortcut")),
            shortcuts = matches,
        )
    }
}
