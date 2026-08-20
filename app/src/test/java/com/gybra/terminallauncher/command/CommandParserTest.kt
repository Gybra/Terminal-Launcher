package com.gybra.terminallauncher.command

import org.junit.Assert.assertEquals
import org.junit.Test

class CommandParserTest {
    @Test
    fun `tokenizes a command without arguments`() {
        assertEquals(listOf("ls"), CommandParser.tokenize("ls"))
    }

    @Test
    fun `trims and collapses whitespace around a command and its arguments`() {
        assertEquals(
            listOf("pin", "com.example.mail", "--force"),
            CommandParser.tokenize("   pin \t com.example.mail   --force  "),
        )
    }

    @Test
    fun `tokenizes blank input as no tokens`() {
        assertEquals(emptyList<String>(), CommandParser.tokenize("   "))
        assertEquals(emptyList<String>(), CommandParser.tokenize(""))
    }

    @Test
    fun `keeps token case so commands can read their arguments verbatim`() {
        assertEquals(listOf("PIN", "Com.Example.Mail"), CommandParser.tokenize("PIN Com.Example.Mail"))
    }
}
