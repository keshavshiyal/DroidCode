package com.droidcode.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.droidcode.db.WorkspaceEntity
import com.droidcode.filesystem.SafUtils
import com.droidcode.project.ProjectTemplate
import com.droidcode.project.WorkspaceManager
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun HomeView(
    onOpenWorkspace: (String, String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGeneralMenu: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val workspaceMgr = remember { WorkspaceManager.getInstance() }
    val recentWorkspaces by workspaceMgr.getRecentWorkspacesFlow(context)
        .collectAsState(initial = emptyList())

    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showOpenDirDialog by remember { mutableStateOf(false) }

    // SAF (Storage Access Framework) Folder Picker Launcher
    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val resolvedPath = SafUtils.resolvePathFromTreeUri(context, uri)
            if (resolvedPath != null) {
                onOpenWorkspace(resolvedPath, "General Workspace")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "<D/> DroidCode",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = FontFamily.Monospace
        )

        Text(
            text = "Professional Native Android IDE Workstation",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick Actions Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Start",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                ActionTile(
                    title = "New Project",
                    subtitle = "Create workspace from template",
                    icon = Icons.Default.Add,
                    onClick = { showNewProjectDialog = true },
                    testTag = "home_new_project_btn"
                )

                Spacer(modifier = Modifier.height(8.dp))

                ActionTile(
                    title = "Open Directory (SAF)",
                    subtitle = "Select folder from device locations",
                    icon = Icons.Default.FolderOpen,
                    onClick = { showOpenDirDialog = true },
                    testTag = "home_open_directory_btn"
                )

                Spacer(modifier = Modifier.height(8.dp))

                ActionTile(
                    title = "Settings",
                    subtitle = "Theme, fonts & preferences",
                    icon = Icons.Default.Settings,
                    onClick = onOpenSettings,
                    testTag = "home_settings_btn"
                )
            }

            // Recent Workspaces Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recent Workspaces",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (recentWorkspaces.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recent projects",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.height(180.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(recentWorkspaces) { workspace ->
                            RecentWorkspaceTile(
                                workspace = workspace,
                                onClick = {
                                    if (File(workspace.path).exists()) {
                                        onOpenWorkspace(workspace.path, workspace.type)
                                    } else {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Workspace directory '${workspace.path}' is unavailable or moved.",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                                onRemove = {
                                    coroutineScope.launch {
                                        workspaceMgr.removeWorkspace(context, workspace.path)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNewProjectDialog) {
        NewProjectModal(
            onDismiss = { showNewProjectDialog = false },
            onCreate = { path, type ->
                showNewProjectDialog = false
                onOpenWorkspace(path, type)
            }
        )
    }

    if (showOpenDirDialog) {
        OpenDirectoryModal(
            onDismiss = { showOpenDirDialog = false },
            onLaunchSaf = {
                showOpenDirDialog = false
                safLauncher.launch(null)
            },
            onOpen = { path ->
                showOpenDirDialog = false
                onOpenWorkspace(path, "General Workspace")
            }
        )
    }
}

@Composable
private fun ActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .padding(10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecentWorkspaceTile(
    workspace: WorkspaceEntity,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val exists = remember(workspace.path) { File(workspace.path).exists() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (exists) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = workspace.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (exists) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                )
                if (!exists) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(Moved/Unavailable)",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Text(
                text = workspace.path,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove from recents",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun NewProjectModal(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    val context = LocalContext.current
    var projectName by remember { mutableStateOf("MyProject") }
    var selectedTemplate by remember { mutableStateOf(ProjectTemplate.Type.WEB) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "New Project",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Project Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Select Template:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                ProjectTemplate.Type.values().forEach { template ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(
                                if (selectedTemplate == template)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                RoundedCornerShape(4.dp)
                            )
                            .clickable { selectedTemplate = template }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = template.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = template.description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            try {
                                val rootDir = context.filesDir
                                val project = ProjectTemplate.createProjectFromTemplate(
                                    rootDir,
                                    projectName.trim(),
                                    selectedTemplate
                                )
                                onCreate(project.path, project.type)
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Failed to create project"
                            }
                        }
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

@Composable
private fun OpenDirectoryModal(
    onDismiss: () -> Unit,
    onLaunchSaf: () -> Unit,
    onOpen: (String) -> Unit
) {
    val context = LocalContext.current
    val defaultPath = remember { context.filesDir.absolutePath }
    var inputPath by remember { mutableStateOf(defaultPath) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Open Directory",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onLaunchSaf,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Browse Device Locations (SAF)")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Or enter custom directory path manually:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = inputPath,
                    onValueChange = { inputPath = it },
                    label = { Text("Directory Path") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val dir = java.io.File(inputPath.trim())
                            if (dir.exists() && dir.isDirectory) {
                                onOpen(dir.absolutePath)
                            } else {
                                errorMessage = "Directory does not exist or is invalid"
                            }
                        }
                    ) {
                        Text("Open")
                    }
                }
            }
        }
    }
}
