package com.example.settings;

public class AppSettings {

    public enum ThemeMode {
        DARK("Dark (Deep Neutral)"),
        LIGHT("Light (Soft Neutral)"),
        SYSTEM("Follow System");

        private final String label;

        ThemeMode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum KeyBarDensity {
        COMPACT(36),
        NORMAL(44),
        COMFORTABLE(52);

        private final int heightDp;

        KeyBarDensity(int heightDp) {
            this.heightDp = heightDp;
        }

        public int getHeightDp() {
            return heightDp;
        }
    }

    private ThemeMode themeMode;
    private int fontSizeSp;
    private boolean wordWrap;
    private boolean quickKeyBarEnabled;
    private KeyBarDensity quickKeyBarDensity;
    private boolean lineNumbersEnabled;

    public AppSettings(ThemeMode themeMode, int fontSizeSp, boolean wordWrap, boolean quickKeyBarEnabled, KeyBarDensity quickKeyBarDensity, boolean lineNumbersEnabled) {
        this.themeMode = themeMode != null ? themeMode : ThemeMode.DARK;
        this.fontSizeSp = fontSizeSp > 0 ? fontSizeSp : 14;
        this.wordWrap = wordWrap;
        this.quickKeyBarEnabled = quickKeyBarEnabled;
        this.quickKeyBarDensity = quickKeyBarDensity != null ? quickKeyBarDensity : KeyBarDensity.NORMAL;
        this.lineNumbersEnabled = lineNumbersEnabled;
    }

    public static AppSettings createDefault() {
        return new AppSettings(ThemeMode.DARK, 14, true, true, KeyBarDensity.NORMAL, true);
    }

    public ThemeMode getThemeMode() {
        return themeMode;
    }

    public void setThemeMode(ThemeMode themeMode) {
        this.themeMode = themeMode;
    }

    public int getFontSizeSp() {
        return fontSizeSp;
    }

    public void setFontSizeSp(int fontSizeSp) {
        this.fontSizeSp = fontSizeSp;
    }

    public boolean isWordWrap() {
        return wordWrap;
    }

    public void setWordWrap(boolean wordWrap) {
        this.wordWrap = wordWrap;
    }

    public boolean isQuickKeyBarEnabled() {
        return quickKeyBarEnabled;
    }

    public void setQuickKeyBarEnabled(boolean quickKeyBarEnabled) {
        this.quickKeyBarEnabled = quickKeyBarEnabled;
    }

    public KeyBarDensity getQuickKeyBarDensity() {
        return quickKeyBarDensity;
    }

    public void setQuickKeyBarDensity(KeyBarDensity quickKeyBarDensity) {
        this.quickKeyBarDensity = quickKeyBarDensity;
    }

    public boolean isLineNumbersEnabled() {
        return lineNumbersEnabled;
    }

    public void setLineNumbersEnabled(boolean lineNumbersEnabled) {
        this.lineNumbersEnabled = lineNumbersEnabled;
    }
}
