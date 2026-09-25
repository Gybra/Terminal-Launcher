package com.gybra.terminallauncher.theme

/** Opaque hex color accepted at the preference boundary and in Settings. */
public object BadgeColor {
    public fun isValid(value: String): Boolean = Regex("#[0-9a-fA-F]{6}").matches(value)
}
