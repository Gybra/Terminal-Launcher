package com.gybra.terminallauncher.ui.home

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.gybra.terminallauncher.launcher.InstalledApp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `renders application labels and forwards clicks`() {
        val app = InstalledApp(packageName = "com.example.browser", label = "Browser")
        var clickedApp: InstalledApp? = null

        composeRule.setContent {
            HomeScreen(
                state = HomeUiState(apps = listOf(app)),
                onAppClick = { clickedApp = it },
            )
        }

        composeRule
            .onNodeWithText("Browser")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertEquals(app, clickedApp)
    }

    @Test
    fun `renders no application rows for empty state`() {
        composeRule.setContent {
            HomeScreen(
                state = HomeUiState(),
                onAppClick = {},
            )
        }

        composeRule.onNodeWithText("Browser").assertDoesNotExist()
    }
}
