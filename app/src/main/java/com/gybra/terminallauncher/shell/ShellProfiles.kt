package com.gybra.terminallauncher.shell

import com.gybra.terminallauncher.shell.dos.DosShellProfile
import com.gybra.terminallauncher.shell.unix.UnixShellProfile

public object ShellProfiles {
    public fun forType(shellType: ShellType): ShellProfile = when (shellType) {
        ShellType.DOS -> DosShellProfile
        ShellType.UNIX -> UnixShellProfile
    }
}
