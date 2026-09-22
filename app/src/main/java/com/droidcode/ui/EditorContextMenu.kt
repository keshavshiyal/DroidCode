package com.droidcode.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.droidcode.core.Command
import com.droidcode.core.CommandRegistry

@Composable
fun EditorContextMenuDialog(
    onDismiss: () -> Unit,
    onExecuteCommand: (Command) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }

    val registry = remember { CommandRegistry.getInstance() }

    // Categories in explicit IDE order requested
    val ideCategories = listOf(
        "Editing",
        "Navigation",
        "Code",
        "Editor",
        "File",
        "Developer Tools"
    )

    // Gather all commands from registry grouped by category
    val commandsByCategory = remember(searchQuery, selectedCategoryFilter) {
        val results = mutableMapOf<String, List<Command>>()
        val query = searchQuery.trim().lowercase()

        for (cat in ideCategories) {
            val cmds = registry.getCommandsByCategory(cat)
            val filtered = cmds.filter { cmd ->
                val matchesQuery = query.isEmpty() ||
                        cmd.title.lowercase().contains(query) ||
                        cmd.id.lowercase().contains(query) ||
                        (cmd.shortcut != null && cmd.shortcut.lowercase().contains(query))
                val matchesCategory = selectedCategoryFilter == "ALL" || selectedCategoryFilter.equals(cat, ignoreCase = true)
                matchesQuery && matchesCategory
            }
            if (filtered.isNotEmpty()) {
                results[cat] = filtered
            }
        }
        results
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Editor Context Menu",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Menu",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Filter Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search editor actions...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_context_menu_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Quick Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("ALL", "Editing", "Navigation", "Code", "Editor", "File", "Dev").forEach { filterLabel ->
                        val targetCat = if (filterLabel == "Dev") "Developer Tools" else filterLabel
                        val isSelected = (selectedCategoryFilter.equals(targetCat, ignoreCase = true))

                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategoryFilter = targetCat },
                            label = { Text(filterLabel, fontSize = 11.sp) },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Spacer(modifier = Modifier.height(8.dp))

                // Action Items Grouped by Category
                if (commandsByCategory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No matching editor actions found.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        commandsByCategory.forEach { (categoryName, commandList) ->
                            item {
                                Text(
                                    text = categoryName.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(top = 6.dp, bottom = 4.dp, start = 4.dp)
                                )
                            }

                            items(commandList) { command ->
                                EditorContextMenuItem(
                                    command = command,
                                    onClick = {
                                        if (command.isEnabled) {
                                            onExecuteCommand(command)
                                            onDismiss()
                                        }
                                    }
                                )
                            }

                            item {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorContextMenuItem(
    command: Command,
    onClick: () -> Unit
) {
    val isEnabled = command.isEnabled
    val textColor = if (isEnabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    }

    val icon = when (command.category) {
        "Editing" -> when {
            command.title.contains("Cut", true) -> Icons.Default.ContentCut
            command.title.contains("Copy", true) -> Icons.Default.ContentCopy
            command.title.contains("Paste", true) -> Icons.Default.ContentPaste
            command.title.contains("Undo", true) -> Icons.AutoMirrored.Filled.Undo
            command.title.contains("Redo", true) -> Icons.AutoMirrored.Filled.Redo
            command.title.contains("Select", true) -> Icons.Default.SelectAll
            else -> Icons.Default.Edit
        }
        "Navigation" -> when {
            command.title.contains("Find", true) -> Icons.Default.Search
            command.title.contains("Line", true) -> Icons.Default.FormatListNumbered
            else -> Icons.Default.Navigation
        }
        "Code" -> when {
            command.title.contains("Format", true) -> Icons.Default.AutoFixHigh
            command.title.contains("Comment", true) -> Icons.AutoMirrored.Filled.Comment
            command.title.contains("Fold", true) -> Icons.Default.UnfoldMore
            else -> Icons.Default.Code
        }
        "Editor" -> when {
            command.title.contains("Wrap", true) -> Icons.AutoMirrored.Filled.WrapText
            command.title.contains("Language", true) -> Icons.Default.Language
            command.title.contains("Palette", true) -> Icons.Default.Palette
            else -> Icons.Default.Settings
        }
        "File" -> when {
            command.title.contains("Save", true) -> Icons.Default.Save
            command.title.contains("Close", true) -> Icons.Default.Close
            command.title.contains("Revert", true) -> Icons.Default.Refresh
            else -> Icons.AutoMirrored.Filled.InsertDriveFile
        }
        "Developer Tools" -> when {
            command.title.contains("Run", true) -> Icons.Default.PlayArrow
            command.title.contains("Debug", true) -> Icons.Default.BugReport
            command.title.contains("Terminal", true) -> Icons.Default.Terminal
            command.title.contains("Git", true) -> Icons.Default.AccountTree
            else -> Icons.Default.Build
        }
        else -> Icons.Default.TouchApp
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isEnabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else Color.Transparent,
                RoundedCornerShape(6.dp)
            )
            .clickable(enabled = isEnabled) { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = command.title,
                fontSize = 13.sp,
                fontWeight = if (isEnabled) FontWeight.Medium else FontWeight.Normal,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (!isEnabled) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(3.dp)
                ) {
                    Text(
                        text = "Disabled",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }

        if (command.shortcut != null && command.shortcut.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .background(
                        if (isEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        RoundedCornerShape(4.dp)
                    )
                    .border(
                        1.dp,
                        if (isEnabled) MaterialTheme.colorScheme.outline.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = command.shortcut,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    maxLines = 1
                )
            }
        }
    }
}
