package com.gybra.terminallauncher.ui.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gybra.terminallauncher.launcher.BatteryStatus
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.ui.NotificationBadge
import com.gybra.terminallauncher.ui.StatusLine
import com.gybra.terminallauncher.ui.terminalTextStyle
import com.gybra.terminallauncher.ui.theme.LocalTerminalColors

/** The made-up device the preview writes from, so it never shows a real application or reading. */
internal object PreviewSample {
    const val CLOCK: String = "12:00"
    const val NOTIFICATIONS: Int = 3
    val BATTERY: BatteryStatus = BatteryStatus(percentage = 50, charging = false)
    val APP: InstalledApp = InstalledApp(packageName = "sample", label = "messages")
}

/**
 * What Home would write with these settings: the status line, the prompt, and one application
 * with its badge. It answers no touch. Whatever a change touched is outlined for a moment, and the
 * whole preview is when the change added or took away a part or recoloured everything.
 */
@Composable
internal fun SettingsPreview(state: SettingsUiState, modifier: Modifier = Modifier) {
    val colors = LocalTerminalColors.current
    val statusVisible = state.showClock || state.showBattery
    val prompt = state.shellProfile.prompt(state.shellContext())
    ChangeHighlight(
        value = listOf(state.terminalTheme, statusVisible, state.notificationAccess),
        modifier = modifier
            .fillMaxWidth()
            .dashedBorder(colors.secondary)
            .padding(12.dp)
            .testTag(SettingsEntry.PREVIEW.key),
    ) {
        Column {
            BasicText(text = state.write(SettingsEntry.PREVIEW.label), style = descriptionStyle(colors.secondary))
            if (statusVisible) {
                ChangeHighlight(value = listOf(state.showClock, state.showBattery)) {
                    StatusLine(
                        clock = PreviewSample.CLOCK.takeIf { state.showClock },
                        battery = state.shellProfile.formatBattery(PreviewSample.BATTERY).takeIf { state.showBattery },
                    )
                }
            }
            ChangeHighlight(value = prompt) {
                BasicText(text = prompt, style = terminalTextStyle(colors.foreground))
            }
            SampleApp(state)
        }
    }
}

@Composable
private fun SampleApp(state: SettingsUiState) {
    val colors = LocalTerminalColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        BasicText(
            text = state.shellProfile.formatAppName(PreviewSample.APP),
            style = terminalTextStyle(colors.foreground),
        )
        if (state.notificationAccess) {
            ChangeHighlight(value = listOf(state.badgeSize, state.badgeBackground, state.badgeText)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NotificationBadge(
                        count = PreviewSample.NOTIFICATIONS,
                        background = state.badgeBackground,
                        text = state.badgeText,
                        pressed = false,
                        size = state.badgeSize,
                    )
                }
            }
        }
    }
}

/**
 * Outlines [content] in the foreground colour, fading out over a second, whenever [value] changes
 * after the first composition.
 */
@Composable
private fun ChangeHighlight(
    value: Any,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val color = LocalTerminalColors.current.foreground
    val glow = remember { Animatable(0f) }
    var shownValue by remember { mutableStateOf(value) }
    LaunchedEffect(value) {
        if (value != shownValue) {
            shownValue = value
            glow.snapTo(1f)
            glow.animateTo(0f, tween(durationMillis = HIGHLIGHT_MILLIS))
        }
    }
    Box(
        modifier = Modifier
            .drawWithContent {
                drawContent()
                drawRect(color.copy(alpha = glow.value), style = Stroke(width = 2.dp.toPx()))
            }
            .then(modifier),
    ) {
        content()
    }
}

private fun Modifier.dashedBorder(color: Color): Modifier = drawBehind {
    val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
    drawRect(color, style = Stroke(width = 1.dp.toPx(), pathEffect = dash))
}

private const val HIGHLIGHT_MILLIS = 1000
