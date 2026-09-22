package com.droidcode.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object FileIconUtils {

    /**
     * Pure non-composable classification method for unit testing and logic mapping.
     */
    fun classifyFile(nameOrExt: String): String {
        val clean = nameOrExt.trim()
        val lower = clean.lowercase()

        // 1. Exact filenames or special prefixes
        when {
            lower == "dockerfile" || lower.startsWith("dockerfile.") -> return "DOCKER"
            lower == "makefile" || lower == "cmakelists.txt" -> return "BUILD"
            lower == "pom.xml" -> return "MAVEN"
            lower == ".env" || lower.startsWith(".env.") -> return "ENV"
            lower == "license" || lower == "license.txt" || lower == "license.md" -> return "LICENSE"
            lower == ".editorconfig" -> return "CONFIG"
            lower == "readme" || lower == "readme.md" || lower == "readme.txt" -> return "README"
            lower == ".gitignore" || lower == ".gitattributes" || lower == ".gitmodules" -> return "GIT"
        }

        // 2. Extract extension
        val ext = if (clean.contains(".")) clean.substringAfterLast(".", "").lowercase() else lower

        return when (ext) {
            "html", "htm" -> "HTML"
            "css", "scss", "sass", "less" -> "CSS"
            "js", "jsx", "mjs", "cjs" -> "JAVASCRIPT"
            "ts", "tsx" -> "TYPESCRIPT"
            "json" -> "JSON"
            "md", "markdown" -> "MARKDOWN"
            "sql", "db", "sqlite", "sqlite3" -> "SQL"
            "py", "pyw" -> "PYTHON"
            "java" -> "JAVA"
            "kt", "kts" -> "KOTLIN"
            "c" -> "C"
            "cpp", "cc", "cxx", "h", "hpp" -> "CPP"
            "cs" -> "CSHARP"
            "go" -> "GO"
            "rs" -> "RUST"
            "rb" -> "RUBY"
            "php" -> "PHP"
            "swift" -> "SWIFT"
            "xml" -> "XML"
            "yaml", "yml" -> "YAML"
            "toml" -> "TOML"
            "properties", "ini", "cfg", "conf" -> "CONFIG"
            "csv", "tsv" -> "DATA"
            "gradle" -> "GRADLE"
            "png", "jpg", "jpeg", "gif", "svg", "webp", "bmp", "ico", "avif", "heic", "heif", "tif", "tiff" -> "IMAGE"
            "pdf" -> "PDF"
            "mp4", "mkv", "avi", "webm", "mov", "3gp" -> "VIDEO"
            "txt", "log", "rst" -> "DOCUMENTATION"
            "zip", "tar", "gz", "7z", "rar", "jar", "aar" -> "ARCHIVE"
            "sh", "bash", "zsh", "bat", "cmd", "ps1" -> "SHELL"
            else -> "FILE"
        }
    }

    @Composable
    fun getFileIcon(nameOrExt: String): ImageVector {
        return when (classifyFile(nameOrExt)) {
            "DOCKER" -> Icons.Default.Layers
            "BUILD" -> Icons.Default.Build
            "MAVEN" -> Icons.Default.Inventory2
            "ENV" -> Icons.Default.Key
            "LICENSE" -> Icons.Default.VerifiedUser
            "CONFIG" -> Icons.Default.Tune
            "README" -> Icons.Default.Info
            "GIT" -> Icons.Default.AccountTree
            "HTML" -> Icons.Default.Language
            "CSS" -> Icons.Default.Palette
            "JAVASCRIPT" -> Icons.Default.Javascript
            "TYPESCRIPT" -> Icons.Default.IntegrationInstructions
            "JSON" -> Icons.Default.DataObject
            "MARKDOWN" -> Icons.AutoMirrored.Filled.Article
            "SQL" -> Icons.Default.Storage
            "PYTHON" -> Icons.Default.Terminal
            "JAVA" -> Icons.Default.Coffee
            "KOTLIN" -> Icons.Default.Code
            "C", "CPP", "CSHARP", "PHP", "SWIFT", "RUBY" -> Icons.Default.Code
            "GO" -> Icons.Default.Speed
            "RUST" -> Icons.Default.Build
            "XML" -> Icons.Default.Code
            "YAML" -> Icons.Default.FormatListBulleted
            "TOML" -> Icons.Default.Tune
            "DATA" -> Icons.Default.TableChart
            "GRADLE" -> Icons.Default.Handyman
            "IMAGE" -> Icons.Default.Image
            "PDF" -> Icons.Default.PictureAsPdf
            "VIDEO" -> Icons.Default.VideoFile
            "DOCUMENTATION" -> Icons.Default.Description
            "ARCHIVE" -> Icons.Default.FolderZip
            "SHELL" -> Icons.Default.Terminal
            else -> Icons.AutoMirrored.Filled.InsertDriveFile
        }
    }

    @Composable
    fun getFileIconColor(nameOrExt: String): Color {
        return when (classifyFile(nameOrExt)) {
            "DOCKER" -> Color(0xFF0288D1)
            "BUILD" -> Color(0xFF607D8B)
            "MAVEN" -> Color(0xFFC71A36)
            "ENV" -> Color(0xFFFFD54F)
            "LICENSE" -> Color(0xFF66BB6A)
            "CONFIG" -> Color(0xFF78909C)
            "README" -> Color(0xFF00ACC1)
            "GIT" -> Color(0xFFF4511E)
            "HTML" -> Color(0xFFE65100)
            "CSS" -> Color(0xFF0288D1)
            "JAVASCRIPT" -> Color(0xFFF7DF1E)
            "TYPESCRIPT" -> Color(0xFF3178C6)
            "JSON" -> Color(0xFFE53935)
            "MARKDOWN" -> Color(0xFF00ACC1)
            "SQL" -> Color(0xFF336791)
            "PYTHON" -> Color(0xFFFFC107)
            "JAVA" -> Color(0xFFE65100)
            "KOTLIN" -> MaterialTheme.colorScheme.primary
            "C" -> Color(0xFF5C6BC0)
            "CPP" -> Color(0xFF00599C)
            "CSHARP" -> Color(0xFF239120)
            "GO" -> Color(0xFF00ADD8)
            "RUST" -> Color(0xFFDEA584)
            "RUBY" -> Color(0xFFCC342D)
            "PHP" -> Color(0xFF777BB4)
            "SWIFT" -> Color(0xFFFA7343)
            "XML" -> Color(0xFF8E24AA)
            "YAML" -> Color(0xFF388E3C)
            "TOML" -> Color(0xFF9E9D24)
            "DATA" -> Color(0xFF43A047)
            "GRADLE" -> Color(0xFF02303A)
            "IMAGE" -> Color(0xFFAB47BC)
            "PDF" -> Color(0xFFE53935)
            "VIDEO" -> Color(0xFF5E35B1)
            "DOCUMENTATION" -> MaterialTheme.colorScheme.outline
            "ARCHIVE" -> Color(0xFFFFA000)
            "SHELL" -> Color(0xFF2E7D32)
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    }
}
