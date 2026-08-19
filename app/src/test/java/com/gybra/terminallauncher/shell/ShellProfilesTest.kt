package com.gybra.terminallauncher.shell

import com.gybra.terminallauncher.shell.dos.DosShellProfile
import com.gybra.terminallauncher.shell.unix.UnixShellProfile
import org.junit.Assert.assertSame
import org.junit.Test

class ShellProfilesTest {
    @Test
    fun `selects the profile matching each shell type`() {
        assertSame(DosShellProfile, ShellProfiles.forType(ShellType.DOS))
        assertSame(UnixShellProfile, ShellProfiles.forType(ShellType.UNIX))
    }
}
