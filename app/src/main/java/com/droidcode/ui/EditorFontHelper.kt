package com.droidcode.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.droidcode.R
import com.droidcode.settings.AppSettings

object EditorFontHelper {

    fun getFontFamily(fontOption: AppSettings.EditorFontFamily?): FontFamily {
        return when (fontOption) {
            AppSettings.EditorFontFamily.JETBRAINS_MONO -> FontFamily(
                Font(R.font.jetbrains_mono, FontWeight.Normal)
            )
            AppSettings.EditorFontFamily.FIRA_CODE -> FontFamily(
                Font(R.font.fira_code, FontWeight.Normal)
            )
            AppSettings.EditorFontFamily.SOURCE_CODE_PRO -> FontFamily(
                Font(R.font.source_code_pro, FontWeight.Normal)
            )
            AppSettings.EditorFontFamily.ROBOTO_MONO -> FontFamily(
                Font(R.font.roboto_mono, FontWeight.Normal)
            )
            AppSettings.EditorFontFamily.SYSTEM_MONOSPACE -> FontFamily.Monospace
            else -> FontFamily(
                Font(R.font.jetbrains_mono, FontWeight.Normal)
            )
        }
    }
}
