package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.settings.AppSettings
import com.example.settings.SettingsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsManagerTest {

    @Test
    fun testSettingsDefaultValuesAndPersistence() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settingsMgr = SettingsManager.getInstance(context)

        val settings = settingsMgr.settings
        assertTrue(settings.fontSizeSp > 0)
        assertTrue(settings.isWordWrap)
        assertTrue(settings.isQuickKeyBarEnabled)

        // Modify settings
        settings.fontSizeSp = 20
        settings.themeMode = AppSettings.ThemeMode.LIGHT
        settings.editorFontFamily = AppSettings.EditorFontFamily.FIRA_CODE
        settingsMgr.saveSettings(context)

        assertEquals(20, settingsMgr.settings.fontSizeSp)
        assertEquals(AppSettings.ThemeMode.LIGHT, settingsMgr.settings.themeMode)
        assertEquals(AppSettings.EditorFontFamily.FIRA_CODE, settingsMgr.settings.editorFontFamily)

        // Verify copy preserves values
        val copy = settingsMgr.settingsCopy
        assertEquals(20, copy.fontSizeSp)
        assertEquals(AppSettings.ThemeMode.LIGHT, copy.themeMode)
        assertEquals(AppSettings.EditorFontFamily.FIRA_CODE, copy.editorFontFamily)
    }
}
