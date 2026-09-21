package com.example.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

class EditorTab(
    val filePath: String,
    val fileName: String,
    initialContent: String?,
    languageId: String? = null
) {
    val id: String = filePath

    var content: String by mutableStateOf(initialContent ?: "")
        private set

    var originalContent: String = content
        private set

    var isModified: Boolean by mutableStateOf(false)
        private set

    var cursorPosition: Int by mutableIntStateOf(0)

    var line: Int by mutableIntStateOf(1)
        private set

    var column: Int by mutableIntStateOf(1)
        private set

    val languageId: String = languageId ?: detectLanguage(fileName)

    fun updateContent(newContent: String) {
        this.content = newContent
        this.isModified = (this.content != this.originalContent)
        calculateLineColumn()
    }

    fun markSaved() {
        this.originalContent = this.content
        this.isModified = false
    }

    fun updateCursor(position: Int) {
        this.cursorPosition = position.coerceIn(0, content.length)
        calculateLineColumn()
    }

    val file: File
        get() = File(filePath)

    val extension: String
        get() = file.extension

    private fun calculateLineColumn() {
        val pos = cursorPosition.coerceIn(0, content.length)
        var currentLine = 1
        var lastLineBreak = -1
        for (i in 0 until pos) {
            if (content[i] == '\n') {
                currentLine++
                lastLineBreak = i
            }
        }
        this.line = currentLine
        this.column = pos - lastLineBreak
    }

    companion object {
        fun detectLanguage(name: String?): String {
            if (name == null) return "text"
            val lower = name.lowercase()
            return when {
                lower.endsWith(".html") || lower.endsWith(".htm") -> "html"
                lower.endsWith(".css") -> "css"
                lower.endsWith(".js") || lower.endsWith(".jsx") -> "javascript"
                lower.endsWith(".ts") || lower.endsWith(".tsx") -> "typescript"
                lower.endsWith(".json") -> "json"
                lower.endsWith(".sql") -> "sql"
                lower.endsWith(".py") -> "python"
                lower.endsWith(".java") -> "java"
                lower.endsWith(".kt") || lower.endsWith(".kts") -> "kotlin"
                lower.endsWith(".md") -> "markdown"
                lower.endsWith(".xml") -> "xml"
                lower.endsWith(".sh") || lower.endsWith(".bash") -> "shell"
                lower.endsWith(".properties") || lower.endsWith(".gradle") || lower.endsWith(".toml") -> "config"
                else -> "text"
            }
        }
    }
}
