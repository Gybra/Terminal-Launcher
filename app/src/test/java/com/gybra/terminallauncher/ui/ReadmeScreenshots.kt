package com.gybra.terminallauncher.ui

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.gybra.terminallauncher.command.Command
import com.gybra.terminallauncher.launcher.BatteryStatus
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.search.SearchResult
import com.gybra.terminallauncher.shell.DosDrive
import com.gybra.terminallauncher.shell.LauncherLocation
import com.gybra.terminallauncher.shell.PromptSymbol
import com.gybra.terminallauncher.shell.ShellContext
import com.gybra.terminallauncher.shell.ShellProfile
import com.gybra.terminallauncher.shell.dos.DosShellProfile
import com.gybra.terminallauncher.shell.unix.UnixShellProfile
import com.gybra.terminallauncher.theme.TerminalTheme
import com.gybra.terminallauncher.ui.home.HomeScreen
import com.gybra.terminallauncher.ui.home.HomeUiState
import com.gybra.terminallauncher.ui.home.PromptActions
import com.gybra.terminallauncher.ui.home.PromptState
import com.gybra.terminallauncher.ui.home.TerminalEntry
import com.gybra.terminallauncher.ui.settings.SettingsActions
import com.gybra.terminallauncher.ui.settings.SettingsScreen
import com.gybra.terminallauncher.ui.settings.SettingsUiState
import com.gybra.terminallauncher.ui.theme.TerminalThemeProvider
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Writes the pictures the README shows, from the same composables the launcher renders, so an
 * interface change that is not reflected in them arrives as a diff instead of a stale image.
 * Every picture is built from the shell profile it is written in, never from copied text.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-port-notnight-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReadmeScreenshots {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `writes the Unix home`() {
        val profile = UnixShellProfile

        capture("home-unix", TerminalTheme.GREEN) {
            HomeScreen(
                state = homeState(
                    profile = profile,
                    apps = INSTALLED.take(3),
                    history = listOf(
                        listedApplications(id = 1L, profile = profile),
                        reportedBattery(id = 2L, profile = profile),
                    ),
                    searchResults = listOf(
                        SearchResult(INSTALLED.first(), SearchResult.Match.PREFIX),
                    ),
                    prompt = PromptState(input = "fire"),
                ),
                onAppClick = {},
                onShortcutClick = {},
                onLockScreen = {},
                promptActions = emptyPromptActions(),
            )
        }
    }

    @Test
    fun `writes the DOS home`() {
        val profile = DosShellProfile

        capture("home-dos", TerminalTheme.AMBER) {
            HomeScreen(
                state = homeState(
                    profile = profile,
                    apps = INSTALLED.take(3),
                    history = listOf(
                        listedApplications(id = 1L, profile = profile),
                        reportedBattery(id = 2L, profile = profile),
                    ),
                ),
                onAppClick = {},
                onShortcutClick = {},
                onLockScreen = {},
                promptActions = emptyPromptActions(),
            )
        }
    }

    /** What `ls` and `DIR` answer, written by the profile rather than copied into the picture. */
    private fun listedApplications(id: Long, profile: ShellProfile): TerminalEntry = TerminalEntry(
        id = id,
        input = profile.aliasFor(Command.LIST_APPS),
        output = profile.formatAppList(INSTALLED),
    )

    private fun reportedBattery(id: Long, profile: ShellProfile): TerminalEntry = TerminalEntry(
        id = id,
        input = profile.aliasFor(Command.BATTERY),
        output = listOf(profile.formatBattery(BATTERY)),
    )

    @Test
    fun `writes the settings screen`() {
        capture("settings", TerminalTheme.SOLARIZED) {
            SettingsScreen(
                state = SettingsUiState(
                    shellProfile = UnixShellProfile,
                    terminalTheme = TerminalTheme.SOLARIZED,
                    showClock = true,
                    showBattery = true,
                    immersiveMode = true,
                    doubleTapToLock = false,
                    username = "user",
                    hostname = "android",
                    promptSymbol = PromptSymbol.DOLLAR,
                    showPromptPath = true,
                    dosDrive = DosDrive.C,
                ),
                actions = emptySettingsActions(),
                onBack = {},
            )
        }
    }

    private fun capture(name: String, theme: TerminalTheme, content: @Composable () -> Unit) {
        composeRule.setContent {
            TerminalThemeProvider(theme = theme, content = content)
        }
        composeRule.waitForIdle()

        val picture = screenshotsDirectory().resolve("$name.png")
        picture.outputStream().use { stream ->
            composeRule.onRoot()
                .captureToImage()
                .asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, stream)
        }

        assertTrue("Expected $picture to be written", picture.length() > 0)
    }

    /** The committed directory the README reads its pictures from, found from the Gradle root. */
    private fun screenshotsDirectory(): File {
        var directory = File("").absoluteFile
        while (!File(directory, "settings.gradle.kts").exists()) {
            directory = directory.parentFile ?: error("No Gradle root above ${File("").absolutePath}")
        }

        return File(directory, "docs/screenshots").apply { mkdirs() }
    }

    private fun homeState(
        profile: ShellProfile,
        apps: List<InstalledApp> = emptyList(),
        history: List<TerminalEntry> = emptyList(),
        searchResults: List<SearchResult> = emptyList(),
        prompt: PromptState = PromptState(),
    ): HomeUiState = HomeUiState(
        shellProfile = profile,
        shellContext = ShellContext(
            username = "user",
            hostname = "android",
            location = LauncherLocation.HOME,
        ),
        apps = apps,
        history = history,
        searchResults = searchResults,
        prompt = prompt,
        statusClock = "09:41",
        statusBattery = profile.formatBattery(BATTERY),
    )

    private fun emptyPromptActions(): PromptActions = PromptActions(
        updateValue = {},
        updateFocus = {},
        submit = {},
        writeShortcutCommand = {},
    )

    private fun emptySettingsActions(): SettingsActions = SettingsActions(
        selectShell = {},
        selectTheme = {},
        setShowClock = {},
        setShowBattery = {},
        setImmersiveMode = {},
        setDoubleTapToLock = {},
        setUsername = {},
        setHostname = {},
        selectPromptSymbol = {},
        setShowPromptPath = {},
        selectDosDrive = {},
    )
}

private val INSTALLED = listOf(
    InstalledApp("org.mozilla.firefox", "firefox"),
    InstalledApp("org.telegram.messenger", "telegram"),
    InstalledApp("com.android.camera", "camera"),
    InstalledApp("com.google.android.apps.maps", "maps"),
    InstalledApp("org.thoughtcrime.securesms", "signal"),
    InstalledApp("com.spotify.music", "spotify"),
    InstalledApp("com.android.deskclock", "clock"),
    InstalledApp("com.android.calculator2", "calculator"),
)

private val BATTERY = BatteryStatus(percentage = 87, charging = true)

private const val PNG_QUALITY = 100
