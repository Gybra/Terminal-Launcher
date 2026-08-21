package com.gybra.terminallauncher.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

internal fun terminalTextStyle(color: Color): TextStyle = TextStyle(
    color = color,
    fontFamily = FontFamily.Monospace,
    fontSize = TERMINAL_FONT_SIZE,
    lineHeight = 24.sp,
)

/**
 * The size every terminal line is written at. The prompt cursor is measured from it rather than
 * from a length in pixels, so it keeps covering one character when the device scales its fonts.
 */
internal val TERMINAL_FONT_SIZE = 18.sp
