package com.gybra.terminallauncher.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.gybra.terminallauncher.theme.BadgeSize
import java.io.File
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BadgeColorsTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun `independent colors persist and reset to theme defaults`() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(folder.root, "badge.preferences_pb")
        }
        val repository = DataStorePreferencesRepository(store)
        repository.setBadgeBackground("#123456")
        repository.setBadgeText("#ABCDEF")
        assertEquals("#123456", repository.preferences.first().badgeBackground)
        assertEquals("#ABCDEF", repository.preferences.first().badgeText)
        repository.setBadgeText(null)
        assertEquals(null, repository.preferences.first().badgeText)
        assertEquals("#123456", repository.preferences.first().badgeBackground)
    }

    @Test fun `invalid colors cannot be persisted`() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(folder.root, "invalid.preferences_pb")
        }
        val repository = DataStorePreferencesRepository(store)
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { repository.setBadgeText("red") }
        }
    }

    @Test fun `badge size persists and an unknown step keeps the current size`() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(folder.root, "size.preferences_pb")
        }
        val repository = DataStorePreferencesRepository(store)
        repository.setBadgeSize(BadgeSize.THREE)
        assertEquals(BadgeSize.THREE, repository.preferences.first().badgeSize)
        store.edit { preferences -> preferences[intPreferencesKey("badge_size")] = 9 }
        assertEquals(BadgeSize.TWO, repository.preferences.first().badgeSize)
    }
}
