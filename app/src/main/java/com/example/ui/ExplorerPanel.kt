package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.filesystem.FileNode
import com.example.filesystem.LocalFileSystem
import com.example.project.WorkspaceManager
import java.io.File

@Composable
fun ExplorerPanel(
    onOpenFile: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val workspaceMgr = remember { WorkspaceManager.getInstance() }
    val fs = remember { LocalFileSystem.getInstance() }

    var treeNodes by remember { mutableStateOf(workspaceMgr.workspaceFileTree) }
    var expandedPaths by remember { mutableStateOf(setOf<String>()) }

    var selectedNode by remember { mutableStateOf<FileNode?>(null) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val refreshTree = {
        treeNodes = workspaceMgr.workspaceFileTree
    }

    LaunchedEffect(workspaceMgr.currentProject) {
        refreshTree()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        // Explorer Header / Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PROJECT EXPLORER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace
                )
                if (workspaceMgr.hasOpenWorkspace()) {
                    Text(
                        text = workspaceMgr.currentProject.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (workspaceMgr.hasOpenWorkspace()) {
                            showNewFileDialog = true
                        } else {
                            Toast.makeText(context, "Open a project first", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("explorer_new_file_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.NoteAdd,
                        contentDescription = "New File",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = {
                        if (workspaceMgr.hasOpenWorkspace()) {
                            showNewFolderDialog = true
                        } else {
                            Toast.makeText(context, "Open a project first", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("explorer_new_folder_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = "New Folder",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (selectedNode != null) {
                    IconButton(
                        onClick = { showRenameDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { refreshTree() },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("explorer_refresh_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Selected Node Bar
        if (selectedNode != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Selected: ${selectedNode!!.name}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1
                )
                TextButton(
                    onClick = { selectedNode = null },
                    modifier = Modifier.height(24.dp)
                ) {
                    Text("Clear", fontSize = 10.sp)
                }
            }
        }

        // Workspace Tree Content
        if (!workspaceMgr.hasOpenWorkspace()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No workspace open",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (treeNodes.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Empty Directory",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                items(treeNodes) { node ->
                    FileTreeItem(
                        node = node,
                        depth = 0,
                        selectedPath = selectedNode?.path,
                        expandedPaths = expandedPaths,
                        onFolderToggle = { folderNode ->
                            expandedPaths = if (expandedPaths.contains(folderNode.path)) {
                                expandedPaths - folderNode.path
                            } else {
                                expandedPaths + folderNode.path
                            }
                        },
                        onNodeClick = { clicked ->
                            selectedNode = clicked
                            if (clicked.isFolder) {
                                expandedPaths = if (expandedPaths.contains(clicked.path)) {
                                    expandedPaths - clicked.path
                                } else {
                                    expandedPaths + clicked.path
                                }
                            } else {
                                onOpenFile(File(clicked.path))
                            }
                        }
                    )
                }
            }
        }
    }

    // Modal Action Dialogs
    if (showNewFileDialog && workspaceMgr.hasOpenWorkspace()) {
        val parentDir = if (selectedNode != null && selectedNode!!.isFolder) {
            File(selectedNode!!.path)
        } else if (selectedNode != null && !selectedNode!!.isFolder) {
            File(selectedNode!!.path).parentFile ?: workspaceMgr.currentProject.directory
        } else {
            workspaceMgr.currentProject.directory
        }

        InputDialog(
            title = "New File in ${parentDir.name}",
            label = "File Name (e.g. main.py)",
            onDismiss = { showNewFileDialog = false },
            onConfirm = { fileName: String ->
                try {
                    val created = fs.createFile(parentDir, fileName)
                    showNewFileDialog = false
                    expandedPaths = expandedPaths + parentDir.absolutePath
                    refreshTree()
                    val newNode = FileNode.fromFile(created)
                    if (newNode != null) {
                        selectedNode = newNode
                    }
                    onOpenFile(created)
                    Toast.makeText(context, "Created $fileName", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    if (showNewFolderDialog && workspaceMgr.hasOpenWorkspace()) {
        val parentDir = if (selectedNode != null && selectedNode!!.isFolder) {
            File(selectedNode!!.path)
        } else if (selectedNode != null && !selectedNode!!.isFolder) {
            File(selectedNode!!.path).parentFile ?: workspaceMgr.currentProject.directory
        } else {
            workspaceMgr.currentProject.directory
        }

        InputDialog(
            title = "New Folder in ${parentDir.name}",
            label = "Folder Name",
            onDismiss = { showNewFolderDialog = false },
            onConfirm = { folderName: String ->
                try {
                    val createdDir = fs.createDirectory(parentDir, folderName)
                    showNewFolderDialog = false
                    expandedPaths = expandedPaths + parentDir.absolutePath + createdDir.absolutePath
                    refreshTree()
                    val newNode = FileNode.fromFile(createdDir)
                    if (newNode != null) {
                        selectedNode = newNode
                    }
                    Toast.makeText(context, "Created folder $folderName", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    if (showRenameDialog && selectedNode != null) {
        val target = File(selectedNode!!.path)
        InputDialog(
            title = "Rename ${selectedNode!!.name}",
            label = "New Name",
            initialValue = selectedNode!!.name,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                try {
                    val renamed = fs.renameFile(target, newName)
                    showRenameDialog = false
                    refreshTree()
                    selectedNode = FileNode.fromFile(renamed)
                    Toast.makeText(context, "Renamed to $newName", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    if (showDeleteDialog && selectedNode != null) {
        val target = File(selectedNode!!.path)
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete ${selectedNode!!.name}?") },
            text = { Text("Are you sure you want to delete '${selectedNode!!.name}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val deletedName = selectedNode!!.name
                            fs.deleteFile(target)
                            selectedNode = null
                            showDeleteDialog = false
                            refreshTree()
                            Toast.makeText(context, "Deleted $deletedName", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun FileTreeItem(
    node: FileNode,
    depth: Int,
    selectedPath: String?,
    expandedPaths: Set<String>,
    onFolderToggle: (FileNode) -> Unit,
    onNodeClick: (FileNode) -> Unit
) {
    val isSelected = selectedPath == node.path
    val isExpanded = expandedPaths.contains(node.path)
    val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (depth * 14).dp, top = 2.dp, bottom = 2.dp)
                .background(bg, RoundedCornerShape(6.dp))
                .clickable { onNodeClick(node) }
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (node.isFolder) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onFolderToggle(node) }
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .size(20.dp)
                )
            } else {
                Spacer(modifier = Modifier.width(18.dp))
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = getFileIconColor(node.extension),
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .size(20.dp)
                )
            }

            Text(
                text = node.name,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (node.isFolder) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }

        if (node.isFolder && isExpanded) {
            node.children.forEach { child ->
                FileTreeItem(
                    node = child,
                    depth = depth + 1,
                    selectedPath = selectedPath,
                    expandedPaths = expandedPaths,
                    onFolderToggle = onFolderToggle,
                    onNodeClick = onNodeClick
                )
            }
        }
    }
}

@Composable
private fun getFileIconColor(ext: String) = when (ext.lowercase()) {
    "kt", "kts" -> MaterialTheme.colorScheme.primary
    "java" -> MaterialTheme.colorScheme.secondary
    "py" -> Color(0xFFFFC107) // Python Yellow
    "js", "ts", "jsx", "tsx" -> Color(0xFF4CAF50) // Green
    "html", "css" -> Color(0xFFE91E63) // Pink
    "json", "xml", "toml", "yaml", "yml" -> MaterialTheme.colorScheme.tertiary
    "md", "txt" -> MaterialTheme.colorScheme.outline
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun InputDialog(
    title: String,
    label: String,
    initialValue: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var textValue by remember { mutableStateOf(initialValue) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text(label) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (textValue.trim().isNotEmpty()) {
                                onConfirm(textValue.trim())
                            }
                        }
                    ) { Text("Save") }
                }
            }
        }
    }
}
