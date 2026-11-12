package com.gybra.terminallauncher.ui.settings

import com.gybra.terminallauncher.MainDispatcherRule
import com.gybra.terminallauncher.launcher.DeviceLock
import com.gybra.terminallauncher.launcher.FakeDeviceLock
import com.gybra.terminallauncher.preferences.AccessibilityConsent
import com.gybra.terminallauncher.preferences.LauncherPreferences
import com.gybra.terminallauncher.preferences.PreferencesRepository
import com.gybra.terminallauncher.preferences.RecordingPreferencesRepository
import com.gybra.terminallauncher.theme.TerminalTheme
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccessibilityConsentViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `waits for stored preferences before showing the disclosure or running a gesture`() = runTest {
        val viewModel = SettingsViewModel(RecordingPreferencesRepository(), FakeDeviceLock())
        var actions = 0
        viewModel.performAccessibilityAction { actions += 1 }
        viewModel.acceptAccessibilityDisclosure()
        viewModel.dismissAccessibilityDisclosure()
        assertEquals(false, viewModel.uiState.value.accessibilityDisclosureVisible)
        assertEquals(0, actions)
        advanceUntilIdle()
        assertEquals(true, viewModel.uiState.value.accessibilityDisclosureVisible)
    }

    @Test
    fun `saved choices do not interrupt ordinary Home use`() = runTest {
        for (consent in listOf(AccessibilityConsent.GRANTED, AccessibilityConsent.DECLINED)) {
            val repository = RecordingPreferencesRepository(LauncherPreferences(accessibilityConsent = consent))
            val deviceLock = FakeDeviceLock()
            val viewModel = SettingsViewModel(repository, deviceLock)
            advanceUntilIdle()
            assertEquals(false, viewModel.uiState.value.accessibilityDisclosureVisible)
            viewModel.acceptAccessibilityDisclosure()
            viewModel.dismissAccessibilityDisclosure()
            advanceUntilIdle()
            assertEquals(emptyList<String>(), deviceLock.calls)
            assertEquals(emptyList<String>(), repository.writes)
        }
    }

    @Test
    fun `a declined gesture reopens disclosure without running the global action`() = runTest {
        val repository = RecordingPreferencesRepository(
            LauncherPreferences(accessibilityConsent = AccessibilityConsent.DECLINED),
        )
        val viewModel = SettingsViewModel(repository, FakeDeviceLock(enabled = true))
        var actions = 0
        advanceUntilIdle()
        viewModel.performAccessibilityAction { actions += 1 }
        assertEquals(0, actions)
        assertEquals(false, viewModel.uiState.value.doubleTapToLock)
        assertEquals(true, viewModel.uiState.value.accessibilityDisclosureVisible)
        repository.emit(
            LauncherPreferences(
                accessibilityConsent = AccessibilityConsent.DECLINED,
                terminalTheme = TerminalTheme.AMBER,
            ),
        )
        advanceUntilIdle()
        assertEquals(true, viewModel.uiState.value.accessibilityDisclosureVisible)
    }

    @Test
    fun `persists acceptance before settings and ignores duplicate choices while saving`() = runTest {
        val stored = RecordingPreferencesRepository()
        val release = CompletableDeferred<Unit>()
        val repository = object : PreferencesRepository by stored {
            override suspend fun setAccessibilityConsent(consent: AccessibilityConsent) {
                release.await()
                stored.setAccessibilityConsent(consent)
            }
        }
        val deviceLock = FakeDeviceLock()
        val viewModel = SettingsViewModel(repository, deviceLock)
        advanceUntilIdle()
        viewModel.acceptAccessibilityDisclosure()
        runCurrent()
        viewModel.acceptAccessibilityDisclosure()
        viewModel.dismissAccessibilityDisclosure()
        var actions = 0
        viewModel.performAccessibilityAction { actions += 1 }
        assertEquals(true, viewModel.uiState.value.savingAccessibilityConsent)
        assertEquals(emptyList<String>(), deviceLock.calls)
        assertEquals(0, actions)
        release.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf("setAccessibilityConsent(GRANTED)"), stored.writes)
        assertEquals(listOf("requestEnable"), deviceLock.calls)
        assertEquals(false, viewModel.uiState.value.accessibilityDisclosureVisible)
        assertEquals(false, viewModel.uiState.value.savingAccessibilityConsent)
        assertEquals(true, viewModel.uiState.value.doubleTapToLock)
        viewModel.performAccessibilityAction { actions += 1 }
        assertEquals(1, actions)
    }

    @Test
    fun `failed acceptance keeps disclosure open and never opens Android settings`() = runTest {
        val repository = RecordingPreferencesRepository(writeFailure = IOException("disk full"))
        val deviceLock = FakeDeviceLock(enabled = true)
        val viewModel = SettingsViewModel(repository, deviceLock)
        advanceUntilIdle()
        viewModel.acceptAccessibilityDisclosure()
        advanceUntilIdle()
        assertEquals(emptyList<String>(), deviceLock.calls)
        assertEquals(true, viewModel.uiState.value.accessibilityDisclosureVisible)
        assertEquals(false, viewModel.uiState.value.savingAccessibilityConsent)
        assertEquals(SettingsEntry.STORAGE_ERROR.label, viewModel.uiState.value.storageError)
        assertEquals(AccessibilityConsent.UNDECIDED, viewModel.uiState.value.accessibilityConsent)
        assertEquals(false, viewModel.uiState.value.doubleTapToLock)
    }

    @Test
    fun `declining is remembered without granting or disabling Android access`() = runTest {
        val repository = RecordingPreferencesRepository()
        val deviceLock = FakeDeviceLock(enabled = true)
        val viewModel = SettingsViewModel(repository, deviceLock)
        advanceUntilIdle()
        viewModel.dismissAccessibilityDisclosure()
        assertEquals(false, viewModel.uiState.value.accessibilityDisclosureVisible)
        advanceUntilIdle()
        assertEquals(emptyList<String>(), deviceLock.calls)
        assertEquals(AccessibilityConsent.DECLINED, viewModel.uiState.value.accessibilityConsent)
        val restored = SettingsViewModel(repository, deviceLock)
        advanceUntilIdle()
        assertEquals(false, restored.uiState.value.accessibilityDisclosureVisible)
        assertEquals(false, restored.uiState.value.doubleTapToLock)
    }

    @Test
    fun `a failed decline does not trap Home or reappear on unrelated preferences`() = runTest {
        val repository = RecordingPreferencesRepository(writeFailure = IOException("disk full"))
        val viewModel = SettingsViewModel(repository, FakeDeviceLock())
        advanceUntilIdle()
        viewModel.dismissAccessibilityDisclosure()
        advanceUntilIdle()
        assertEquals(false, viewModel.uiState.value.accessibilityDisclosureVisible)
        assertEquals(SettingsEntry.STORAGE_ERROR.label, viewModel.uiState.value.storageError)
        repository.emit(LauncherPreferences(terminalTheme = TerminalTheme.GREEN))
        advanceUntilIdle()
        assertEquals(false, viewModel.uiState.value.accessibilityDisclosureVisible)
        val restored = SettingsViewModel(repository, FakeDeviceLock())
        advanceUntilIdle()
        assertEquals(true, restored.uiState.value.accessibilityDisclosureVisible)
    }

    @Test
    fun `an already enabled legacy service is not affirmative consent`() = runTest {
        val viewModel = SettingsViewModel(RecordingPreferencesRepository(), FakeDeviceLock(enabled = true))
        advanceUntilIdle()
        var actions = 0
        viewModel.performAccessibilityAction { actions += 1 }
        viewModel.refreshDeviceLock()
        assertEquals(0, actions)
        assertEquals(false, viewModel.uiState.value.doubleTapToLock)
        assertEquals(true, viewModel.uiState.value.accessibilityDisclosureVisible)
    }

    @Test
    fun `settings navigation observes the persisted choice rather than only optimistic state`() = runTest {
        val repository = RecordingPreferencesRepository()
        val deviceLock = object : DeviceLock by FakeDeviceLock() {
            override fun requestEnable(): Boolean {
                assertEquals(listOf("setAccessibilityConsent(GRANTED)"), repository.writes)
                return false
            }
        }
        val viewModel = SettingsViewModel(repository, deviceLock)
        advanceUntilIdle()
        viewModel.acceptAccessibilityDisclosure()
        advanceUntilIdle()
        assertEquals(AccessibilityConsent.GRANTED, viewModel.uiState.value.accessibilityConsent)
        assertEquals(false, viewModel.uiState.value.doubleTapToLock)
    }
}
