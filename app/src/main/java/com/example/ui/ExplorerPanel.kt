package com.example.ui

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val workspaceMgr = remember { WorkspaceManager.getInstance() }
    val fs = remember { LocalFileSystem.getInstance() }

    var currentProject by remember { mutableStateOf(workspaceMgr.currentProject) }
    var treeNodes by remember { mutableStateOf(workspaceMgr.workspaceFileTree) }

    var selectedNode by remember { mutableStateOf<FileNode?>(null) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val refreshTree = {
        treeNodes = workspaceMgr.workspaceFileTree
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
                .height(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EXPLORER",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row {
                IconButton(
                    onClick = { showNewFileDialog = true },
                    modifier = Modifier
                        .size(28.dp)
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
                    onClick = { showNewFolderDialog = true },
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("explorer_new_folder_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = "New Folder",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { refreshTree() },
                    modifier = Modifier
                        .size(28.dp)
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
                    .padding(4.dp)
            ) {
                items(treeNodes) { node ->
                    FileTreeItem(
                        node = node,
                        depth = 0,
                        isSelected = selectedNode?.path == node.path,
                        onNodeClick = { clicked ->
                            selectedNode = clicked
                            if (clicked.isFolder) {
                                clicked.isExpanded = !clicked.isExpanded
                                if (clicked.isExpanded) {
                                    clicked.children = fs.listDirectory(File(clicked.path))
                                }
                                refreshTree()
                            } else {
                                onOpenFile(File(clicked.path))
                            }
                        },
                        onNodeLongClick = { clicked ->
                            selectedNode = clicked
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
        } else {
            workspaceMgr.currentProject.directory
        }

        InputDialog(
            title = "New File in ${parentDir.name}",
            label = "File Name",
            onDismiss = { showNewFileDialog = false },
            onConfirm = { fileName: String ->
                try {
                    val created = fs.createFile(parentDir, fileName)
                    showNewFileDialog = false
                    refreshTree()
                    onOpenFile(created)
                } catch (e: Exception) {}
            }
        )
    }

    if (showNewFolderDialog && workspaceMgr.hasOpenWorkspace()) {
        val parentDir = if (selectedNode != null && selectedNode!!.isFolder) {
            File(selectedNode!!.path)
        } else {
            workspaceMgr.currentProject.directory
        }

        InputDialog(
            title = "New Folder in ${parentDir.name}",
            label = "Folder Name",
            onDismiss = { showNewFolderDialog = false },
            onConfirm = { folderName: String ->
                try {
                    fs.createDirectory(parentDir, folderName)
                    showNewFolderDialog = false
                    refreshTree()
                } catch (e: Exception) {}
            }
        )
    }
}

@Composable
private fun FileTreeItem(
    node: FileNode,
    depth: Int,
    isSelected: Boolean,
    onNodeClick: (FileNode) -> Unit,
    onNodeLongClick: (FileNode) -> Unit
) {
    val fs = remember { LocalFileSystem.getInstance() }
    val bg = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (depth * 12).dp)
                .background(bg, RoundedCornerShape(4.dp))
                .clickable { onNodeClick(node) }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (node.isFolder) {
                Icon(
                    imageVector = if (node.isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Icon(
                    imageVector = if (node.isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(18.dp)
                )
            } else {
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(18.dp)
                )
            }

            Text(
                text = node.name,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (node.isFolder && node.isExpanded) {
            node.children.forEach { child ->
                FileTreeItem(
                    node = child,
                    depth = depth + 1,
                    isSelected = false,
                    onNodeClick = onNodeClick,
                    onNodeLongClick = onNodeLongClick
                )
            }
        }
    }
}

@Composable
private fun InputDialog(
    title: String,
    label: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var textValue by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text(label) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
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
                    ) { Text("Create") }
                }
            }
        }
    }
}
