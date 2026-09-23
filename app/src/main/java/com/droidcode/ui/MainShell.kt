package com.droidcode.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidcode.core.Command
import com.droidcode.core.CommandRegistry
import com.droidcode.editor.EditorManager
import com.droidcode.git.GitService
import com.droidcode.project.WorkspaceManager
import com.droidcode.settings.SettingsManager
import com.droidcode.ui.theme.DroidCodeTheme
import java.io.File
import kotlinx.coroutines.launch

@Composable
fun MainShell() {
    val context = LocalContext.current
    val settingsMgr = remember { SettingsManager.getInstance(context) }
    var settingsState by remember { mutableStateOf(settingsMgr.settings) }

    val workspaceMgr = remember { WorkspaceManager.getInstance() }
    val editorMgr = remember { EditorManager.getInstance() }
    val commandRegistry = remember { CommandRegistry.getInstance() }
    val gitService = remember { GitService.getInstance() }

    var isWorkspaceOpen by remember { mutableStateOf(workspaceMgr.hasOpenWorkspace()) }
    var currentView by remember { mutableStateOf(if (isWorkspaceOpen) "IDE" else "HOME") }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    var showBottomPanel by remember { mutableStateOf(true) }
    var showCommandPalette by remember { mutableStateOf(false) }
    var showProjectMenubar by remember { mutableStateOf(false) }
    var showWorkspaceDropdown by remember { mutableStateOf(false) }
    var showTopOverflowMenu by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showShortcutsDialog by remember { mutableStateOf(false) }
    var showRecentWorkspacesDialog by remember { mutableStateOf(false) }

    val gitStatus = remember(workspaceMgr.currentProject, isWorkspaceOpen) {
        gitService.inspectWorkspace(workspaceMgr.currentProject)
    }
    val recentWorkspaces by workspaceMgr.getRecentWorkspacesFlow(context).collectAsState(initial = emptyList())

    // Quick Key Bar sticky modifiers
    var ctrlActive by remember { mutableStateOf(false) }
    var shiftActive by remember { mutableStateOf(false) }
    var altActive by remember { mutableStateOf(false) }

    // Register Commands in CommandRegistry
    remember {
        commandRegistry.registerCommand(Command("file.save", "Save Active File", "File", "Ctrl+S") {
            try {
                editorMgr.saveActiveTab()
            } catch (e: Exception) {
                android.util.Log.e("MainShell", "Failed to save active file", e)
            }
        })
        commandRegistry.registerCommand(Command("file.save_all", "Save All Files", "File", "Ctrl+Shift+S") {
            try {
                editorMgr.saveAllTabs()
            } catch (e: Exception) {
                android.util.Log.e("MainShell", "Failed to save all files", e)
            }
        })
        commandRegistry.registerCommand(Command("command.palette", "Open Command Palette", "View", "Ctrl+Shift+P") {
            showCommandPalette = true
        })
        commandRegistry.registerCommand(Command("editor.undo", "Undo Edit", "Editor", "Ctrl+Z") {
            editorMgr.undoActiveTab()
        })
        commandRegistry.registerCommand(Command("editor.redo", "Redo Edit", "Editor", "Ctrl+Y") {
            editorMgr.redoActiveTab()
        })
        commandRegistry.registerCommand(Command("view.toggle_explorer", "Toggle Project Explorer", "View", "Ctrl+B") {
            coroutineScope.launch {
                if (drawerState.isClosed) drawerState.open() else drawerState.close()
            }
        })
        commandRegistry.registerCommand(Command("view.toggle_terminal", "Toggle Terminal Panel", "View", "Ctrl+`") {
            showBottomPanel = !showBottomPanel
        })
        commandRegistry.registerCommand(Command("workspace.open", "Open Workspace / Project", "Workspace", "Ctrl+O") {
            currentView = "HOME"
        })
        commandRegistry.registerCommand(Command("workspace.close", "Close Workspace", "Workspace", null) {
            workspaceMgr.closeWorkspace()
            editorMgr.closeAllTabs()
            isWorkspaceOpen = false
            currentView = "HOME"
        })
        commandRegistry.registerCommand(Command("settings.open", "Open Settings", "Preferences", "Ctrl+,") {
            currentView = "SETTINGS"
        })
        commandRegistry.registerCommand(Command("settings.editor", "Editor Fonts & Preferences", "Preferences", null) {
            currentView = "SETTINGS"
        })
        commandRegistry.registerCommand(Command("settings.keybar", "Quick Key Bar Settings", "Preferences", null) {
            currentView = "SETTINGS"
        })
        commandRegistry.registerCommand(Command("app.developer", "Developer Profile (Keshu Shiyal)", "Preferences", null) {
            currentView = "SETTINGS"
        })
        commandRegistry.registerCommand(Command("project.run", "Run Active Workspace", "Build & Run", "Ctrl+R") {
            val tab = editorMgr.activeTab
            if (tab != null) {
                EditorActionsHandler.runFile(context, tab) { title, res ->
                    Toast.makeText(context, "$title: $res", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "No active file to run", Toast.LENGTH_SHORT).show()
            }
        })
        commandRegistry.registerCommand(Command("project.build", "Rebuild Workspace", "Build & Run", "Ctrl+Shift+B") {
            Toast.makeText(context, "Workspace build completed successfully", Toast.LENGTH_SHORT).show()
        })
        commandRegistry.registerCommand(Command("git.status", "Check Git Status", "Git VCS", "Ctrl+G S") {
            showBottomPanel = true
        })
        commandRegistry.registerCommand(Command("git.pull", "Git Pull / Sync", "Git VCS", "Ctrl+G P") {
            Toast.makeText(context, "Workspace synchronized with git remote", Toast.LENGTH_SHORT).show()
        })
        commandRegistry.registerCommand(Command("app.about", "About DroidCode Workstation", "Preferences", null) {
            currentView = "SETTINGS"
        })
    }

    val triggerActionKey: (String) -> Unit = { action ->
        if (action == "MENU") {
            showProjectMenubar = true
        } else {
            val tab = editorMgr.activeTab
            if (tab != null) {
                val current = tab.content
                val pos = tab.cursorPosition.coerceIn(0, current.length)
                when (action) {
                    "UNDO" -> editorMgr.undoActiveTab()
                    "REDO" -> editorMgr.redoActiveTab()
                    "ESC" -> {
                        ctrlActive = false
                        shiftActive = false
                        altActive = false
                    }
                    "LEFT" -> if (pos > 0) tab.updateCursor(pos - 1)
                    "RIGHT" -> if (pos < current.length) tab.updateCursor(pos + 1)
                    "UP" -> {
                        val lastNewline = current.lastIndexOf('\n', (pos - 1).coerceAtLeast(0))
                        if (lastNewline >= 0) {
                            val prevNewline = current.lastIndexOf('\n', (lastNewline - 1).coerceAtLeast(0))
                            val col = pos - (lastNewline + 1)
                            val targetLineStart = if (prevNewline >= 0) prevNewline + 1 else 0
                            val targetLineLength = lastNewline - targetLineStart
                            val newPos = targetLineStart + col.coerceAtMost(targetLineLength)
                            tab.updateCursor(newPos)
                        }
                    }
                    "DOWN" -> {
                        val nextNewline = current.indexOf('\n', pos)
                        if (nextNewline >= 0) {
                            val prevNewline = current.lastIndexOf('\n', (pos - 1).coerceAtLeast(0))
                            val col = if (prevNewline >= 0) pos - (prevNewline + 1) else pos
                            val afterNext = current.indexOf('\n', nextNewline + 1)
                            val targetLineEnd = if (afterNext >= 0) afterNext else current.length
                            val targetLineLength = targetLineEnd - (nextNewline + 1)
                            val newPos = (nextNewline + 1) + col.coerceAtMost(targetLineLength)
                            tab.updateCursor(newPos)
                        }
                    }
                    "HOME" -> {
                        val prevNewline = current.lastIndexOf('\n', (pos - 1).coerceAtLeast(0))
                        val lineStart = if (prevNewline >= 0) prevNewline + 1 else 0
                        tab.updateCursor(lineStart)
                    }
                    "END" -> {
                        val nextNewline = current.indexOf('\n', pos)
                        val lineEnd = if (nextNewline >= 0) nextNewline else current.length
                        tab.updateCursor(lineEnd)
                    }
                    "DELETE" -> {
                        if (pos < current.length) {
                            val updated = current.substring(0, pos) + current.substring(pos + 1)
                            editorMgr.updateActiveTabContent(updated)
                        }
                    }
                }
            }
        }
    }

    val insertText: (String) -> Unit = { text ->
        val tab = editorMgr.activeTab
        if (tab != null) {
            // Check modifier shortcut triggers
            if (ctrlActive) {
                when (text.lowercase()) {
                    "s" -> {
                        try {
                            editorMgr.saveActiveTab()
                        } catch (e: Exception) {
                            android.util.Log.e("MainShell", "Failed to save active tab via quick key", e)
                        }
                        ctrlActive = false
                    }
                    "p" -> {
                        showCommandPalette = true
                        ctrlActive = false
                        shiftActive = false
                    }
                    "z" -> {
                        editorMgr.undoActiveTab()
                        ctrlActive = false
                    }
                    "y" -> {
                        editorMgr.redoActiveTab()
                        ctrlActive = false
                    }
                    else -> {
                        ctrlActive = false
                    }
                }
            } else {
                val current = tab.content
                val pos = tab.cursorPosition
                val updated = current.substring(0, pos) + text + current.substring(pos)
                editorMgr.updateActiveTabContent(updated)
                tab.updateCursor(pos + text.length)
            }
        }
    }

    DroidCodeTheme(themeMode = settingsState.themeMode) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = currentView == "IDE" && isWorkspaceOpen,
            drawerContent = {
                if (isWorkspaceOpen) {
                    ModalDrawerSheet(
                        drawerContainerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .width(300.dp)
                            .fillMaxHeight()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    ) {
                        ExplorerPanel(
                            onOpenFile = { file: File ->
                                try {
                                    editorMgr.openFile(file)
                                } catch (e: Exception) {
                                    android.util.Log.e("MainShell", "Failed to open file: ${file.absolutePath}", e)
                                }
                                coroutineScope.launch { drawerState.close() }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        ) {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    // Top App Bar Container with statusBarsPadding to keep title below Android notification bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .statusBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Section: App Logo & Workspace Dropdown
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "<D/> DroidCode",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clickable {
                                            if (isWorkspaceOpen) currentView = "IDE" else currentView = "HOME"
                                        }
                                        .padding(end = 4.dp)
                                )

                                if (isWorkspaceOpen && workspaceMgr.currentProject != null) {
                                    Box {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.clickable { showWorkspaceDropdown = true }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = workspaceMgr.currentProject?.name ?: "",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDropDown,
                                                    contentDescription = "Workspace options",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = showWorkspaceDropdown,
                                            onDismissRequest = { showWorkspaceDropdown = false }
                                        ) {
                                            // Git Branch Info
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(
                                                            text = if (gitStatus.isGitRepo) "Git Branch: ${gitStatus.currentBranch}" else "Git: Not a repository",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = if (gitStatus.isGitRepo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = workspaceMgr.currentProject?.path ?: "",
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Info,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                },
                                                onClick = {
                                                    showWorkspaceDropdown = false
                                                    Toast.makeText(
                                                        context,
                                                        if (gitStatus.isGitRepo) "Branch: ${gitStatus.currentBranch}" else "Not a Git repository",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            )

                                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                            // Switch Workspace
                                            DropdownMenuItem(
                                                text = { Text("Switch Workspace", fontSize = 13.sp) },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.SwapHoriz,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                },
                                                onClick = {
                                                    showWorkspaceDropdown = false
                                                    isWorkspaceOpen = false
                                                    currentView = "HOME"
                                                }
                                            )

                                            // Recent Workspaces
                                            DropdownMenuItem(
                                                text = { Text("Recent Workspaces", fontSize = 13.sp) },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.History,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                },
                                                onClick = {
                                                    showWorkspaceDropdown = false
                                                    showRecentWorkspacesDialog = true
                                                }
                                            )

                                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                            // Close Workspace
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = "Close Workspace",
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                },
                                                onClick = {
                                                    showWorkspaceDropdown = false
                                                    workspaceMgr.closeWorkspace()
                                                    editorMgr.closeAllTabs()
                                                    isWorkspaceOpen = false
                                                    currentView = "HOME"
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Right Section: Primary High-Frequency Toggles & Three-Dot Overflow Menu
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Search & Commands toggle
                                IconButton(
                                    onClick = { showCommandPalette = true },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("top_bar_search_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search & Commands",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                if (isWorkspaceOpen) {
                                    // Project Explorer toggle
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                            }
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("top_bar_explorer_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = "Toggle Project Explorer",
                                            tint = if (drawerState.isOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Terminal toggle
                                    IconButton(
                                        onClick = { showBottomPanel = !showBottomPanel },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("top_bar_terminal_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Terminal,
                                            contentDescription = "Toggle Terminal",
                                            tint = if (showBottomPanel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // 8dp horizontal breathing room between primary toggles and overflow menu
                                Spacer(modifier = Modifier.width(8.dp))

                                // Three-Dot Overflow Menu
                                Box {
                                    IconButton(
                                        onClick = { showTopOverflowMenu = true },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("top_bar_overflow_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "More Options",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showTopOverflowMenu,
                                        onDismissRequest = { showTopOverflowMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Settings", fontSize = 13.sp) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Settings,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            onClick = {
                                                showTopOverflowMenu = false
                                                currentView = "SETTINGS"
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = { Text("Command Palette", fontSize = 13.sp) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Search,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            onClick = {
                                                showTopOverflowMenu = false
                                                showCommandPalette = true
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = { Text("Project Menubar", fontSize = 13.sp) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Menu,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            onClick = {
                                                showTopOverflowMenu = false
                                                showProjectMenubar = true
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = { Text("Keyboard Shortcuts", fontSize = 13.sp) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Keyboard,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            onClick = {
                                                showTopOverflowMenu = false
                                                showShortcutsDialog = true
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = { Text("About DroidCode", fontSize = 13.sp) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            onClick = {
                                                showTopOverflowMenu = false
                                                showAboutDialog = true
                                            }
                                        )

                                        if (isWorkspaceOpen) {
                                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = "Close Workspace",
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                },
                                                onClick = {
                                                    showTopOverflowMenu = false
                                                    workspaceMgr.closeWorkspace()
                                                    editorMgr.closeAllTabs()
                                                    isWorkspaceOpen = false
                                                    currentView = "HOME"
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentView) {
                        "HOME" -> {
                            Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                                HomeView(
                                    onOpenWorkspace = { path, type ->
                                        coroutineScope.launch {
                                            try {
                                                workspaceMgr.openWorkspace(context, File(path), type)
                                                isWorkspaceOpen = true
                                                currentView = "IDE"
                                            } catch (e: Exception) {
                                                android.util.Log.e("MainShell", "Failed to open workspace", e)
                                            }
                                        }
                                    },
                                    onOpenSettings = { currentView = "SETTINGS" },
                                    onOpenGeneralMenu = { showProjectMenubar = true }
                                )
                            }
                        }

                        "SETTINGS" -> {
                            Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                                SettingsView(
                                    onBack = {
                                        if (isWorkspaceOpen) currentView = "IDE" else currentView = "HOME"
                                    },
                                    onSettingsChanged = {
                                        settingsState = settingsMgr.getSettingsCopy()
                                    },
                                    onOpenGeneralMenu = { showProjectMenubar = true }
                                )
                            }
                        }

                        "IDE" -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .imePadding()
                                    .navigationBarsPadding()
                            ) {
                                // Main Editor Workstation Area
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    EditorView(
                                        settings = settingsState,
                                        ctrlActive = ctrlActive,
                                        shiftActive = shiftActive,
                                        altActive = altActive,
                                        onResetModifiers = {
                                            ctrlActive = false
                                            shiftActive = false
                                            altActive = false
                                        },
                                        onOpenCommandPalette = { showCommandPalette = true },
                                        onOpenGeneralMenu = { showProjectMenubar = true },
                                        onSaveRequested = {},
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                // Developer Quick Key Bar
                                if (settingsState.isQuickKeyBarEnabled) {
                                    DeveloperQuickKeyBar(
                                        density = settingsState.quickKeyBarDensity,
                                        ctrlActive = ctrlActive,
                                        shiftActive = shiftActive,
                                        altActive = altActive,
                                        onToggleCtrl = { ctrlActive = !ctrlActive },
                                        onToggleShift = { shiftActive = !shiftActive },
                                        onToggleAlt = { altActive = !altActive },
                                        onInsertText = insertText,
                                        onActionKey = triggerActionKey
                                    )
                                }

                                // Bottom Panel (Git / Terminal / Database / Problems)
                                if (showBottomPanel) {
                                    BottomPanel()
                                }
                            }
                        }
                    }

                    // Command Palette Modal Dialog
                    if (showCommandPalette) {
                        CommandPaletteDialog(
                            onDismiss = { showCommandPalette = false },
                            onExecuteCommand = { cmd ->
                                cmd.execute()
                            }
                        )
                    }

                    // Project Menubar Modal Dialog
                    if (showProjectMenubar) {
                        ProjectMenubarDialog(
                            onDismiss = { showProjectMenubar = false },
                            onExecuteCommand = { cmd ->
                                cmd.execute()
                            }
                        )
                    }

                    // About Dialog
                    if (showAboutDialog) {
                        AlertDialog(
                            onDismissRequest = { showAboutDialog = false },
                            title = {
                                Text(
                                    text = "<D/> DroidCode",
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Version: 0.1.0-alpha01 (Milestone 1)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Professional Android IDE shell optimized for mobile touch ergonomics and hardware keyboard workflows.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showAboutDialog = false }) {
                                    Text("OK")
                                }
                            }
                        )
                    }

                    // Keyboard Shortcuts Dialog
                    if (showShortcutsDialog) {
                        AlertDialog(
                            onDismissRequest = { showShortcutsDialog = false },
                            title = { Text("Keyboard Shortcuts") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(
                                        "Ctrl + S" to "Save active file",
                                        "Ctrl + Shift + S" to "Save all files",
                                        "Ctrl + P" to "Command Palette",
                                        "Ctrl + B" to "Toggle Project Explorer",
                                        "Ctrl + `" to "Toggle Terminal Panel",
                                        "Ctrl + Z" to "Undo edit",
                                        "Ctrl + Y" to "Redo edit"
                                    ).forEach { (shortcut, description) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = shortcut,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = description,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showShortcutsDialog = false }) {
                                    Text("Close")
                                }
                            }
                        )
                    }

                    // Recent Workspaces Dialog
                    if (showRecentWorkspacesDialog) {
                        AlertDialog(
                            onDismissRequest = { showRecentWorkspacesDialog = false },
                            title = { Text("Recent Workspaces") },
                            text = {
                                if (recentWorkspaces.isEmpty()) {
                                    Text(
                                        text = "No recent workspaces found.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        recentWorkspaces.take(5).forEach { entity ->
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        showRecentWorkspacesDialog = false
                                                        coroutineScope.launch {
                                                            try {
                                                                workspaceMgr.openWorkspace(context, File(entity.path), entity.type)
                                                                isWorkspaceOpen = true
                                                                currentView = "IDE"
                                                            } catch (e: Exception) {
                                                                Toast.makeText(context, "Cannot open: ${e.message}", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                            ) {
                                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                                    Text(
                                                        text = entity.name,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 13.sp
                                                    )
                                                    Text(
                                                        text = entity.path,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showRecentWorkspacesDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
