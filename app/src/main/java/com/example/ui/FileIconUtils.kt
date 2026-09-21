package com.example.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object FileIconUtils {

    @Composable
    fun getFileIcon(nameOrExt: String): ImageVector {
        val clean = nameOrExt.trim()
        val lower = clean.lowercase()

        // 1. Check special exact filenames or prefixes
        when {
            lower == "dockerfile" || lower.startsWith("dockerfile.") -> return Icons.Default.Layers
            lower == "makefile" || lower == "cmakelists.txt" -> return Icons.Default.Build
            lower == ".env" || lower.startsWith(".env.") -> return Icons.Default.Key
            lower == "readme" || lower == "readme.md" || lower == "readme.txt" -> return Icons.Default.Info
            lower == ".gitignore" || lower == ".gitattributes" || lower == ".gitmodules" -> return Icons.Default.AccountTree
        }

        // 2. Extract extension if input is a filename, or treat as extension
        val ext = if (clean.contains(".")) clean.substringAfterLast(".", "").lowercase() else lower

        return when (ext) {
            "html", "htm" -> Icons.Default.Language
            "css", "scss", "less" -> Icons.Default.Palette
            "js", "jsx", "mjs", "cjs" -> Icons.Default.Javascript
            "ts", "tsx" -> Icons.Default.IntegrationInstructions
            "json" -> Icons.Default.DataObject
            "md", "markdown" -> Icons.AutoMirrored.Filled.Article
            "php" -> Icons.Default.Code
            "py", "pyw" -> Icons.Default.Terminal
            "java" -> Icons.Default.Coffee
            "kt", "kts" -> Icons.Default.Code
            "c" -> Icons.Default.Code
            "cpp", "cc", "h", "hpp", "cxx" -> Icons.Default.Code
            "rs" -> Icons.Default.Build
            "go" -> Icons.Default.Speed
            "sql", "db", "sqlite", "sqlite3" -> Icons.Default.Storage
            "xml" -> Icons.Default.Code
            "yaml", "yml" -> Icons.Default.FormatListBulleted
            "sh", "bash", "zsh", "bat", "cmd", "ps1" -> Icons.Default.Terminal
            "txt", "log" -> Icons.Default.Description
            "gitignore", "gitattributes", "gitmodules" -> Icons.Default.AccountTree
            "png", "jpg", "jpeg", "gif", "svg", "webp", "ico", "bmp" -> Icons.Default.Image
            "zip", "tar", "gz", "7z", "rar" -> Icons.Default.FolderZip
            else -> Icons.AutoMirrored.Filled.InsertDriveFile
        }
    }

    @Composable
    fun getFileIconColor(nameOrExt: String): Color {
        val clean = nameOrExt.trim()
        val lower = clean.lowercase()

        // 1. Check special exact filenames or prefixes
        when {
            lower == "dockerfile" || lower.startsWith("dockerfile.") -> return Color(0xFF0288D1) // Docker Blue
            lower == "makefile" || lower == "cmakelists.txt" -> return Color(0xFF607D8B) // Slate Grey
            lower == ".env" || lower.startsWith(".env.") -> return Color(0xFFFFD54F) // Key Yellow
            lower == "readme" || lower == "readme.md" || lower == "readme.txt" -> return Color(0xFF00ACC1) // Readme Cyan
            lower == ".gitignore" || lower == ".gitattributes" || lower == ".gitmodules" -> return Color(0xFFF4511E) // Git Orange
        }

        // 2. Extract extension if input is a filename, or treat as extension
        val ext = if (clean.contains(".")) clean.substringAfterLast(".", "").lowercase() else lower

        return when (ext) {
            "html", "htm" -> Color(0xFFE65100) // HTML Deep Orange
            "css", "scss", "less" -> Color(0xFF0288D1) // CSS Light Blue
            "js", "jsx", "mjs", "cjs" -> Color(0xFFF7DF1E) // JS Yellow
            "ts", "tsx" -> Color(0xFF3178C6) // TS Blue
            "json" -> Color(0xFFE53935) // JSON Red/Coral
            "md", "markdown" -> Color(0xFF00ACC1) // Markdown Cyan
            "php" -> Color(0xFF777BB4) // PHP Purple
            "py", "pyw" -> Color(0xFFFFC107) // Python Yellow
            "java" -> Color(0xFFE65100) // Java Coffee Amber
            "kt", "kts" -> MaterialTheme.colorScheme.primary // Kotlin Primary Violet
            "c" -> Color(0xFF5C6BC0) // C Indigo
            "cpp", "cc", "h", "hpp", "cxx" -> Color(0xFF00599C) // C++ Blue
            "rs" -> Color(0xFFDEA584) // Rust Copper
            "go" -> Color(0xFF00ADD8) // Go Cyan
            "sql", "db", "sqlite", "sqlite3" -> Color(0xFF336791) // SQL Blue
            "xml" -> Color(0xFF8E24AA) // XML Purple
            "yaml", "yml" -> Color(0xFF388E3C) // YAML Green
            "sh", "bash", "zsh", "bat", "cmd", "ps1" -> Color(0xFF2E7D32) // Terminal Green
            "txt", "log" -> MaterialTheme.colorScheme.outline
            "gitignore", "gitattributes", "gitmodules" -> Color(0xFFF4511E) // Git Orange
            "png", "jpg", "jpeg", "gif", "svg", "webp", "ico", "bmp" -> Color(0xFFAB47BC) // Image Pink/Purple
            "zip", "tar", "gz", "7z", "rar" -> Color(0xFFFFA000) // Zip Amber
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    }
}

