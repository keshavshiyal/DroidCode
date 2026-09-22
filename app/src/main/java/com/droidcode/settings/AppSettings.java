package com.droidcode.settings;

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

    public enum EditorFontFamily {
        JETBRAINS_MONO("JetBrains Mono", "Popular developer font optimized for high code legibility"),
        FIRA_CODE("Fira Code", "Monospaced font with programming symbol ligatures"),
        SOURCE_CODE_PRO("Source Code Pro", "Clean Adobe monospace font designed for coding interfaces"),
        ROBOTO_MONO("Roboto Mono", "Google monospaced font tailored for Android development"),
        SYSTEM_MONOSPACE("System Monospace", "Standard Android system default monospace font");

        private final String label;
        private final String description;

        EditorFontFamily(String label, String description) {
            this.label = label;
            this.description = description;
        }

        public String getLabel() {
            return label;
        }

        public String getDescription() {
            return description;
        }
    }

    private ThemeMode themeMode;
    private int fontSizeSp;
    private boolean wordWrap;
    private boolean quickKeyBarEnabled;
    private KeyBarDensity quickKeyBarDensity;
    private boolean lineNumbersEnabled;
    private EditorFontFamily editorFontFamily;

    public AppSettings(ThemeMode themeMode, int fontSizeSp, boolean wordWrap, boolean quickKeyBarEnabled, KeyBarDensity quickKeyBarDensity, boolean lineNumbersEnabled, EditorFontFamily editorFontFamily) {
        this.themeMode = themeMode != null ? themeMode : ThemeMode.DARK;
        this.fontSizeSp = fontSizeSp > 0 ? fontSizeSp : 14;
        this.wordWrap = wordWrap;
        this.quickKeyBarEnabled = quickKeyBarEnabled;
        this.quickKeyBarDensity = quickKeyBarDensity != null ? quickKeyBarDensity : KeyBarDensity.NORMAL;
        this.lineNumbersEnabled = lineNumbersEnabled;
        this.editorFontFamily = editorFontFamily != null ? editorFontFamily : EditorFontFamily.JETBRAINS_MONO;
    }

    public AppSettings(ThemeMode themeMode, int fontSizeSp, boolean wordWrap, boolean quickKeyBarEnabled, KeyBarDensity quickKeyBarDensity, boolean lineNumbersEnabled) {
        this(themeMode, fontSizeSp, wordWrap, quickKeyBarEnabled, quickKeyBarDensity, lineNumbersEnabled, EditorFontFamily.JETBRAINS_MONO);
    }

    public static AppSettings createDefault() {
        return new AppSettings(ThemeMode.DARK, 14, true, true, KeyBarDensity.NORMAL, true, EditorFontFamily.JETBRAINS_MONO);
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

    public EditorFontFamily getEditorFontFamily() {
        return editorFontFamily != null ? editorFontFamily : EditorFontFamily.JETBRAINS_MONO;
    }

    public void setEditorFontFamily(EditorFontFamily editorFontFamily) {
        this.editorFontFamily = editorFontFamily;
    }
}
