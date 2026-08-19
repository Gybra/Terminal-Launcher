package com.gybra.terminallauncher.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

internal fun terminalTextStyle(color: Color): TextStyle = TextStyle(
    color = color,
    fontFamily = FontFamily.Monospace,
    fontSize = 18.sp,
    lineHeight = 24.sp,
)
