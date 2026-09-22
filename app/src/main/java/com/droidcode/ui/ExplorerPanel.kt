package com.droidcode.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.droidcode.filesystem.FileNode
import com.droidcode.filesystem.LocalFileSystem
import com.droidcode.filesystem.SafUtils
import com.droidcode.project.WorkspaceManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FileClipboardItem(
    val fileNode: FileNode,
    val isCut: Boolean
)

@Composable
fun ExplorerPanel(
    onOpenFile: (File) -> Unit,
    onCloseProject: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val workspaceMgr = remember { WorkspaceManager.getInstance() }
    val fs = remember { LocalFileSystem.getInstance() }
    val coroutineScope = rememberCoroutineScope()

    var treeNodes by remember { mutableStateOf(workspaceMgr.workspaceFileTree) }
    var expandedPaths by remember { mutableStateOf(setOf<String>()) }
    var isLoadingTree by remember { mutableStateOf(false) }

    var selectedNode by remember { mutableStateOf<FileNode?>(null) }
    var clipboardItem by remember { mutableStateOf<FileClipboardItem?>(null) }

    var showToolbarOverflow by remember { mutableStateOf(false) }
    var showPropertiesDialog by remember { mutableStateOf<FileNode?>(null) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableStateOf(0f) }
    var exportStatusText by remember { mutableStateOf("") }
    var exportTargetFolder by remember { mutableStateOf<File?>(null) }

    val safExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { destinationUri ->
        if (destinationUri != null) {
            val sourceDir = exportTargetFolder ?: workspaceMgr.currentProject?.directory
            if (sourceDir != null && sourceDir.exists()) {
                isExporting = true
                exportProgress = 0f
                exportStatusText = "Preparing export..."
                coroutineScope.launch {
                    val result = SafUtils.exportProjectToSaf(
                        context = context,
                        sourceDir = sourceDir,
                        destinationTreeUri = destinationUri,
                        onProgress = { progress, fileName ->
                            exportProgress = progress
                            exportStatusText = "Exporting: $fileName"
                        }
                    )
                    isExporting = false
                    result.fold(
                        onSuccess = { count ->
                            Toast.makeText(
                                context,
                                "Exported ${sourceDir.name} ($count files) to device storage!",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        onFailure = { error ->
                            Toast.makeText(
                                context,
                                "Export failed: ${error.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                }
            } else {
                Toast.makeText(context, "No directory available to export", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val refreshTree: () -> Unit = {
        coroutineScope.launch {
            isLoadingTree = true
            val nodes = withContext(Dispatchers.IO) {
                workspaceMgr.workspaceFileTree
            }
            treeNodes = nodes
            isLoadingTree = false
        }
    }

    val handlePaste: (FileNode?) -> Unit = { targetFolderNode ->
        val item = clipboardItem
        val currentProj = workspaceMgr.currentProject
        if (item != null && workspaceMgr.hasOpenWorkspace() && currentProj != null) {
            val targetDir = if (targetFolderNode != null && targetFolderNode.isFolder) {
                File(targetFolderNode.path)
            } else if (targetFolderNode != null && !targetFolderNode.isFolder) {
                File(targetFolderNode.path).parentFile ?: currentProj.directory
            } else if (selectedNode != null && selectedNode!!.isFolder) {
                File(selectedNode!!.path)
            } else if (selectedNode != null && !selectedNode!!.isFolder) {
                File(selectedNode!!.path).parentFile ?: currentProj.directory
            } else {
                currentProj.directory
            }

            coroutineScope.launch {
                isLoadingTree = true
                try {
                    val sourceFile = File(item.fileNode.path)
                    val resultFile = withContext(Dispatchers.IO) {
                        if (item.isCut) {
                            fs.moveFileOrDirectory(sourceFile, targetDir)
                        } else {
                            fs.copyFileOrDirectory(sourceFile, targetDir)
                        }
                    }
                    if (item.isCut) {
                        clipboardItem = null
                        Toast.makeText(context, "Moved '${sourceFile.name}' into '${targetDir.name}'", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Copied '${sourceFile.name}' into '${targetDir.name}'", Toast.LENGTH_SHORT).show()
                    }
                    expandedPaths = expandedPaths + targetDir.absolutePath
                    val nodes = withContext(Dispatchers.IO) {
                        workspaceMgr.workspaceFileTree
                    }
                    treeNodes = nodes
                    FileNode.fromFile(resultFile)?.let { selectedNode = it }
                } catch (e: Exception) {
                    Toast.makeText(context, "Operation failed: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isLoadingTree = false
                }
            }
        }
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
        // Explorer Header & Contextual Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "EXPLORER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                if (workspaceMgr.hasOpenWorkspace()) {
                    Text(
                        text = workspaceMgr.currentProject?.name ?: "",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Contextual Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (selectedNode == null) {
                    // Default State: New File, New Folder, Refresh, Overflow
                    IconButton(
                        onClick = {
                            if (workspaceMgr.hasOpenWorkspace()) {
                                showNewFileDialog = true
                            } else {
                                Toast.makeText(context, "Open a project first", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("explorer_new_file_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.NoteAdd,
                            contentDescription = "New File",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            .size(36.dp)
                            .testTag("explorer_new_folder_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = "New Folder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { refreshTree() },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("explorer_refresh_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh File Tree",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    // Selected State: Cut, Copy, Rename, Delete, Overflow
                    IconButton(
                        onClick = {
                            clipboardItem = FileClipboardItem(selectedNode!!, isCut = true)
                            Toast.makeText(context, "Cut '${selectedNode!!.name}'", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("explorer_cut_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCut,
                            contentDescription = "Cut",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            clipboardItem = FileClipboardItem(selectedNode!!, isCut = false)
                            Toast.makeText(context, "Copied '${selectedNode!!.name}'", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("explorer_copy_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { showRenameDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("explorer_rename_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("explorer_delete_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Overflow Menu
                Box {
                    IconButton(
                        onClick = { showToolbarOverflow = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("explorer_overflow_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Explorer Actions Menu",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showToolbarOverflow,
                        onDismissRequest = { showToolbarOverflow = false },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    ) {
                        // Paste
                        DropdownMenuItem(
                            text = { Text("Paste", fontSize = 13.sp) },
                            enabled = clipboardItem != null,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            onClick = {
                                showToolbarOverflow = false
                                handlePaste(selectedNode)
                            }
                        )

                        // Duplicate
                        if (selectedNode != null) {
                            DropdownMenuItem(
                                text = { Text("Duplicate", fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                onClick = {
                                    showToolbarOverflow = false
                                    val node = selectedNode ?: return@DropdownMenuItem
                                    coroutineScope.launch {
                                        isLoadingTree = true
                                        try {
                                            val duplicated = withContext(Dispatchers.IO) {
                                                fs.duplicateFile(File(node.path))
                                            }
                                            Toast.makeText(context, "Duplicated to '${duplicated.name}'", Toast.LENGTH_SHORT).show()
                                            refreshTree()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Duplicate failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isLoadingTree = false
                                        }
                                    }
                                }
                            )

                            // Copy Path
                            DropdownMenuItem(
                                text = { Text("Copy Path", fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                onClick = {
                                    showToolbarOverflow = false
                                    selectedNode?.let { node ->
                                        clipboardManager.setText(AnnotatedString(node.path))
                                        Toast.makeText(context, "Path copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )

                            // Properties
                            DropdownMenuItem(
                                text = { Text("Properties", fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                onClick = {
                                    showToolbarOverflow = false
                                    showPropertiesDialog = selectedNode
                                }
                            )

                            // Export selection if folder
                            if (selectedNode!!.isFolder) {
                                DropdownMenuItem(
                                    text = { Text("Export Folder to Device...", fontSize = 13.sp) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.SdCard,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    onClick = {
                                        showToolbarOverflow = false
                                        exportTargetFolder = File(selectedNode!!.path)
                                        safExportLauncher.launch(null)
                                    }
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            DropdownMenuItem(
                                text = { Text("Clear Selection", fontSize = 13.sp) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                onClick = {
                                    showToolbarOverflow = false
                                    selectedNode = null
                                }
                            )
                        } else {
                            // Default state extra options: Collapse All, Refresh
                            DropdownMenuItem(
                                text = { Text("Collapse All", fontSize = 13.sp) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                onClick = {
                                    showToolbarOverflow = false
                                    expandedPaths = emptySet()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Refresh", fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                onClick = {
                                    showToolbarOverflow = false
                                    refreshTree()
                                }
                            )
                        }

                        if (workspaceMgr.hasOpenWorkspace()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            DropdownMenuItem(
                                text = { Text("Export Project to Device...", fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.SdCard,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                onClick = {
                                    showToolbarOverflow = false
                                    exportTargetFolder = workspaceMgr.currentProject?.directory
                                    safExportLauncher.launch(null)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Exporting to Device Progress Card
        if (isExporting) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SdCard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Exporting to Device (SAF)...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${(exportProgress * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { exportProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = exportStatusText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Progress Indicator while reading large project
        if (isLoadingTree) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reading project files...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Scanning...",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // Clipboard Active Status Banner
        if (clipboardItem != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (clipboardItem!!.isCut) Icons.Default.ContentCut else Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${if (clipboardItem!!.isCut) "Cut" else "Copied"}: ${clipboardItem!!.fileNode.name}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { handlePaste(selectedNode) }
                        ) {
                            Text(
                                text = "Paste Here",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { clipboardItem = null },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Clipboard",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
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
        } else if (treeNodes.isEmpty() && !isLoadingTree) {
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
                        clipboardItem = clipboardItem,
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
                        },
                        onCutRequested = { targetNode ->
                            clipboardItem = FileClipboardItem(targetNode, isCut = true)
                            Toast.makeText(context, "Cut '${targetNode.name}'", Toast.LENGTH_SHORT).show()
                        },
                        onCopyRequested = { targetNode ->
                            clipboardItem = FileClipboardItem(targetNode, isCut = false)
                            Toast.makeText(context, "Copied '${targetNode.name}'", Toast.LENGTH_SHORT).show()
                        },
                        onPasteRequested = { targetNode ->
                            handlePaste(targetNode)
                        },
                        onNewFileRequested = { targetNode ->
                            selectedNode = targetNode
                            showNewFileDialog = true
                        },
                        onNewFolderRequested = { targetNode ->
                            selectedNode = targetNode
                            showNewFolderDialog = true
                        },
                        onRenameRequested = { targetNode ->
                            selectedNode = targetNode
                            showRenameDialog = true
                        },
                        onDeleteRequested = { targetNode ->
                            selectedNode = targetNode
                            showDeleteDialog = true
                        },
                        onDuplicateRequested = { targetNode ->
                            coroutineScope.launch {
                                isLoadingTree = true
                                try {
                                    val duplicated = withContext(Dispatchers.IO) {
                                        fs.duplicateFile(File(targetNode.path))
                                    }
                                    Toast.makeText(context, "Duplicated to '${duplicated.name}'", Toast.LENGTH_SHORT).show()
                                    refreshTree()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Duplicate failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isLoadingTree = false
                                }
                            }
                        },
                        onPropertiesRequested = { targetNode ->
                            showPropertiesDialog = targetNode
                        },
                        onExportRequested = { targetNode ->
                            exportTargetFolder = File(targetNode.path)
                            safExportLauncher.launch(null)
                        }
                    )
                }
            }
        }
    }

    // Modal Action Dialogs
    val activeProjForDialog = workspaceMgr.currentProject
    if (showNewFileDialog && workspaceMgr.hasOpenWorkspace() && activeProjForDialog != null) {
        val parentDir = if (selectedNode != null && selectedNode!!.isFolder) {
            File(selectedNode!!.path)
        } else if (selectedNode != null && !selectedNode!!.isFolder) {
            File(selectedNode!!.path).parentFile ?: activeProjForDialog.directory
        } else {
            activeProjForDialog.directory
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

    if (showNewFolderDialog && workspaceMgr.hasOpenWorkspace() && activeProjForDialog != null) {
        val parentDir = if (selectedNode != null && selectedNode!!.isFolder) {
            File(selectedNode!!.path)
        } else if (selectedNode != null && !selectedNode!!.isFolder) {
            File(selectedNode!!.path).parentFile ?: activeProjForDialog.directory
        } else {
            activeProjForDialog.directory
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

    if (showPropertiesDialog != null) {
        val node = showPropertiesDialog!!
        val file = File(node.path)
        val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
        AlertDialog(
            onDismissRequest = { showPropertiesDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (node.isFolder) Icons.Default.Folder else Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Properties", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column {
                        Text("Name", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(node.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Column {
                        Text("Path", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(node.path, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Type", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (node.isFolder) "Directory" else "File (${node.extension.ifEmpty { "plain" }})", fontSize = 13.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Size", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val sizeStr = if (node.isFolder) "-" else when {
                                file.length() < 1024 -> "${file.length()} B"
                                file.length() < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", file.length() / 1024.0)
                                else -> String.format(Locale.US, "%.2f MB", file.length() / (1024.0 * 1024.0))
                            }
                            Text(sizeStr, fontSize = 13.sp)
                        }
                    }
                    Column {
                        Text("Last Modified", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(dateFormat.format(Date(file.lastModified())), fontSize = 12.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Readable: ${if (file.canRead()) "Yes" else "No"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Writable: ${if (file.canWrite()) "Yes" else "No"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPropertiesDialog = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun FileTreeItem(
    node: FileNode,
    depth: Int,
    selectedPath: String?,
    clipboardItem: FileClipboardItem?,
    expandedPaths: Set<String>,
    onFolderToggle: (FileNode) -> Unit,
    onNodeClick: (FileNode) -> Unit,
    onCutRequested: (FileNode) -> Unit,
    onCopyRequested: (FileNode) -> Unit,
    onPasteRequested: (FileNode) -> Unit,
    onNewFileRequested: (FileNode) -> Unit,
    onNewFolderRequested: (FileNode) -> Unit,
    onRenameRequested: (FileNode) -> Unit,
    onDeleteRequested: (FileNode) -> Unit,
    onDuplicateRequested: (FileNode) -> Unit = {},
    onPropertiesRequested: (FileNode) -> Unit = {},
    onExportRequested: ((FileNode) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isSelected = selectedPath == node.path
    val isExpanded = expandedPaths.contains(node.path)
    val isCutItem = clipboardItem?.fileNode?.path == node.path && clipboardItem.isCut
    var showContextMenu by remember { mutableStateOf(false) }

    val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface

    Column {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = (depth * 14).dp, top = 2.dp, bottom = 2.dp)
                    .background(bg, RoundedCornerShape(6.dp))
                    .alpha(if (isCutItem) 0.5f else 1.0f)
                    .pointerInput(node.path) {
                        detectTapGestures(
                            onTap = { onNodeClick(node) },
                            onLongPress = { showContextMenu = true }
                        )
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
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
                        imageVector = getFileIcon(node.name),
                        contentDescription = null,
                        tint = getFileIconColor(node.name),
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .size(20.dp)
                    )
                }

                Text(
                    text = node.name + (if (isCutItem) " [Cut]" else ""),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (node.isFolder) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { showContextMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            ) {
                if (!node.isFolder) {
                    DropdownMenuItem(
                        text = { Text("Open File", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(18.dp)) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        onClick = {
                            showContextMenu = false
                            onNodeClick(node)
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Cut", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.ContentCut, null, modifier = Modifier.size(18.dp)) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    onClick = {
                        showContextMenu = false
                        onCutRequested(node)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Copy", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp)) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    onClick = {
                        showContextMenu = false
                        onCopyRequested(node)
                    }
                )
                if (clipboardItem != null) {
                    DropdownMenuItem(
                        text = { Text("Paste Here", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.ContentPaste, null, modifier = Modifier.size(18.dp)) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        onClick = {
                            showContextMenu = false
                            onPasteRequested(node)
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("New File Here", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.NoteAdd, null, modifier = Modifier.size(18.dp)) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    onClick = {
                        showContextMenu = false
                        onNewFileRequested(node)
                    }
                )
                DropdownMenuItem(
                    text = { Text("New Folder Here", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.CreateNewFolder, null, modifier = Modifier.size(18.dp)) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    onClick = {
                        showContextMenu = false
                        onNewFolderRequested(node)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Rename", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    onClick = {
                        showContextMenu = false
                        onRenameRequested(node)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Copy Path", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp)) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    onClick = {
                        showContextMenu = false
                        clipboardManager.setText(AnnotatedString(node.path))
                        Toast.makeText(context, "Path copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Duplicate", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp)) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    onClick = {
                        showContextMenu = false
                        onDuplicateRequested(node)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Properties", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Info, null, modifier = Modifier.size(18.dp)) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    onClick = {
                        showContextMenu = false
                        onPropertiesRequested(node)
                    }
                )
                if (node.isFolder && onExportRequested != null) {
                    DropdownMenuItem(
                        text = { Text("Export to Device...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.SdCard, null, modifier = Modifier.size(18.dp)) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        onClick = {
                            showContextMenu = false
                            onExportRequested(node)
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Delete", fontSize = 13.sp, color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    onClick = {
                        showContextMenu = false
                        onDeleteRequested(node)
                    }
                )
            }
        }

        if (node.isFolder && isExpanded) {
            node.children.forEach { child ->
                FileTreeItem(
                    node = child,
                    depth = depth + 1,
                    selectedPath = selectedPath,
                    clipboardItem = clipboardItem,
                    expandedPaths = expandedPaths,
                    onFolderToggle = onFolderToggle,
                    onNodeClick = onNodeClick,
                    onCutRequested = onCutRequested,
                    onCopyRequested = onCopyRequested,
                    onPasteRequested = onPasteRequested,
                    onNewFileRequested = onNewFileRequested,
                    onNewFolderRequested = onNewFolderRequested,
                    onRenameRequested = onRenameRequested,
                    onDeleteRequested = onDeleteRequested,
                    onDuplicateRequested = onDuplicateRequested,
                    onPropertiesRequested = onPropertiesRequested,
                    onExportRequested = onExportRequested
                )
            }
        }
    }
}

@Composable
private fun getFileIcon(nameOrExt: String) = FileIconUtils.getFileIcon(nameOrExt)

@Composable
private fun getFileIconColor(nameOrExt: String) = FileIconUtils.getFileIconColor(nameOrExt)

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

