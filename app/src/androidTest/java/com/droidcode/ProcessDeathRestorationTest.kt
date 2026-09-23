package com.droidcode

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ProcessDeathRestorationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun testProcessDeathRestorationForWorkspaceState() {
        val restorationTester = StateRestorationTester(composeTestRule)

        var stateUpdater: (String) -> Unit = {}

        restorationTester.setContent {
            var activeWorkspacePath by rememberSaveable { mutableStateOf("/data/user/0/com.droidcode/files/MyProject") }
            var isWorkspaceOpen by rememberSaveable { mutableStateOf(true) }

            stateUpdater = { newPath ->
                activeWorkspacePath = newPath
                isWorkspaceOpen = newPath.isNotEmpty()
            }

            Text(
                text = if (isWorkspaceOpen) activeWorkspacePath else "NO_WORKSPACE",
                modifier = Modifier.testTag("workspace_state_text")
            )
        }

        // Verify initial state
        composeTestRule.onNodeWithTag("workspace_state_text").assertIsDisplayed()
        composeTestRule.onNodeWithTag("workspace_state_text").assertTextEquals("/data/user/0/com.droidcode/files/MyProject")

        // Update state prior to death
        composeTestRule.runOnIdle {
            stateUpdater("/storage/emulated/0/DroidCode/UpdatedProject")
        }
        composeTestRule.onNodeWithTag("workspace_state_text").assertTextEquals("/storage/emulated/0/DroidCode/UpdatedProject")

        // Trigger simulated process death and recreation
        restorationTester.emulateSavedInstanceStateRestore()

        // Assert restored value matches exactly
        composeTestRule.onNodeWithTag("workspace_state_text").assertIsDisplayed()
        composeTestRule.onNodeWithTag("workspace_state_text").assertTextEquals("/storage/emulated/0/DroidCode/UpdatedProject")
    }
}
