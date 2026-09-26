package com.gybra.terminallauncher.theme

import androidx.compose.ui.graphics.Color

/** The 16 CGA colors a badge can take, stored as `#RRGGBB`. */
public enum class DosColor(public val hex: String) {
    BLACK("#000000"),
    BLUE("#0000AA"),
    GREEN("#00AA00"),
    CYAN("#00AAAA"),
    RED("#AA0000"),
    MAGENTA("#AA00AA"),
    BROWN("#AA5500"),
    LIGHT_GRAY("#AAAAAA"),
    DARK_GRAY("#555555"),
    LIGHT_BLUE("#5555FF"),
    LIGHT_GREEN("#55FF55"),
    LIGHT_CYAN("#55FFFF"),
    LIGHT_RED("#FF5555"),
    LIGHT_MAGENTA("#FF55FF"),
    YELLOW("#FFFF55"),
    WHITE("#FFFFFF"),
    ;

    public val color: Color
        get() = Color(0xFF000000L or hex.removePrefix("#").toLong(16))
}
