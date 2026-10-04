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
                backgroundColor = Color.parseColor("#1E1E1E"),
                gutterBackgroundColor = Color.parseColor("#252526"),
                gutterDividerColor = Color.parseColor("#333333"),
                lineNumberColor = Color.parseColor("#858585"),
                activeLineNumberColor = Color.parseColor("#C6C6C6"),
                currentLineBackgroundColor = Color.parseColor("#282828"),
                cursorColor = Color.parseColor("#569CD6"),
                selectionColor = Color.parseColor("#33264F78"),
                addedGutterColor = Color.parseColor("#10B981"),
                modifiedGutterColor = Color.parseColor("#3B82F6"),
                textColor = Color.parseColor("#D4D4D4"),
                keywordColor = Color.parseColor("#569CD6"),
                stringColor = Color.parseColor("#CE9178"),
                numberColor = Color.parseColor("#B5CEA8"),
                commentColor = Color.parseColor("#6A9955"),
                typeColor = Color.parseColor("#4EC9B0")
            )
        }

        fun oneDarkTheme(): EditorTheme {
            return EditorTheme(
                isDark = true,
                backgroundColor = Color.parseColor("#282C34"),
                gutterBackgroundColor = Color.parseColor("#21252B"),
                gutterDividerColor = Color.parseColor("#3B4048"),
                lineNumberColor = Color.parseColor("#5C6370"),
                activeLineNumberColor = Color.parseColor("#ABB2BF"),
                currentLineBackgroundColor = Color.parseColor("#2C313A"),
                cursorColor = Color.parseColor("#528BFF"),
                selectionColor = Color.parseColor("#4D3E4451"),
                addedGutterColor = Color.parseColor("#98C379"),
                modifiedGutterColor = Color.parseColor("#61AFEF"),
                textColor = Color.parseColor("#ABB2BF"),
                keywordColor = Color.parseColor("#C678DD"),
                stringColor = Color.parseColor("#98C379"),
                numberColor = Color.parseColor("#D19A66"),
                commentColor = Color.parseColor("#5C6370"),
                typeColor = Color.parseColor("#E5C07B")
            )
        }

        fun draculaTheme(): EditorTheme {
            return EditorTheme(
                isDark = true,
                backgroundColor = Color.parseColor("#282A36"),
                gutterBackgroundColor = Color.parseColor("#21222C"),
                gutterDividerColor = Color.parseColor("#44475A"),
                lineNumberColor = Color.parseColor("#6272A4"),
                activeLineNumberColor = Color.parseColor("#F8F8F2"),
                currentLineBackgroundColor = Color.parseColor("#343746"),
                cursorColor = Color.parseColor("#FF79C6"),
                selectionColor = Color.parseColor("#4D44475A"),
                addedGutterColor = Color.parseColor("#50FA7B"),
                modifiedGutterColor = Color.parseColor("#8BE9FD"),
                textColor = Color.parseColor("#F8F8F2"),
                keywordColor = Color.parseColor("#FF79C6"),
                stringColor = Color.parseColor("#F1FA8C"),
                numberColor = Color.parseColor("#BD93F9"),
                commentColor = Color.parseColor("#6272A4"),
                typeColor = Color.parseColor("#8BE9FD")
            )
        }

        fun monokaiTheme(): EditorTheme {
            return EditorTheme(
                isDark = true,
                backgroundColor = Color.parseColor("#272822"),
                gutterBackgroundColor = Color.parseColor("#1E1F1C"),
                gutterDividerColor = Color.parseColor("#3E3D32"),
                lineNumberColor = Color.parseColor("#75715E"),
                activeLineNumberColor = Color.parseColor("#F8F8F2"),
                currentLineBackgroundColor = Color.parseColor("#3E3D32"),
                cursorColor = Color.parseColor("#F8F8F0"),
                selectionColor = Color.parseColor("#4D49483E"),
                addedGutterColor = Color.parseColor("#A6E22E"),
                modifiedGutterColor = Color.parseColor("#66D9EF"),
                textColor = Color.parseColor("#F8F8F2"),
                keywordColor = Color.parseColor("#F92672"),
                stringColor = Color.parseColor("#E6DB74"),
                numberColor = Color.parseColor("#AE81FF"),
                commentColor = Color.parseColor("#75715E"),
                typeColor = Color.parseColor("#66D9EF")
            )
        }

        fun solarizedDarkTheme(): EditorTheme {
            return EditorTheme(
                isDark = true,
                backgroundColor = Color.parseColor("#002B36"),
                gutterBackgroundColor = Color.parseColor("#073642"),
                gutterDividerColor = Color.parseColor("#0F4A58"),
                lineNumberColor = Color.parseColor("#586E75"),
                activeLineNumberColor = Color.parseColor("#93A1A1"),
                currentLineBackgroundColor = Color.parseColor("#073642"),
                cursorColor = Color.parseColor("#2AA198"),
                selectionColor = Color.parseColor("#4D073642"),
                addedGutterColor = Color.parseColor("#859900"),
                modifiedGutterColor = Color.parseColor("#268BD2"),
                textColor = Color.parseColor("#839496"),
                keywordColor = Color.parseColor("#859900"),
                stringColor = Color.parseColor("#2AA198"),
                numberColor = Color.parseColor("#D33682"),
                commentColor = Color.parseColor("#586E75"),
                typeColor = Color.parseColor("#B58900")
            )
        }

        fun nordTheme(): EditorTheme {
            return EditorTheme(
                isDark = true,
                backgroundColor = Color.parseColor("#2E3440"),
                gutterBackgroundColor = Color.parseColor("#242933"),
                gutterDividerColor = Color.parseColor("#3B4252"),
                lineNumberColor = Color.parseColor("#4C566A"),
                activeLineNumberColor = Color.parseColor("#ECEFF4"),
                currentLineBackgroundColor = Color.parseColor("#3B4252"),
                cursorColor = Color.parseColor("#88C0D0"),
                selectionColor = Color.parseColor("#4D434C5E"),
                addedGutterColor = Color.parseColor("#A3BE8C"),
                modifiedGutterColor = Color.parseColor("#81A1C1"),
                textColor = Color.parseColor("#D8DEE9"),
                keywordColor = Color.parseColor("#81A1C1"),
                stringColor = Color.parseColor("#A3BE8C"),
                numberColor = Color.parseColor("#B48EAD"),
                commentColor = Color.parseColor("#616E88"),
                typeColor = Color.parseColor("#8FBCBB")
            )
        }

        fun lightTheme(): EditorTheme {
            return EditorTheme(
                isDark = false,
                backgroundColor = Color.parseColor("#FFFFFF"),
                gutterBackgroundColor = Color.parseColor("#F3F3F3"),
                gutterDividerColor = Color.parseColor("#E5E5E5"),
                lineNumberColor = Color.parseColor("#858585"),
                activeLineNumberColor = Color.parseColor("#000000"),
                currentLineBackgroundColor = Color.parseColor("#F5F5F5"),
                cursorColor = Color.parseColor("#007ACC"),
                selectionColor = Color.parseColor("#33ADD6FF"),
                addedGutterColor = Color.parseColor("#10B981"),
                modifiedGutterColor = Color.parseColor("#1976D2"),
                textColor = Color.parseColor("#000000"),
                keywordColor = Color.parseColor("#0000FF"),
                stringColor = Color.parseColor("#A31515"),
                numberColor = Color.parseColor("#098658"),
                commentColor = Color.parseColor("#008000"),
                typeColor = Color.parseColor("#267F99")
            )
        }

        fun solarizedLightTheme(): EditorTheme {
            return EditorTheme(
                isDark = false,
                backgroundColor = Color.parseColor("#FDF6E3"),
                gutterBackgroundColor = Color.parseColor("#EEE8D5"),
                gutterDividerColor = Color.parseColor("#E0D6BC"),
                lineNumberColor = Color.parseColor("#93A1A1"),
                activeLineNumberColor = Color.parseColor("#586E75"),
                currentLineBackgroundColor = Color.parseColor("#EEE8D5"),
                cursorColor = Color.parseColor("#268BD2"),
                selectionColor = Color.parseColor("#33EEE8D5"),
                addedGutterColor = Color.parseColor("#859900"),
                modifiedGutterColor = Color.parseColor("#268BD2"),
                textColor = Color.parseColor("#657B83"),
                keywordColor = Color.parseColor("#859900"),
                stringColor = Color.parseColor("#2AA198"),
                numberColor = Color.parseColor("#D33682"),
                commentColor = Color.parseColor("#93A1A1"),
                typeColor = Color.parseColor("#B58900")
            )
        }

        fun forThemeMode(
            mode: com.droidcode.settings.AppSettings.ThemeMode,
            isSystemDark: Boolean
        ): EditorTheme {
            return when (mode) {
                com.droidcode.settings.AppSettings.ThemeMode.DARK -> darkTheme()
                com.droidcode.settings.AppSettings.ThemeMode.ONE_DARK -> oneDarkTheme()
                com.droidcode.settings.AppSettings.ThemeMode.DRACULA -> draculaTheme()
                com.droidcode.settings.AppSettings.ThemeMode.MONOKAI -> monokaiTheme()
                com.droidcode.settings.AppSettings.ThemeMode.SOLARIZED_DARK -> solarizedDarkTheme()
                com.droidcode.settings.AppSettings.ThemeMode.NORD -> nordTheme()
                com.droidcode.settings.AppSettings.ThemeMode.LIGHT -> lightTheme()
                com.droidcode.settings.AppSettings.ThemeMode.SOLARIZED_LIGHT -> solarizedLightTheme()
                com.droidcode.settings.AppSettings.ThemeMode.SYSTEM -> if (isSystemDark) darkTheme() else lightTheme()
            }
        }
    }
}
