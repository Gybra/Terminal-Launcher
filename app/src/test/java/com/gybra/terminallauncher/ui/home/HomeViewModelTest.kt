package com.gybra.terminallauncher.ui.home

import com.gybra.terminallauncher.MainDispatcherRule
import com.gybra.terminallauncher.launcher.AppRepository
import com.gybra.terminallauncher.launcher.InstalledApp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loads installed applications into immutable UI state`() = runTest(mainDispatcherRule.dispatcher) {
        val apps = listOf(
            InstalledApp(packageName = "com.example.browser", label = "Browser"),
        )
        val viewModel = HomeViewModel(FakeAppRepository(apps = apps))

        advanceUntilIdle()

        assertEquals(HomeUiState(apps = apps), viewModel.uiState.value)
    }

    @Test
    fun `keeps an empty state when package access is denied`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = HomeViewModel(FakeAppRepository(failure = SecurityException("denied")))

        advanceUntilIdle()

        assertEquals(HomeUiState(), viewModel.uiState.value)
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

        override fun observeInstalledApps(): Flow<List<InstalledApp>> = flowOf(apps)
    }
}
