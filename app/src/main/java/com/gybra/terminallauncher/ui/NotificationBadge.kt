package com.gybra.terminallauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gybra.terminallauncher.theme.BadgeColor
import com.gybra.terminallauncher.theme.BadgeSize
import com.gybra.terminallauncher.ui.home.HomeItem
import com.gybra.terminallauncher.ui.theme.LocalTerminalColors

/**
 * The count written after an application name. [background] and [text] are stored `#RRGGBB`
 * overrides; a missing or invalid one falls back to the theme. A pressed row swaps the colours.
 */
@Composable
internal fun NotificationBadge(
    count: Int,
    background: String?,
    text: String?,
    pressed: Boolean,
    size: BadgeSize,
) {
    val colors = LocalTerminalColors.current
    val fill = BadgeColor.parse(background) ?: colors.foreground
    val ink = BadgeColor.parse(text) ?: colors.background
    Spacer(modifier = Modifier.width(8.dp))
    BasicText(
        text = if (count > 99) HomeItem.BADGE_OVERFLOW else count.toString(),
        style = terminalTextStyle(
            color = if (pressed) colors.foreground else ink,
            fontSize = size.fontSize,
            lineHeight = size.lineHeight,
        ),
        modifier = Modifier
            .background(
                if (pressed) colors.background else fill,
                CircleShape,
            )
            .padding(horizontal = size.horizontalPadding, vertical = size.verticalPadding)
            .semantics { contentDescription = HomeItem.badgeDescription(count) },
    )
}
