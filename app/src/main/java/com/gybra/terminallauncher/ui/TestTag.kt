package com.gybra.terminallauncher.ui

/**
 * The regions a Compose test reaches for, named once here so a screen and its test cannot drift
 * apart. Nothing the user sees depends on them.
 */
public enum class TestTag(public val tag: String) {
    HOME_LIST("home-list"),
    SETTINGS_LIST("settings-list"),
    PROMPT_INPUT("prompt-input"),
}
