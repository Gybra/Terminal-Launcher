package com.gybra.terminallauncher.shell

/**
 * What a shell writes around a block of rows Home keeps, such as the pinned applications: the
 * lines printed above it and the lines printed below it. A shell that announces a block before
 * listing it fills [above], one that closes it with a count fills [below], and a shell that says
 * nothing leaves both empty.
 */
public data class SectionLines(
    public val above: List<String> = emptyList(),
    public val below: List<String> = emptyList(),
)
