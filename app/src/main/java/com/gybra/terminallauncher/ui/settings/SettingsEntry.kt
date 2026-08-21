package com.gybra.terminallauncher.ui.settings

/**
 * Every row the settings screen keeps, named once: the screen lists it under [key] and writes
 * [label], and a test reads the same values instead of repeating them. A row whose text is not
 * fixed, such as the one carrying a storage error, keeps no label.
 */
public enum class SettingsEntry(public val key: String, public val label: String = "") {
    BACK("back", "< back"),
    STORAGE_ERROR("storage-error"),
    APPEARANCE("appearance", "Appearance"),
    SHELL("shell", "Shell"),
    THEME("theme", "Theme"),
    SHOW_CLOCK("clock", "Show clock"),
    SHOW_BATTERY("battery", "Show battery"),
    IMMERSIVE_MODE("immersive-mode", "Immersive mode"),
    DOUBLE_TAP_TO_LOCK("double-tap-to-lock", "Double tap to lock"),
    SHOW_PROMPT_PATH("prompt-path", "Show path in prompt"),
    UNIX("unix", "Unix"),
    USERNAME("username", "Username"),
    HOSTNAME("hostname", "Hostname"),
    PROMPT_SYMBOL("prompt-symbol", "Prompt symbol"),
    DOS("dos", "DOS"),
    DRIVE("drive", "Drive"),
    ;

    /** Names one of the options listed under this row, such as a single shell or a single theme. */
    public fun optionKey(option: Enum<*>): String = "$key-${option.name}"
}
