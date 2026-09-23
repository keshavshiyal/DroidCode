package com.droidcode

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidcode.settings.AppSettings
import com.droidcode.ui.DeveloperQuickKeyBar
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class QuickKeyBarTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun testQuickKeyBarModifierTogglesAndTextInsertion() {
        var ctrlActive by mutableStateOf(false)
        var shiftActive by mutableStateOf(false)
        var altActive by mutableStateOf(false)
        var insertedText by mutableStateOf("")
        var triggeredAction by mutableStateOf("")

        composeTestRule.setContent {
            DeveloperQuickKeyBar(
                density = AppSettings.KeyBarDensity.NORMAL,
                ctrlActive = ctrlActive,
                shiftActive = shiftActive,
                altActive = altActive,
                onToggleCtrl = { ctrlActive = !ctrlActive },
                onToggleShift = { shiftActive = !shiftActive },
                onToggleAlt = { altActive = !altActive },
                onInsertText = { text -> insertedText = text },
                onActionKey = { action -> triggeredAction = action }
            )
        }

        // Verify modifier buttons exist and are displayed
        composeTestRule.onNodeWithTag("quick_key_ctrl").assertIsDisplayed()
        composeTestRule.onNodeWithTag("quick_key_shift").assertIsDisplayed()
        composeTestRule.onNodeWithTag("quick_key_alt").assertIsDisplayed()

        // Toggle Ctrl
        composeTestRule.onNodeWithTag("quick_key_ctrl").performClick()
        composeTestRule.waitForIdle()
        assertTrue("Ctrl should be active after click", ctrlActive)

        // Toggle Shift
        composeTestRule.onNodeWithTag("quick_key_shift").performClick()
        composeTestRule.waitForIdle()
        assertTrue("Shift should be active after click", shiftActive)

        // Perform Tab insertion
        composeTestRule.onNodeWithTag("quick_key_tab").assertIsDisplayed()
        composeTestRule.onNodeWithTag("quick_key_tab").performClick()
        composeTestRule.waitForIdle()
        assertEquals("    ", insertedText)

        // Perform Action key (Undo)
        composeTestRule.onNodeWithTag("quick_key_undo").assertIsDisplayed()
        composeTestRule.onNodeWithTag("quick_key_undo").performClick()
        composeTestRule.waitForIdle()
        assertEquals("UNDO", triggeredAction)
    }
}
