package com.droidcode

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidcode.core.Command
import com.droidcode.core.CommandRegistry
import com.droidcode.ui.CommandPaletteDialog
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CommandPaletteTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun testCommandPaletteSearchInputAndRanking() {
        val registry = CommandRegistry.getInstance()
        registry.registerCommand(Command("test.save", "Save Active Document", "File", "Ctrl+S") {})
        registry.registerCommand(Command("test.save_all", "Save All Files in Workspace", "File", "Ctrl+Shift+S") {})
        registry.registerCommand(Command("test.close", "Close Active Document", "File", "Ctrl+W") {})

        var executedCommand: Command? = null

        composeTestRule.setContent {
            CommandPaletteDialog(
                onDismiss = {},
                onExecuteCommand = { cmd -> executedCommand = cmd }
            )
        }

        // Search input must be present
        composeTestRule.onNodeWithTag("command_palette_search_input").assertIsDisplayed()

        // Filter by typing "Save"
        composeTestRule.onNodeWithTag("command_palette_search_input").performTextInput("Save")
        composeTestRule.waitForIdle()

        // Verify ranked results are shown in LazyColumn
        composeTestRule.onNodeWithText("Save Active Document").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save All Files in Workspace").assertIsDisplayed()

        // Verify programmatic search ranking
        val ranked = registry.searchCommands("Save")
        assertTrue("Ranked list should contain at least 2 items", ranked.size >= 2)
        assertTrue(
            "Primary exact match should rank first",
            ranked[0].title.contains("Save", ignoreCase = true)
        )
    }
}
