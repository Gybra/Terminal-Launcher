package com.gybra.terminallauncher.command

/**
 * Splits prompt input into a command token followed by its arguments. Tokens are separated by
 * whitespace; quoting and escaping are deliberately unsupported because the launcher never
 * forwards input to a real shell.
 */
public object CommandParser {
    public fun tokenize(input: String): List<String> =
        input.trim().split(WHITESPACE).filter(String::isNotEmpty)

    private val WHITESPACE = Regex("\\s+")
}
