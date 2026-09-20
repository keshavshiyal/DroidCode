package com.example.editor;

import androidx.annotation.NonNull;

import java.io.File;

public class EditorTab {

    private final String id;
    private final String filePath;
    private final String fileName;
    private String content;
    private String originalContent;
    private boolean isModified;
    private int cursorPosition;
    private int line;
    private int column;
    private final String languageId;

    public EditorTab(@NonNull String filePath, @NonNull String fileName, String content, String languageId) {
        this.id = filePath;
        this.filePath = filePath;
        this.fileName = fileName;
        this.content = content != null ? content : "";
        this.originalContent = this.content;
        this.isModified = false;
        this.cursorPosition = 0;
        this.line = 1;
        this.column = 1;
        this.languageId = languageId != null ? languageId : detectLanguage(fileName);
    }

    public static String detectLanguage(String name) {
        if (name == null) return "text";
        String lower = name.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "html";
        if (lower.endsWith(".css")) return "css";
        if (lower.endsWith(".js") || lower.endsWith(".jsx")) return "javascript";
        if (lower.endsWith(".json")) return "json";
        if (lower.endsWith(".sql")) return "sql";
        if (lower.endsWith(".py")) return "python";
        if (lower.endsWith(".java")) return "java";
        if (lower.endsWith(".kt") || lower.endsWith(".kts")) return "kotlin";
        if (lower.endsWith(".md")) return "markdown";
        if (lower.endsWith(".xml")) return "xml";
        return "text";
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getFilePath() {
        return filePath;
    }

    @NonNull
    public String getFileName() {
        return fileName;
    }

    public String getContent() {
        return content;
    }

    public void updateContent(String newContent) {
        this.content = newContent != null ? newContent : "";
        this.isModified = !this.content.equals(this.originalContent);
        calculateLineColumn();
    }

    public void markSaved() {
        this.originalContent = this.content;
        this.isModified = false;
    }

    public boolean isModified() {
        return isModified;
    }

    public int getCursorPosition() {
        return cursorPosition;
    }

    public void setCursorPosition(int position) {
        this.cursorPosition = Math.max(0, Math.min(position, content.length()));
        calculateLineColumn();
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getLanguageId() {
        return languageId;
    }

    public File getFile() {
        return new File(filePath);
    }

    private void calculateLineColumn() {
        int pos = Math.min(cursorPosition, content.length());
        int currentLine = 1;
        int lastLineBreak = -1;
        for (int i = 0; i < pos; i++) {
            if (content.charAt(i) == '\n') {
                currentLine++;
                lastLineBreak = i;
            }
        }
        this.line = currentLine;
        this.column = pos - lastLineBreak;
    }
}
