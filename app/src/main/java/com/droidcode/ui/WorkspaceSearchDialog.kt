package com.droidcode.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SearchResultMatch(
    val file: File,
    val relativePath: String,
    val lineNumber: Int,
    val lineContent: String,
    val matchStartInLine: Int,
    val matchLength: Int
)

private val IGNORED_DIRECTORIES = setOf(
    ".git", ".gradle", ".idea", ".cxx", "build", "node_modules",
    "dist", "out", "target", "__pycache__", ".vscode"
)

private val BINARY_EXTENSIONS = setOf(
    "png", "jpg", "jpeg", "gif", "webp", "bmp", "ico",
    "mp4", "mkv", "avi", "mov", "mp3", "wav", "flac",
    "zip", "tar", "gz", "rar", "7z", "apk", "aab", "jar", "class",
    "so", "dll", "dylib", "exe", "pdf"
)

@Composable
fun WorkspaceSearchDialog(
    rootDirectory: File?,
    initialQuery: String = "",
    onDismiss: () -> Unit,
    onResultClick: (File, Int) -> Unit
) {
    var query by remember { mutableStateOf(initialQuery) }
    var matchCase by remember { mutableStateOf(false) }
    var wholeWord by remember { mutableStateOf(false) }
    var useRegex by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }

    val results = remember { mutableStateListOf<SearchResultMatch>() }

    LaunchedEffect(query, matchCase, wholeWord, useRegex, rootDirectory) {
        results.clear()
        if (query.trim().isEmpty() || rootDirectory == null || !rootDirectory.exists()) {
            isSearching = false
            return@LaunchedEffect
        }

        isSearching = true
        withContext(Dispatchers.IO) {
            val q = query.trim()
            val regex = try {
                when {
                    useRegex -> Regex(q, if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE))
                    wholeWord -> {
                        val escaped = Regex.escape(q)
                        Regex("\\b$escaped\\b", if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE))
                    }
                    else -> null
                }
            } catch (e: Exception) {
                null
            }

            val found = mutableListOf<SearchResultMatch>()
            fun scanDir(dir: File) {
                if (found.size >= 500) return
                val children = dir.listFiles() ?: return
                for (child in children) {
                    if (found.size >= 500) break
                    if (child.isDirectory) {
                        if (!IGNORED_DIRECTORIES.contains(child.name)) {
                            scanDir(child)
                        }
                    } else if (child.isFile) {
                        val ext = child.extension.lowercase()
                        if (BINARY_EXTENSIONS.contains(ext) || child.length() > 2_000_000L) {
                            continue
                        }

                        try {
                            val rel = child.relativeToOrNull(rootDirectory)?.path ?: child.name
                            var lineNum = 1
                            child.useLines { lines ->
                                for (line in lines) {
                                    if (found.size >= 500) break
                                    if (regex != null) {
                                        val match = regex.find(line)
                                        if (match != null) {
                                            found.add(
                                                SearchResultMatch(
                                                    file = child,
                                                    relativePath = rel,
                                                    lineNumber = lineNum,
                                                    lineContent = line.trimEnd(),
                                                    matchStartInLine = match.range.first,
                                                    matchLength = match.value.length
                                                )
                                            )
                                        }
                                    } else {
                                        val idx = line.indexOf(q, ignoreCase = !matchCase)
                                        if (idx >= 0) {
                                            found.add(
                                                SearchResultMatch(
                                                    file = child,
                                                    relativePath = rel,
                                                    lineNumber = lineNum,
                                                    lineContent = line.trimEnd(),
                                                    matchStartInLine = idx,
                                                    matchLength = q.length
                                                )
                                            )
                                        }
                                    }
                                    lineNum++
                                }
                            }
                        } catch (_: Exception) {
                            // Skip unreadable files safely
                        }
                    }
                }
            }

            scanDir(rootDirectory)
            withContext(Dispatchers.Main) {
                results.clear()
                results.addAll(found)
                isSearching = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(16.dp))
                .testTag("workspace_search_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Find in Files (Project Grep)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Input Field
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search text across workspace...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("workspace_search_query_input"),
                    singleLine = true,
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Options Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = matchCase,
                        onClick = { matchCase = !matchCase },
                        label = { Text("Match Case (Aa)", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors()
                    )
                    FilterChip(
                        selected = wholeWord,
                        onClick = { wholeWord = !wholeWord },
                        label = { Text("Whole Word (\\b)", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors()
                    )
                    FilterChip(
                        selected = useRegex,
                        onClick = { useRegex = !useRegex },
                        label = { Text("Regex (.*)", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isSearching) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Summary
                Text(
                    text = when {
                        query.isBlank() -> "Type a query to search workspace files."
                        isSearching -> "Searching workspace..."
                        results.isEmpty() -> "No matching occurrences found."
                        else -> {
                            val fileCount = results.map { it.file }.distinct().size
                            "Found ${results.size} matches in $fileCount files:"
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Results List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(results) { match ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onResultClick(match.file, match.lineNumber) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = match.relativePath,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = ":${match.lineNumber}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                val matchColor = MaterialTheme.colorScheme.primary
                                val snippet = remember(match.lineContent, match.matchStartInLine, match.matchLength) {
                                    buildAnnotatedString {
                                        val content = match.lineContent
                                        val s = match.matchStartInLine.coerceIn(0, content.length)
                                        val e = (s + match.matchLength).coerceIn(s, content.length)
                                        if (s > 0) append(content.substring(0, s))
                                        if (e > s) {
                                            pushStyle(
                                                SpanStyle(
                                                    background = matchColor.copy(alpha = 0.25f),
                                                    fontWeight = FontWeight.Bold,
                                                    color = matchColor
                                                )
                                            )
                                            append(content.substring(s, e))
                                            pop()
                                        }
                                        if (e < content.length) append(content.substring(e))
                                    }
                                }

                                Text(
                                    text = snippet,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
