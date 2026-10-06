package com.droidcode.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

enum class FileViewerType {
    TEXT,
    IMAGE,
    VIDEO,
    PDF,
    UNSUPPORTED
}

class EditorTab(
    val filePath: String,
    val fileName: String,
    initialContent: String?,
    languageId: String? = null,
    initialViewerType: FileViewerType = detectViewerType(File(filePath))
) {
    val id: String = filePath

    var viewerType: FileViewerType by mutableStateOf(initialViewerType)

    var content: String by mutableStateOf(initialContent ?: "")
        private set

    var lineStartOffsets: IntArray = computeLineStartOffsets(initialContent ?: "")
        private set

    fun forceOpenAsText(rawText: String) {
        this.content = rawText
        this.originalContent = rawText
        this.lineStartOffsets = computeLineStartOffsets(rawText)
        this.viewerType = FileViewerType.TEXT
        calculateLineColumn()
    }

    var originalContent: String = content
        private set

    var isModified: Boolean by mutableStateOf(false)
        private set

    var cursorPosition: Int by mutableIntStateOf(0)

    var line: Int by mutableIntStateOf(1)
        private set

    var column: Int by mutableIntStateOf(1)
        private set

    var languageId: String by mutableStateOf(languageId ?: detectLanguage(fileName))

    var encoding: String by mutableStateOf("UTF-8")

    var lineEnding: String by mutableStateOf(if (initialContent?.contains("\r\n") == true) "CRLF" else "LF")

    var selectionStart: Int by mutableIntStateOf(0)
    var selectionEnd: Int by mutableIntStateOf(0)

    var scrollX: Int = 0
    var scrollY: Int = 0

    var findQuery: String by mutableStateOf("")
    var replaceQuery: String by mutableStateOf("")
    var showFindBar: Boolean by mutableStateOf(false)
    var showReplaceBar: Boolean by mutableStateOf(false)
    var currentMatchIndex: Int by mutableIntStateOf(0)

    val foldedLines = mutableStateListOf<Int>()

    fun updateSelection(start: Int, end: Int) {
        this.selectionStart = start
        this.selectionEnd = end
        this.cursorPosition = this.selectionEnd
        if (start != end && content.isNotEmpty() && lineStartOffsets.isNotEmpty()) {
            calculateLineColumn()
        }
    }

    fun updateContent(newContent: String) {
        this.content = newContent
        this.lineStartOffsets = computeLineStartOffsets(newContent)
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

    fun updateCursor(targetLine: Int, targetColumn: Int) {
        this.line = targetLine
        this.column = targetColumn
        val lineIdx = (targetLine - 1).coerceIn(0, (lineStartOffsets.size - 1).coerceAtLeast(0))
        val lineStart = lineStartOffsets.getOrElse(lineIdx) { 0 }
        this.cursorPosition = (lineStart + (targetColumn - 1).coerceAtLeast(0)).coerceIn(0, content.length.coerceAtLeast(0))
    }

    val file: File
        get() = File(filePath)

    val extension: String
        get() = file.extension

    private fun calculateLineColumn() {
        val pos = cursorPosition.coerceIn(0, content.length)
        val offsets = lineStartOffsets
        var low = 0
        var high = offsets.size - 1
        var lineIdx = 0
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (offsets[mid] <= pos) {
                lineIdx = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        this.line = lineIdx + 1
        this.column = pos - offsets[lineIdx] + 1
    }

    private fun computeLineStartOffsets(text: String): IntArray {
        val starts = mutableListOf<Int>()
        starts.add(0)
        for (i in 0 until text.length) {
            if (text[i] == '\n') {
                starts.add(i + 1)
            }
        }
        return starts.toIntArray()
    }

    companion object {
        fun detectViewerType(file: File): FileViewerType {
            if (!file.exists()) return FileViewerType.UNSUPPORTED
            val ext = file.extension.lowercase()

            if (ext in listOf("png", "jpg", "jpeg", "gif", "webp", "svg", "bmp", "ico", "avif", "heic", "heif", "tif", "tiff")) {
                return FileViewerType.IMAGE
            }
            if (ext in listOf("mp4", "mkv", "avi", "webm", "mov", "3gp")) {
                return FileViewerType.VIDEO
            }
            if (ext == "pdf") {
                return FileViewerType.PDF
            }
            if (ext in listOf(
                    "zip", "apk", "exe", "bin", "db", "so", "aab", "class", "pyc", "dex", "tar", "gz",
                    "7z", "rar", "aar", "jar", "iso", "dmg", "sqlite", "sqlite3", "doc", "docx", "xls",
                    "xlsx", "ppt", "pptx", "ttf", "otf", "woff", "woff2", "eot", "mp3", "wav", "aac",
                    "flac", "ogg", "m4a"
                )
            ) {
                return FileViewerType.UNSUPPORTED
            }

            return try {
                val bytes = file.inputStream().use { stream ->
                    val buf = ByteArray(1024)
                    val read = stream.read(buf, 0, 1024)
                    if (read <= 0) ByteArray(0) else buf.copyOf(read)
                }
                if (bytes.contains(0.toByte())) {
                    FileViewerType.UNSUPPORTED
                } else {
                    FileViewerType.TEXT
                }
            } catch (e: Exception) {
                FileViewerType.UNSUPPORTED
            }
        }

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
