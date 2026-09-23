package com.droidcode

import android.content.pm.ActivityInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidcode.editor.EditorManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TabLifecycleTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun testTabLifecycleUnderConfigurationChange() {
        val editorMgr = EditorManager.getInstance()

        // Create initial editor tabs
        composeTestRule.runOnUiThread {
            editorMgr.closeAllTabs()
            val tab1 = editorMgr.openVirtualFile("MainActivity.kt", "fun main() {\n    println(\"Hello\")\n}")
            val tab2 = editorMgr.openVirtualFile("build.gradle.kts", "plugins { kotlin(\"jvm\") }")
            editorMgr.selectTab(tab1.id)
        }

        composeTestRule.waitForIdle()

        // Verify active tab before rotation
        val activeBefore = editorMgr.activeTab
        assertNotNull(activeBefore)
        assertEquals("MainActivity.kt", activeBefore?.title)
        assertEquals(2, editorMgr.openTabs.size)

        // Trigger configuration change / screen rotation
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        composeTestRule.waitForIdle()

        // Switch back to portrait
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        composeTestRule.waitForIdle()

        // Verify tabs persist across configuration changes
        val activeAfter = editorMgr.activeTab
        assertNotNull(activeAfter)
        assertEquals("MainActivity.kt", activeAfter?.title)
        assertEquals(2, editorMgr.openTabs.size)
    }
}
