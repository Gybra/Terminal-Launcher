package com.gybra.terminallauncher.theme

import androidx.compose.ui.graphics.Color

/** Opaque hex color accepted at the preference boundary and in Settings. */
public object BadgeColor {
    public fun isValid(value: String): Boolean = Regex("#[0-9a-fA-F]{6}").matches(value)

    /** Reads a stored `#RRGGBB` override, or `null` when there is none or it is not valid. */
    public fun parse(value: String?): Color? = value?.takeIf(::isValid)
        ?.removePrefix("#")?.toLongOrNull(radix = 16)
        ?.let { rgb -> Color(0xff000000L or rgb) }
}
