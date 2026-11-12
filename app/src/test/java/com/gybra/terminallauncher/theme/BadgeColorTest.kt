package com.gybra.terminallauncher.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BadgeColorTest {
    @Test
    fun `parses a stored override as an opaque color`() {
        assertEquals(Color(0xFFAA0000), BadgeColor.parse("#aa0000"))
    }

    @Test
    fun `reads a missing or malformed override as none`() {
        assertNull(BadgeColor.parse(null))
        assertNull(BadgeColor.parse("red"))
        assertNull(BadgeColor.parse("#12345"))
    }
}
