package com.gybra.terminallauncher.ui.home

import com.gybra.terminallauncher.MainDispatcherRule
import com.gybra.terminallauncher.launcher.AppRepository
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.preferences.LauncherPreferences
import com.gybra.terminallauncher.preferences.PreferencesRepository
import com.gybra.terminallauncher.shell.ShellType
import com.gybra.terminallauncher.shell.dos.DosShellProfile
import com.gybra.terminallauncher.shell.unix.UnixShellProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `shows only installed applications pinned in preferences`() = runTest(mainDispatcherRule.dispatcher) {
        val apps = listOf(
            InstalledApp(packageName = "com.example.browser", label = "Browser"),
            InstalledApp(packageName = "com.example.mail", label = "Mail"),
        )
        val preferencesRepository = FakePreferencesRepository(
            initialPreferences = LauncherPreferences(
                pinnedPackages = setOf("com.example.mail", "com.example.removed"),
            ),
        )
        val viewModel = HomeViewModel(
            appRepository = FakeAppRepository(apps = apps),
            preferencesRepository = preferencesRepository,
        )

        advanceUntilIdle()

        assertEquals(unixHomeState(apps = listOf(apps[1])), viewModel.uiState.value)
    }

    @Test
    fun `reacts when pinned package preferences change`() = runTest(mainDispatcherRule.dispatcher) {
        val app = InstalledApp(packageName = "com.example.mail", label = "Mail")
        val preferencesRepository = FakePreferencesRepository()
        val viewModel = HomeViewModel(
            appRepository = FakeAppRepository(apps = listOf(app)),
            preferencesRepository = preferencesRepository,
        )
        advanceUntilIdle()

        assertEquals(unixHomeState(), viewModel.uiState.value)

        preferencesRepository.emit(
            LauncherPreferences(pinnedPackages = setOf(app.packageName)),
        )
        advanceUntilIdle()

        assertEquals(unixHomeState(apps = listOf(app)), viewModel.uiState.value)
    }

    @Test
    fun `reacts when the selected shell changes`() = runTest(mainDispatcherRule.dispatcher) {
        val app = InstalledApp(packageName = "com.example.mail", label = "Mail")
        val preferencesRepository = FakePreferencesRepository(
            initialPreferences = LauncherPreferences(
                pinnedPackages = setOf(app.packageName),
            ),
        )
        val viewModel = HomeViewModel(
            appRepository = FakeAppRepository(apps = listOf(app)),
            preferencesRepository = preferencesRepository,
        )
        advanceUntilIdle()

        preferencesRepository.emit(
            LauncherPreferences(
                shellType = ShellType.DOS,
                pinnedPackages = setOf(app.packageName),
            ),
        )
        advanceUntilIdle()

        assertEquals(
            HomeUiState(
                apps = listOf(app),
                shellProfile = DosShellProfile,
            ),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `keeps an empty state when package access is denied`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = HomeViewModel(
            appRepository = FakeAppRepository(failure = SecurityException("denied")),
            preferencesRepository = FakePreferencesRepository(
                initialPreferences = LauncherPreferences(
                    pinnedPackages = setOf("com.example.mail"),
                ),
            ),
        )

        advanceUntilIdle()

        assertEquals(unixHomeState(), viewModel.uiState.value)
    }

    @Test
    fun `propagates unexpected application repository failures`() {
        assertThrows(IllegalStateException::class.java) {
            runTest(mainDispatcherRule.dispatcher) {
                HomeViewModel(
                    appRepository = FakeAppRepository(
                        failure = IllegalStateException("broken repository"),
                    ),
                    preferencesRepository = FakePreferencesRepository(),
                )

                advanceUntilIdle()
            }
        }
    }

    private class FakeAppRepository(
        private val apps: List<InstalledApp> = emptyList(),
        private val failure: Throwable? = null,
    ) : AppRepository {
        override suspend fun getInstalledApps(): List<InstalledApp> {
            failure?.let { throw it }
            return apps
        }

        override suspend fun findApp(query: String): InstalledApp? = null

        override fun observeInstalledApps(): Flow<List<InstalledApp>> = flow {
            failure?.let { throw it }
            emit(apps)
        }
    }

    private fun unixHomeState(apps: List<InstalledApp> = emptyList()): HomeUiState = HomeUiState(
        apps = apps,
        shellProfile = UnixShellProfile,
    )

    private class FakePreferencesRepository(
        initialPreferences: LauncherPreferences = LauncherPreferences(),
    ) : PreferencesRepository {
        private val mutablePreferences = MutableStateFlow(initialPreferences)
        override val preferences: Flow<LauncherPreferences> = mutablePreferences

        fun emit(preferences: LauncherPreferences) {
            mutablePreferences.value = preferences
        }

        override suspend fun setShellType(shellType: ShellType) = unsupported()

        override suspend fun setShowClock(showClock: Boolean) = unsupported()

        override suspend fun setUsername(username: String) = unsupported()

        override suspend fun setHostname(hostname: String) = unsupported()

        override suspend fun pinPackage(packageName: String) = unsupported()

        override suspend fun unpinPackage(packageName: String) = unsupported()

        private fun unsupported(): Nothing = error("Not required by this test")
    }
}
