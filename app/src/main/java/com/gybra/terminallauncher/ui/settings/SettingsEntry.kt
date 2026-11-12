package com.gybra.terminallauncher.ui.settings

/**
 * Every row and fixed word the settings screen writes, named once: the screen lists a row under
 * [key] and writes [label], [description] says what the setting changes, and [summary] is the word
 * a collapsed section uses for it. A test reads the same values instead of repeating them. A word
 * that is not a row, such as [OFF] or [EXPANDED], keeps a key nothing reads.
 */
public enum class SettingsEntry(
    public val key: String,
    public val label: String = "",
    public val description: String = "",
    public val summary: String = label,
) {
    BACK("back", "< back"),
    PREVIEW("preview", "preview"),
    STORAGE_ERROR("storage-error"),
    APPEARANCE("appearance", "Appearance"),
    HOME_SCREEN("home-screen", "Home screen"),
    GESTURES("gestures", "Gestures"),
    PROMPT("prompt", "Prompt"),
    NOTIFICATIONS("notifications", "Notifications"),
    SHELL(
        "shell",
        "Shell",
        "How the launcher talks: unix writes lowercase and user@host, dos writes uppercase and a drive",
    ),
    THEME("theme", "Theme", "Colours of the whole launcher"),
    NOTIFICATION_ACCESS(
        "notification-access",
        "Notification access",
        "Lets the launcher count the notifications of each app; android grants it in its own settings",
    ),
    NOTIFICATION_ENABLED("notification-enabled", "granted"),
    NOTIFICATION_SETTINGS("notification-settings", "< android settings"),
    BADGE_RESET("badge-reset", "< theme default"),
    BADGE_DEFAULT("badge-default", "default"),
    BADGE_SIZE("badge-size", "Badge size", "Size of the counter next to an app", summary = "size"),
    BADGE_BACKGROUND(
        "badge-background",
        "Badge background",
        "Fill of the counter; default follows the theme",
    ),
    BADGE_TEXT("badge-text", "Badge text", "Digits of the counter; default follows the theme"),
    SHOW_CLOCK("clock", "Show clock", "Time at the top of home", summary = "clock"),
    SHOW_BATTERY("battery", "Show battery", "Battery level at the top of home", summary = "battery"),
    IMMERSIVE_MODE(
        "immersive-mode",
        "Immersive mode",
        "Hides the android status and navigation bars",
        summary = "immersive",
    ),
    DOUBLE_TAP_TO_LOCK(
        "double-tap-to-lock",
        "Double tap to lock",
        "Double tap on empty space turns the screen off; android grants it in its accessibility settings",
        summary = "double tap locks",
    ),
    SHOW_PROMPT_PATH("prompt-path", "Show path in prompt", "Adds the current folder to the prompt"),
    USERNAME("username", "Username", "Name before the @ in the prompt"),
    HOSTNAME("hostname", "Hostname", "Name after the @ in the prompt"),
    PROMPT_SYMBOL("prompt-symbol", "Prompt symbol", "Character that ends the prompt"),
    DRIVE("drive", "Drive", "Letter at the start of the prompt"),
    ALL_OFF("all-off", "all off"),
    OFF("off", "off"),
    EXPANDED("expanded", "expanded"),
    COLLAPSED("collapsed", "collapsed"),
    ;

    /** Names one of the options listed under this row, such as a single shell or a single theme. */
    public fun optionKey(option: Enum<*>): String = "$key-${option.name}"
}
