package com.droidcode.settings;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {

    private static final String PREF_NAME = "droidcode_settings";
    private static final String KEY_THEME = "theme_mode";
    private static final String KEY_FONT_SIZE = "font_size_sp";
    private static final String KEY_WORD_WRAP = "word_wrap";
    private static final String KEY_KEYBAR_ENABLE = "keybar_enable";
    private static final String KEY_KEYBAR_DENSITY = "keybar_density";
    private static final String KEY_LINE_NUMBERS = "line_numbers";
    private static final String KEY_FONT_FAMILY = "font_family";

    private static volatile SettingsManager INSTANCE;

    private final AppSettings currentSettings;

    private SettingsManager(Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        String themeStr = prefs.getString(KEY_THEME, AppSettings.ThemeMode.DARK.name());
        AppSettings.ThemeMode themeMode;
        try {
            themeMode = AppSettings.ThemeMode.valueOf(themeStr);
        } catch (Exception e) {
            themeMode = AppSettings.ThemeMode.DARK;
        }

        int fontSize = prefs.getInt(KEY_FONT_SIZE, 14);
        boolean wordWrap = prefs.getBoolean(KEY_WORD_WRAP, true);
        boolean keyBarEnable = prefs.getBoolean(KEY_KEYBAR_ENABLE, true);

        String densityStr = prefs.getString(KEY_KEYBAR_DENSITY, AppSettings.KeyBarDensity.NORMAL.name());
        AppSettings.KeyBarDensity density;
        try {
            density = AppSettings.KeyBarDensity.valueOf(densityStr);
        } catch (Exception e) {
            density = AppSettings.KeyBarDensity.NORMAL;
        }

        boolean lineNumbers = prefs.getBoolean(KEY_LINE_NUMBERS, true);

        String fontFamStr = prefs.getString(KEY_FONT_FAMILY, AppSettings.EditorFontFamily.JETBRAINS_MONO.name());
        AppSettings.EditorFontFamily fontFamily;
        try {
            fontFamily = AppSettings.EditorFontFamily.valueOf(fontFamStr);
        } catch (Exception e) {
            fontFamily = AppSettings.EditorFontFamily.JETBRAINS_MONO;
        }

        this.currentSettings = new AppSettings(themeMode, fontSize, wordWrap, keyBarEnable, density, lineNumbers, fontFamily);
    }

    public static SettingsManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (SettingsManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SettingsManager(context);
                }
            }
        }
        return INSTANCE;
    }

    public AppSettings getSettings() {
        return currentSettings;
    }

    public AppSettings getSettingsCopy() {
        return new AppSettings(
                currentSettings.getThemeMode(),
                currentSettings.getFontSizeSp(),
                currentSettings.isWordWrap(),
                currentSettings.isQuickKeyBarEnabled(),
                currentSettings.getQuickKeyBarDensity(),
                currentSettings.isLineNumbersEnabled(),
                currentSettings.getEditorFontFamily()
        );
    }

    public void saveSettings(Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_THEME, currentSettings.getThemeMode().name())
                .putInt(KEY_FONT_SIZE, currentSettings.getFontSizeSp())
                .putBoolean(KEY_WORD_WRAP, currentSettings.isWordWrap())
                .putBoolean(KEY_KEYBAR_ENABLE, currentSettings.isQuickKeyBarEnabled())
                .putString(KEY_KEYBAR_DENSITY, currentSettings.getQuickKeyBarDensity().name())
                .putBoolean(KEY_LINE_NUMBERS, currentSettings.isLineNumbersEnabled())
                .putString(KEY_FONT_FAMILY, currentSettings.getEditorFontFamily().name())
                .apply();
    }
}
