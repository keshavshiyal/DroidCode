package com.droidcode.ui

import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.droidcode.R
import com.droidcode.db.WorkspaceEntity
import com.droidcode.filesystem.SafUtils
import com.droidcode.project.ProjectTemplate
import com.droidcode.project.WorkspaceManager
import com.droidcode.ui.theme.CornerMedium
import com.droidcode.ui.theme.CornerSmall
import com.droidcode.ui.theme.SpacingL
import com.droidcode.ui.theme.SpacingM
import com.droidcode.ui.theme.SpacingS
import com.droidcode.ui.theme.SpacingXL
import com.droidcode.ui.theme.SpacingXS
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

    var showNewProjectDialog by rememberSaveable { mutableStateOf(false) }
    var showOpenDirDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        workspaceMgr.syncInternalProjects(context)
    }

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

    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredWorkspaces = remember(recentWorkspaces, searchQuery) {
        if (searchQuery.isBlank()) recentWorkspaces
        else recentWorkspaces.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.path.contains(searchQuery, ignoreCase = true) ||
            (it.type?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(SpacingXL),
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
            text = stringResource(R.string.home_subtitle),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = SpacingXS, bottom = SpacingXL)
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
        ) {
            val isNarrow = maxWidth < 580.dp

            val startSection = @Composable { modifier: Modifier ->
                Column(
                    modifier = modifier
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(CornerMedium))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(CornerMedium))
                        .padding(SpacingM)
                ) {
                    Text(
                        text = "Start",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = SpacingS)
                    )

                    ActionTile(
                        title = stringResource(R.string.home_new_project),
                        subtitle = stringResource(R.string.home_new_project_desc),
                        icon = Icons.Default.Add,
                        onClick = { showNewProjectDialog = true },
                        testTag = "home_new_project_btn"
                    )

                    Spacer(modifier = Modifier.height(SpacingS))

                    ActionTile(
                        title = stringResource(R.string.home_open_folder),
                        subtitle = stringResource(R.string.home_open_folder_desc),
                        icon = Icons.Default.FolderOpen,
                        onClick = { showOpenDirDialog = true },
                        testTag = "home_open_directory_btn"
                    )

                    Spacer(modifier = Modifier.height(SpacingS))

                    ActionTile(
                        title = stringResource(R.string.menu_settings),
                        subtitle = stringResource(R.string.cmd_settings_editor),
                        icon = Icons.Default.Settings,
                        onClick = onOpenSettings,
                        testTag = "home_settings_btn"
                    )
                }
            }

            val recentsSection = @Composable { modifier: Modifier ->
                Column(
                    modifier = modifier
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(CornerMedium))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(CornerMedium))
                        .padding(SpacingM)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = SpacingS),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(SpacingS))
                            Text(
                                text = stringResource(R.string.recent_workspaces_title),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (recentWorkspaces.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        text = "${recentWorkspaces.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        val missingCount = remember(recentWorkspaces) {
                            recentWorkspaces.count { !File(it.path).exists() }
                        }
                        if (missingCount > 0) {
                            TextButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val missing = recentWorkspaces.filter { !File(it.path).exists() }
                                        missing.forEach { workspaceMgr.removeWorkspace(context, it.path) }
                                        Toast.makeText(context, "Cleaned $missingCount missing workspace(s)", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Clean Missing",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    if (recentWorkspaces.size >= 4) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Filter recents...", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(16.dp))
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = SpacingS),
                            shape = RoundedCornerShape(CornerMedium)
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
                                text = stringResource(R.string.home_no_recents),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    } else if (filteredWorkspaces.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No workspaces match \"$searchQuery\"",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 160.dp, max = 340.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredWorkspaces, key = { it.path }) { workspace ->
                                RecentWorkspaceTile(
                                    workspace = workspace,
                                    onClick = {
                                        if (File(workspace.path).exists()) {
                                            onOpenWorkspace(workspace.path, workspace.type)
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Workspace directory '${workspace.path}' is unavailable or moved.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    },
                                    onRemove = {
                                        coroutineScope.launch {
                                            workspaceMgr.removeWorkspace(context, workspace.path)
                                        }
                                    },
                                    onDeletePermanently = {
                                        coroutineScope.launch {
                                            val deleted = workspaceMgr.deleteWorkspacePermanently(context, workspace.path)
                                            val msg = if (deleted) "Project '${workspace.name}' permanently deleted"
                                                      else "Project '${workspace.name}' removed"
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (isNarrow) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(SpacingM)
                ) {
                    startSection(Modifier.fillMaxWidth())
                    recentsSection(Modifier.fillMaxWidth())
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SpacingM)
                ) {
                    startSection(Modifier.weight(1f))
                    recentsSection(Modifier.weight(1.15f))
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

private fun getWorkspaceIcon(type: String?, isGit: Boolean): ImageVector {
    if (isGit) return Icons.Default.AccountTree
    val t = type?.lowercase() ?: ""
    return when {
        t.contains("python") -> Icons.Default.Terminal
        t.contains("android") || t.contains("kotlin") || t.contains("java") -> Icons.Default.Code
        t.contains("web") || t.contains("html") || t.contains("javascript") || t.contains("js") -> Icons.Default.Language
        else -> Icons.Default.Folder
    }
}

@Composable
private fun RecentWorkspaceTile(
    workspace: WorkspaceEntity,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    val context = LocalContext.current
    val exists = remember(workspace.path) { File(workspace.path).exists() }
    val isInternal = remember(workspace.path) {
        workspace.path.startsWith(context.filesDir.absolutePath)
    }
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }

    if (showConfirmDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteDialog = false },
            title = {
                Text(
                    text = "Delete Project Permanently?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete \"${workspace.name}\" and all of its files from internal app storage? This action cannot be undone.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDeleteDialog = false
                        onDeletePermanently()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Permanently", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (exists) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                RoundedCornerShape(CornerMedium)
            )
            .border(
                1.dp,
                if (exists) MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.error.copy(alpha = 0.35f),
                RoundedCornerShape(CornerMedium)
            )
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Project Type Avatar Icon
        val projectIcon = getWorkspaceIcon(workspace.type, workspace.isGitRepo)
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    if (exists) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    RoundedCornerShape(CornerSmall)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = projectIcon,
                contentDescription = null,
                tint = if (exists) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = workspace.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (exists) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (isInternal) {
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "App Storage",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (workspace.isGitRepo) {
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "Git",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                if (!exists) {
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "Missing",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = workspace.path,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (workspace.lastOpenedTimestamp > 0L) {
                Spacer(modifier = Modifier.height(2.dp))
                val relativeTime = DateUtils.getRelativeTimeSpanString(
                    workspace.lastOpenedTimestamp,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
                ).toString()
                Text(
                    text = "Opened $relativeTime",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isInternal) {
                IconButton(
                    onClick = { showConfirmDeleteDialog = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete permanently",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(2.dp))
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
}

@Composable
private fun NewProjectModal(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    val context = LocalContext.current
    var projectName by rememberSaveable { mutableStateOf("MyProject") }
    var selectedTemplateName by rememberSaveable { mutableStateOf(ProjectTemplate.Type.WEB.name) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedTemplate = try {
        ProjectTemplate.Type.valueOf(selectedTemplateName)
    } catch (e: Exception) {
        ProjectTemplate.Type.WEB
    }
    val isProjectNameValid = projectName.trim().isNotEmpty()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingM),
            shape = RoundedCornerShape(CornerMedium),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 580.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(SpacingL)
            ) {
                Text(
                    text = stringResource(R.string.new_project_dialog_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(SpacingM))

                OutlinedTextField(
                    value = projectName,
                    onValueChange = {
                        projectName = it
                        if (errorMessage != null) errorMessage = null
                    },
                    label = { Text(stringResource(R.string.project_name_label)) },
                    singleLine = true,
                    isError = !isProjectNameValid,
                    supportingText = if (!isProjectNameValid) {
                        { Text(stringResource(R.string.project_name_blank_error), color = MaterialTheme.colorScheme.error) }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_project_name_input")
                )

                Spacer(modifier = Modifier.height(SpacingM))

                Text(
                    text = stringResource(R.string.select_template_label),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(SpacingS))

                Column(verticalArrangement = Arrangement.spacedBy(SpacingS)) {
                    ProjectTemplate.Type.values().forEach { template ->
                        val isSelected = selectedTemplate == template
                        val icon = when (template) {
                            ProjectTemplate.Type.WEB -> Icons.Default.Code
                            ProjectTemplate.Type.PYTHON -> Icons.Default.Terminal
                            ProjectTemplate.Type.SQL -> Icons.Default.Storage
                            ProjectTemplate.Type.MARKDOWN -> Icons.Default.Description
                            else -> Icons.Default.Folder
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTemplateName = template.name },
                            shape = RoundedCornerShape(CornerMedium),
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = SpacingM, vertical = SpacingS),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedTemplateName = template.name },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(SpacingS))
                                Surface(
                                    shape = RoundedCornerShape(CornerSmall),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(SpacingM))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = template.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = template.description,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = SpacingS)
                    )
                }

                Spacer(modifier = Modifier.height(SpacingL))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Spacer(modifier = Modifier.width(SpacingS))
                    Button(
                        enabled = isProjectNameValid,
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
                        },
                        modifier = Modifier.testTag("new_project_create_btn")
                    ) {
                        Text(stringResource(R.string.action_create))
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
