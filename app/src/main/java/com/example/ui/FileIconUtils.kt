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
    fun getFileIcon(ext: String): ImageVector = when (ext.lowercase()) {
        "java" -> Icons.Default.Coffee
        "kt", "kts" -> Icons.Default.Code
        "py", "pyw" -> Icons.Default.Terminal
        "js", "jsx", "ts", "tsx", "mjs", "cjs" -> Icons.Default.DataObject
        "css", "scss", "less" -> Icons.Default.Palette
        "html", "htm" -> Icons.Default.Language
        "bat", "cmd", "sh", "bash", "zsh", "ps1" -> Icons.Default.Terminal
        "md", "markdown" -> Icons.AutoMirrored.Filled.Article
        "json", "xml", "yaml", "yml", "toml", "properties", "gradle", "ini", "conf" -> Icons.Default.Settings
        "sql", "db", "sqlite", "sqlite3" -> Icons.Default.Storage
        "c", "cpp", "h", "hpp", "cc", "cs", "php" -> Icons.Default.Code
        "rb" -> Icons.Default.Diamond
        "go" -> Icons.Default.Speed
        "rs" -> Icons.Default.Build
        "png", "jpg", "jpeg", "gif", "svg", "webp", "ico" -> Icons.Default.Image
        "zip", "tar", "gz", "7z", "rar" -> Icons.Default.FolderZip
        "txt", "log" -> Icons.Default.Description
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }

    @Composable
    fun getFileIconColor(ext: String): Color = when (ext.lowercase()) {
        "java" -> Color(0xFFD84315) // Java Coffee Amber
        "kt", "kts" -> MaterialTheme.colorScheme.primary // Kotlin Primary Violet
        "py", "pyw" -> Color(0xFFFFB300) // Python Yellow
        "js", "jsx", "ts", "tsx", "mjs", "cjs" -> Color(0xFFE5A000) // JS Amber/Yellow
        "css", "scss", "less" -> Color(0xFF29B6F6) // CSS Light Blue
        "html", "htm" -> Color(0xFFFF5722) // HTML Deep Orange
        "bat", "cmd", "sh", "bash", "zsh", "ps1" -> Color(0xFF2E7D32) // Terminal Green
        "md", "markdown" -> Color(0xFF00ACC1) // Markdown Cyan
        "json", "xml", "yaml", "yml", "toml", "properties", "gradle", "ini", "conf" -> MaterialTheme.colorScheme.tertiary
        "sql", "db", "sqlite", "sqlite3" -> Color(0xFF0288D1) // SQL Blue
        "c", "cpp", "h", "hpp", "cc" -> Color(0xFF5C6BC0) // C/C++ Indigo
        "cs" -> Color(0xFF43A047) // C# Green
        "php" -> Color(0xFF7E57C2) // PHP Purple
        "rb" -> Color(0xFFE53935) // Ruby Red
        "go" -> Color(0xFF00ACC1) // Go Cyan
        "rs" -> Color(0xFFD81B60) // Rust Rose
        "png", "jpg", "jpeg", "gif", "svg", "webp", "ico" -> Color(0xFFAB47BC) // Image Pink/Purple
        "zip", "tar", "gz", "7z", "rar" -> Color(0xFFFFA000) // Zip Amber
        "txt", "log" -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}
