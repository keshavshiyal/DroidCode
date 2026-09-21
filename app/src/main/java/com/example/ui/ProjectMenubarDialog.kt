package com.example.ui

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
import com.example.core.Command
import com.example.core.CommandRegistry

@Composable
fun ProjectMenubarDialog(
    onDismiss: () -> Unit,
    onExecuteCommand: (Command) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }

    val registry = remember { CommandRegistry.getInstance() }

    val projectCategories = listOf(
        "File",
        "Workspace",
        "View",
        "Build & Run",
        "Git VCS",
        "Preferences"
    )

    val commandsByCategory = remember(searchQuery, selectedCategoryFilter) {
        val results = mutableMapOf<String, List<Command>>()
        val query = searchQuery.trim().lowercase()

        val allCmds = registry.allCommands
        for (cat in projectCategories) {
            val filtered = allCmds.filter { cmd ->
                val matchesCategory = cmd.category.contains(cat, ignoreCase = true) ||
                        (cat == "File" && cmd.category.equals("File", ignoreCase = true)) ||
                        (cat == "Preferences" && (cmd.category.equals("Preferences", ignoreCase = true) || cmd.category.equals("Settings", ignoreCase = true)))

                val filterPass = selectedCategoryFilter == "ALL" ||
                        selectedCategoryFilter.equals(cat, ignoreCase = true) ||
                        cmd.category.equals(selectedCategoryFilter, ignoreCase = true)

                val matchesQuery = query.isEmpty() ||
                        cmd.title.lowercase().contains(query) ||
                        cmd.id.lowercase().contains(query) ||
                        (cmd.shortcut != null && cmd.shortcut.lowercase().contains(query))

                matchesCategory && filterPass && matchesQuery
            }
            if (filtered.isNotEmpty()) {
                results[cat] = filtered
            }
        }
        
        // Also capture any other uncategorized project commands if filter is ALL
        if (selectedCategoryFilter == "ALL") {
            val categorizedIds = results.values.flatten().map { it.id }.toSet()
            val remaining = allCmds.filter { cmd ->
                !categorizedIds.contains(cmd.id) &&
                        (query.isEmpty() || cmd.title.lowercase().contains(query) || cmd.id.lowercase().contains(query))
            }
            if (remaining.isNotEmpty()) {
                results["General & Actions"] = remaining
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
                            imageVector = Icons.Default.Menu,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Project Menubar",
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
                    placeholder = { Text("Search project commands...", fontSize = 13.sp) },
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
                        .testTag("project_menubar_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Quick Filter Chips with horizontal scrolling
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("ALL", "File", "Workspace", "View", "Build & Run", "Git VCS", "Preferences").forEach { filterLabel ->
                        val isSelected = (selectedCategoryFilter.equals(filterLabel, ignoreCase = true))

                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategoryFilter = filterLabel },
                            label = { Text(filterLabel, fontSize = 11.sp) },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

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
                            text = "No matching project commands found.",
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
                                ProjectMenubarItem(
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
                                Divider(
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
private fun ProjectMenubarItem(
    command: Command,
    onClick: () -> Unit
) {
    val isEnabled = command.isEnabled
    val textColor = if (isEnabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    }

    val icon = when {
        command.title.contains("Save", true) -> Icons.Default.Save
        command.title.contains("Workspace", true) -> Icons.Default.Workspaces
        command.title.contains("Folder", true) || command.title.contains("Explorer", true) -> Icons.Default.Folder
        command.title.contains("Terminal", true) -> Icons.Default.Terminal
        command.title.contains("Database", true) -> Icons.Default.Storage
        command.title.contains("Palette", true) -> Icons.Default.Search
        command.title.contains("Run", true) -> Icons.Default.PlayArrow
        command.title.contains("Build", true) -> Icons.Default.Build
        command.title.contains("Git", true) -> Icons.Default.AccountTree
        command.title.contains("Settings", true) || command.title.contains("Preferences", true) -> Icons.Default.Settings
        command.title.contains("About", true) -> Icons.Default.Info
        else -> Icons.Default.Widgets
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
