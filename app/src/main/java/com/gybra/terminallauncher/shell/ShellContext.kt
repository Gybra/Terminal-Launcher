package com.gybra.terminallauncher.shell

/**
 * What the active shell profile writes a prompt from. The cosmetic options are shared by both
 * profiles, and each profile honours only the ones its own style has: the Unix profile writes the
 * identity and [promptSymbol], the DOS profile writes [dosDrive], and both obey [showPath].
 */
public data class ShellContext(
    public val username: String,
    public val hostname: String,
    public val location: LauncherLocation,
    public val promptSymbol: PromptSymbol = PromptSymbol.DOLLAR,
    public val showPath: Boolean = true,
    public val dosDrive: DosDrive = DosDrive.C,
)
