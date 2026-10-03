package com.droidcode.ui

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.res.ResourcesCompat
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

    fun getTypeface(context: Context, fontOption: AppSettings.EditorFontFamily?): Typeface {
        val resId = when (fontOption) {
            AppSettings.EditorFontFamily.JETBRAINS_MONO -> R.font.jetbrains_mono
            AppSettings.EditorFontFamily.FIRA_CODE -> R.font.fira_code
            AppSettings.EditorFontFamily.SOURCE_CODE_PRO -> R.font.source_code_pro
            AppSettings.EditorFontFamily.ROBOTO_MONO -> R.font.roboto_mono
            AppSettings.EditorFontFamily.SYSTEM_MONOSPACE -> null
            else -> R.font.jetbrains_mono
        }
        return if (resId != null) {
            try {
                ResourcesCompat.getFont(context, resId) ?: Typeface.MONOSPACE
            } catch (_: Exception) {
                Typeface.MONOSPACE
            }
        } else {
            Typeface.MONOSPACE
        }
    }
}
