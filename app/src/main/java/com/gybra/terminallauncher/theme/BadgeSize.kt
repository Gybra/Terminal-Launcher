package com.gybra.terminallauncher.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The three badge steps. [TWO] is the size the badge had before this setting existed.
 */
public enum class BadgeSize(
    public val step: Int,
    public val fontSize: TextUnit,
    public val lineHeight: TextUnit,
    public val horizontalPadding: Dp,
    public val verticalPadding: Dp,
) {
    ONE(1, 12.sp, 16.sp, 4.dp, 1.dp),
    TWO(2, 18.sp, 24.sp, 8.dp, 2.dp),
    THREE(3, 24.sp, 32.sp, 12.dp, 4.dp),
    ;

    public companion object {
        /** Reads a stored step, keeping [TWO] when the value is missing or not one of the three. */
        public fun fromStep(step: Int?): BadgeSize =
            entries.firstOrNull { size -> size.step == step } ?: TWO
    }
}
