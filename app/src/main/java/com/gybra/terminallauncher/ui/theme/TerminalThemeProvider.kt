package com.gybra.terminallauncher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.gybra.terminallauncher.theme.TerminalTheme
import com.gybra.terminallauncher.theme.colors

internal val LocalTerminalColors = staticCompositionLocalOf {
    TerminalTheme.MONOCHROME.colors(systemDarkTheme = true)
}

@Composable
public fun TerminalThemeProvider(
    theme: TerminalTheme,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalTerminalColors provides theme.colors(isSystemInDarkTheme()),
        content = content,
    )
}
