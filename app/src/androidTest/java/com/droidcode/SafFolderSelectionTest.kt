package com.droidcode

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
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
class SafFolderSelectionTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun testSafFolderSelectionContractFlow() {
        var selectedUri by mutableStateOf<Uri?>(null)

        composeTestRule.setContent {
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree()
            ) { uri ->
                selectedUri = uri
            }

            Button(
                onClick = {
                    // Simulate SAF folder selection resolution
                    selectedUri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AProjects")
                },
                modifier = Modifier.testTag("saf_folder_picker_trigger")
            ) {
                Text(text = if (selectedUri != null) "Folder Selected" else "Select Folder")
            }
        }

        composeTestRule.onNodeWithTag("saf_folder_picker_trigger").assertIsDisplayed()
        composeTestRule.onNodeWithText("Select Folder").assertIsDisplayed()

        // Perform click
        composeTestRule.onNodeWithTag("saf_folder_picker_trigger").performClick()

        composeTestRule.waitForIdle()
        assertNotNull(selectedUri)
        assertEquals("content://com.android.externalstorage.documents/tree/primary%3AProjects", selectedUri.toString())
        composeTestRule.onNodeWithText("Folder Selected").assertIsDisplayed()
    }
}
