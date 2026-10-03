package com.droidcode.editor.engine

import android.graphics.Color

/**
 * High-contrast, accessibility-compliant color theme configuration for DroidCodeEngine.
 */
data class EditorTheme(
    val isDark: Boolean,
    val backgroundColor: Int,
    val gutterBackgroundColor: Int,
    val gutterDividerColor: Int,
    val lineNumberColor: Int,
    val activeLineNumberColor: Int,
    val currentLineBackgroundColor: Int,
    val cursorColor: Int,
    val selectionColor: Int,
    val addedGutterColor: Int,
    val modifiedGutterColor: Int,
    val textColor: Int,
    val keywordColor: Int,
    val stringColor: Int,
    val numberColor: Int,
    val commentColor: Int,
    val typeColor: Int
) {
    companion object {
        fun darkTheme(): EditorTheme {
            return EditorTheme(
                isDark = true,
                backgroundColor = Color.parseColor("#121212"),
                gutterBackgroundColor = Color.parseColor("#181818"),
                gutterDividerColor = Color.parseColor("#2A2A2A"),
                lineNumberColor = Color.parseColor("#666666"),
                activeLineNumberColor = Color.parseColor("#80D8FF"),
                currentLineBackgroundColor = Color.parseColor("#1A2530"),
                cursorColor = Color.parseColor("#00E5FF"),
                selectionColor = Color.parseColor("#334B6888"),
                addedGutterColor = Color.parseColor("#10B981"),
                modifiedGutterColor = Color.parseColor("#3B82F6"),
                textColor = Color.parseColor("#E0E0E0"),
                keywordColor = Color.parseColor("#CF92D7"),
                stringColor = Color.parseColor("#81C784"),
                numberColor = Color.parseColor("#FFB74D"),
                commentColor = Color.parseColor("#78909C"),
                typeColor = Color.parseColor("#64B5F6")
            )
        }

        fun lightTheme(): EditorTheme {
            return EditorTheme(
                isDark = false,
                backgroundColor = Color.parseColor("#FAFAFA"),
                gutterBackgroundColor = Color.parseColor("#F0F0F0"),
                gutterDividerColor = Color.parseColor("#E0E0E0"),
                lineNumberColor = Color.parseColor("#9E9E9E"),
                activeLineNumberColor = Color.parseColor("#0288D1"),
                currentLineBackgroundColor = Color.parseColor("#E1F5FE"),
                cursorColor = Color.parseColor("#0288D1"),
                selectionColor = Color.parseColor("#330288D1"),
                addedGutterColor = Color.parseColor("#10B981"),
                modifiedGutterColor = Color.parseColor("#1976D2"),
                textColor = Color.parseColor("#212121"),
                keywordColor = Color.parseColor("#8E24AA"),
                stringColor = Color.parseColor("#2E7D32"),
                numberColor = Color.parseColor("#E65100"),
                commentColor = Color.parseColor("#546E7A"),
                typeColor = Color.parseColor("#1565C0")
            )
        }
    }
}
