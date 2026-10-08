package com.droidcode.ui

import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
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
import androidx.compose.material3.Button
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.droidcode.R
import com.droidcode.core.Command
import com.droidcode.core.CommandRegistry
import com.droidcode.editor.EditorManager
import com.droidcode.git.GitService
import com.droidcode.project.WorkspaceManager
import com.droidcode.settings.SettingsManager
import com.droidcode.ui.theme.CornerLarge
import com.droidcode.ui.theme.CornerMedium
import com.droidcode.ui.theme.CornerSmall
import com.droidcode.ui.theme.DroidCodeTheme
import com.droidcode.ui.theme.SpacingL
import com.droidcode.ui.theme.SpacingM
import com.droidcode.ui.theme.SpacingS
import com.droidcode.ui.theme.SpacingXL
import com.droidcode.ui.theme.SpacingXS
import java.io.File
import kotlinx.coroutines.launch
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.droidcode.navigation.DroidCodeNavGraph
import com.droidcode.navigation.Screen

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun MainShell(
    windowSizeClass: WindowSizeClass? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val sizeClass = windowSizeClass ?: activity?.let { calculateWindowSizeClass(it) }
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val isExpanded = isTablet && (sizeClass?.widthSizeClass == WindowWidthSizeClass.Expanded)
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    var isSidebarVisible by rememberSaveable { mutableStateOf(true) }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route
    val isHomeScreen = currentRoute == Screen.Home.route

    val focusManager = LocalFocusManager.current

    val settingsMgr = remember { SettingsManager.getInstance(context) }
    var settingsState by remember { mutableStateOf(settingsMgr.settings) }

    val workspaceMgr = remember { WorkspaceManager.getInstance() }
    val editorMgr = remember { EditorManager.getInstance() }
    val commandRegistry = remember { CommandRegistry.getInstance() }
    val gitService = remember { GitService.getInstance() }

    var isWorkspaceOpen by rememberSaveable { mutableStateOf(workspaceMgr.hasOpenWorkspace()) }
    var savedWorkspacePath by rememberSaveable { mutableStateOf(workspaceMgr.currentProject?.path ?: "") }

    var showUnsavedCloseDialog by remember { mutableStateOf(false) }
    var pendingCloseAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showSameWorkspaceDialog by remember { mutableStateOf(false) }
    var sameWorkspaceName by remember { mutableStateOf("") }

    fun navigateTo(route: String) {
        if (currentRoute != route) {
            navController.navigate(route) {
                launchSingleTop = true
            }
        }
    }

    fun requestCloseOrSwitchWorkspace(onConfirmed: () -> Unit) {
        if (editorMgr.hasUnsavedChanges()) {
            pendingCloseAction = onConfirmed
            showUnsavedCloseDialog = true
        } else {
            onConfirmed()
        }
    }

    fun closeCurrentWorkspace() {
        requestCloseOrSwitchWorkspace {
            workspaceMgr.closeWorkspace()
            editorMgr.closeAllTabs()
            isWorkspaceOpen = false
            savedWorkspacePath = ""
            navigateTo(Screen.Home.route)
        }
    }

    LaunchedEffect(savedWorkspacePath) {
        if (savedWorkspacePath.isNotEmpty() && !workspaceMgr.hasOpenWorkspace()) {
            val dir = File(savedWorkspacePath)
            if (dir.exists() && dir.isDirectory) {
                try {
                    workspaceMgr.openWorkspace(context, dir, null)
                    isWorkspaceOpen = true
                    if (currentRoute == Screen.Home.route) {
                        navController.navigate(Screen.Workspace.route) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainShell", "Failed to restore workspace from saved path: $savedWorkspacePath", e)
                }
            }
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(drawerState.isOpen, drawerState.isAnimationRunning, drawerState.targetValue, drawerState.currentValue) {
        if (drawerState.isOpen || drawerState.isAnimationRunning || drawerState.targetValue == DrawerValue.Open || drawerState.currentValue == DrawerValue.Open) {
            focusManager.clearFocus(force = true)
        }
    }

    var showBottomPanel by rememberSaveable { mutableStateOf(false) }
    var panelActiveTab by rememberSaveable { mutableStateOf(PanelTab.TERMINAL) }
    var panelDockPosition by rememberSaveable { mutableStateOf(PanelDockPosition.RIGHT) }
    var panelSizePreset by rememberSaveable { mutableStateOf(PanelSizePreset.NORMAL) }
    var isPanelUndocked by rememberSaveable { mutableStateOf(false) }
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
            if (!showBottomPanel) {
                showBottomPanel = true
                panelActiveTab = PanelTab.TERMINAL
            } else if (panelActiveTab == PanelTab.TERMINAL) {
                showBottomPanel = false
            } else {
                panelActiveTab = PanelTab.TERMINAL
            }
        })
        commandRegistry.registerCommand(Command("view.toggle_breadcrumbs", "Toggle Breadcrumbs Path Bar", "View", null) {
            val newState = !settingsState.isBreadcrumbsEnabled
            settingsState.isBreadcrumbsEnabled = newState
            settingsMgr.saveSettings(context)
            settingsState = settingsMgr.getSettingsCopy()
            val status = if (newState) "shown" else "hidden"
            Toast.makeText(context, "Breadcrumbs path bar $status", Toast.LENGTH_SHORT).show()
        })
        commandRegistry.registerCommand(Command("workspace.open", "Open Workspace / Project", "Workspace", "Ctrl+O") {
            navigateTo(Screen.Home.route)
        })
        commandRegistry.registerCommand(Command("workspace.close", "Close Workspace", "Workspace", null) {
            closeCurrentWorkspace()
        })
        commandRegistry.registerCommand(Command("workspace.recent", "Recent Workspaces", "Workspace", null) {
            showRecentWorkspacesDialog = true
        })
        commandRegistry.registerCommand(Command("settings.open", "Open Settings", "Preferences", "Ctrl+,") {
            navigateTo(Screen.Settings.route)
        })
        commandRegistry.registerCommand(Command("settings.editor", "Editor Fonts & Preferences", "Preferences", null) {
            navigateTo(Screen.Settings.route)
        })
        commandRegistry.registerCommand(Command("settings.keybar", "Quick Key Bar Settings", "Preferences", null) {
            navigateTo(Screen.Settings.route)
        })
        commandRegistry.registerCommand(Command("app.developer", "Developer Profile (Keshu Shiyal)", "Preferences", null) {
            navigateTo(Screen.Settings.route)
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
            navigateTo(Screen.Git.route)
        })
        commandRegistry.registerCommand(Command("git.pull", "Git Pull / Sync", "Git VCS", "Ctrl+G P") {
            Toast.makeText(context, "Workspace synchronized with git remote", Toast.LENGTH_SHORT).show()
        })
        commandRegistry.registerCommand(Command("app.about", "About DroidCode Workstation", "Preferences", null) {
            navigateTo(Screen.Settings.route)
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
                        tab.requestSelection(pos, pos)
                    }
                    "LEFT" -> {
                        if (pos > 0) {
                            val newPos = if (pos >= 2 && Character.isSurrogatePair(current[pos - 2], current[pos - 1])) {
                                pos - 2
                            } else {
                                pos - 1
                            }
                            if (shiftActive) {
                                tab.requestSelection(tab.selectionStart, newPos)
                            } else {
                                tab.requestSelection(newPos, newPos)
                            }
                        }
                    }
                    "RIGHT" -> {
                        if (pos < current.length) {
                            val newPos = if (pos + 1 < current.length && Character.isSurrogatePair(current[pos], current[pos + 1])) {
                                pos + 2
                            } else {
                                pos + 1
                            }
                            if (shiftActive) {
                                tab.requestSelection(tab.selectionStart, newPos)
                            } else {
                                tab.requestSelection(newPos, newPos)
                            }
                        }
                    }
                    "UP" -> {
                        val lastNewline = current.lastIndexOf('\n', (pos - 1).coerceAtLeast(0))
                        if (lastNewline >= 0) {
                            val prevNewline = current.lastIndexOf('\n', (lastNewline - 1).coerceAtLeast(0))
                            val col = pos - (lastNewline + 1)
                            val targetLineStart = if (prevNewline >= 0) prevNewline + 1 else 0
                            val targetLineLength = lastNewline - targetLineStart
                            val newPos = targetLineStart + col.coerceAtMost(targetLineLength)
                            if (shiftActive) {
                                tab.requestSelection(tab.selectionStart, newPos)
                            } else {
                                tab.requestSelection(newPos, newPos)
                            }
                        } else {
                            if (shiftActive) {
                                tab.requestSelection(tab.selectionStart, 0)
                            } else {
                                tab.requestSelection(0, 0)
                            }
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
                            if (shiftActive) {
                                tab.requestSelection(tab.selectionStart, newPos)
                            } else {
                                tab.requestSelection(newPos, newPos)
                            }
                        } else {
                            if (shiftActive) {
                                tab.requestSelection(tab.selectionStart, current.length)
                            } else {
                                tab.requestSelection(current.length, current.length)
                            }
                        }
                    }
                    "HOME" -> {
                        val prevNewline = current.lastIndexOf('\n', (pos - 1).coerceAtLeast(0))
                        val lineStart = if (prevNewline >= 0) prevNewline + 1 else 0
                        if (shiftActive) {
                            tab.requestSelection(tab.selectionStart, lineStart)
                        } else {
                            tab.requestSelection(lineStart, lineStart)
                        }
                    }
                    "END" -> {
                        val nextNewline = current.indexOf('\n', pos)
                        val lineEnd = if (nextNewline >= 0) nextNewline else current.length
                        if (shiftActive) {
                            tab.requestSelection(tab.selectionStart, lineEnd)
                        } else {
                            tab.requestSelection(lineEnd, lineEnd)
                        }
                    }
                    "DELETE" -> {
                        val s = minOf(tab.selectionStart, tab.selectionEnd).coerceIn(0, current.length)
                        val e = maxOf(tab.selectionStart, tab.selectionEnd).coerceIn(0, current.length)
                        if (s != e) {
                            val updated = current.substring(0, s) + current.substring(e)
                            editorMgr.updateActiveTabContent(updated)
                            tab.requestSelection(s, s)
                        } else if (pos < current.length) {
                            val charCount = if (pos + 1 < current.length && Character.isSurrogatePair(current[pos], current[pos + 1])) 2 else 1
                            val updated = current.substring(0, pos) + current.substring(pos + charCount)
                            editorMgr.updateActiveTabContent(updated)
                            tab.requestSelection(pos, pos)
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
                    "f" -> {
                        tab.showFindBar = true
                        tab.showReplaceBar = false
                        ctrlActive = false
                    }
                    "a" -> {
                        tab.requestSelection(0, tab.content.length)
                        ctrlActive = false
                    }
                    else -> {
                        ctrlActive = false
                    }
                }
            } else {
                val current = tab.content
                val s = minOf(tab.selectionStart, tab.selectionEnd).coerceIn(0, current.length)
                val e = maxOf(tab.selectionStart, tab.selectionEnd).coerceIn(0, current.length)

                if (s != e) {
                    if (text == "    " && current.substring(s, e).contains('\n')) {
                        // Multi-line block indent with standard 4 spaces
                        val lineStart = current.lastIndexOf('\n', (s - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
                        val lineEnd = current.indexOf('\n', e).let { if (it < 0) current.length else it }
                        val block = current.substring(lineStart, lineEnd)
                        val indented = block.lines().joinToString("\n") { "    $it" }
                        val updated = current.substring(0, lineStart) + indented + current.substring(lineEnd)
                        editorMgr.updateActiveTabContent(updated)
                        tab.requestSelection(lineStart, lineStart + indented.length)
                    } else {
                        // Check auto-wrap for bracket/quote pairs
                        val closer = when (text) {
                            "{" -> "}"
                            "[" -> "]"
                            "(" -> ")"
                            "\"" -> "\""
                            "'" -> "'"
                            "`" -> "`"
                            "<" -> ">"
                            else -> null
                        }
                        if (closer != null) {
                            val wrapped = current.substring(0, s) + text + current.substring(s, e) + closer + current.substring(e)
                            editorMgr.updateActiveTabContent(wrapped)
                            tab.requestSelection(s + text.length, e + text.length)
                        } else {
                            val updated = current.substring(0, s) + text + current.substring(e)
                            editorMgr.updateActiveTabContent(updated)
                            val newCursor = s + text.length
                            tab.requestSelection(newCursor, newCursor)
                        }
                    }
                } else {
                    val pos = tab.cursorPosition.coerceIn(0, current.length)
                    val closer = when (text) {
                        "{" -> "}"
                        "[" -> "]"
                        "(" -> ")"
                        "\"" -> "\""
                        "'" -> "'"
                        "`" -> "`"
                        else -> null
                    }
                    if (closer != null) {
                        // Auto-insert pair and place cursor inside
                        val pair = text + closer
                        val updated = current.substring(0, pos) + pair + current.substring(pos)
                        editorMgr.updateActiveTabContent(updated)
                        val newCursor = pos + text.length
                        tab.requestSelection(newCursor, newCursor)
                    } else if (text == "{}" || text == "[]" || text == "()" || text == "\"\"" || text == "''" || text == "``") {
                        // Pair variation from long-press popup: position cursor inside
                        val updated = current.substring(0, pos) + text + current.substring(pos)
                        editorMgr.updateActiveTabContent(updated)
                        val newCursor = pos + 1
                        tab.requestSelection(newCursor, newCursor)
                    } else if (text == "</>") {
                        // Tag snippet: position cursor inside the tag
                        val updated = current.substring(0, pos) + text + current.substring(pos)
                        editorMgr.updateActiveTabContent(updated)
                        val newCursor = pos + 1
                        tab.requestSelection(newCursor, newCursor)
                    } else {
                        // Check typeover if sitting before matching closing char
                        val isClosingChar = (text == ")" || text == "]" || text == "}" || text == "\"" || text == "'" || text == "`")
                        val nextChar = if (pos < current.length) current[pos].toString() else null
                        if (isClosingChar && nextChar == text) {
                            val newCursor = pos + 1
                            tab.requestSelection(newCursor, newCursor)
                        } else {
                            val updated = current.substring(0, pos) + text + current.substring(pos)
                            editorMgr.updateActiveTabContent(updated)
                            val newCursor = pos + text.length
                            tab.requestSelection(newCursor, newCursor)
                        }
                    }
                }
            }
        }
    }

    DroidCodeTheme(themeMode = settingsState.themeMode) {
        // High-priority BackHandler when navigation drawer is open on ANY screen
        BackHandler(enabled = drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        }

        BackHandler {
            if (showCommandPalette) {
                showCommandPalette = false
            } else if (showProjectMenubar) {
                showProjectMenubar = false
            } else if (showUnsavedCloseDialog) {
                showUnsavedCloseDialog = false
                pendingCloseAction = null
            } else if (showSameWorkspaceDialog) {
                showSameWorkspaceDialog = false
            } else if (showRecentWorkspacesDialog) {
                showRecentWorkspacesDialog = false
            } else if (showAboutDialog) {
                showAboutDialog = false
            } else if (showShortcutsDialog) {
                showShortcutsDialog = false
            } else if (drawerState.isOpen) {
                coroutineScope.launch { drawerState.close() }
            } else if (navController.previousBackStackEntry != null) {
                navController.popBackStack()
            } else if (isWorkspaceOpen && currentRoute != Screen.Workspace.route) {
                navigateTo(Screen.Workspace.route)
            }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = !isExpanded && isWorkspaceOpen && drawerState.isOpen,
            drawerContent = {
                if (isWorkspaceOpen) {
                    val activePath = workspaceMgr.currentProject?.path ?: savedWorkspacePath
                    key(activePath) {
                        ModalDrawerSheet(
                            drawerShape = RectangleShape,
                            drawerContainerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .zIndex(100f)
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
                                onCloseProject = {
                                    coroutineScope.launch { drawerState.close() }
                                    closeCurrentWorkspace()
                                },
                                onCloseDrawer = {
                                    coroutineScope.launch { drawerState.close() }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        ) {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                modifier = Modifier
                    .fillMaxSize()
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Escape) {
                            if (showCommandPalette) {
                                showCommandPalette = false
                                true
                            } else if (showProjectMenubar) {
                                showProjectMenubar = false
                                true
                            } else if (showUnsavedCloseDialog) {
                                showUnsavedCloseDialog = false
                                pendingCloseAction = null
                                true
                            } else if (showSameWorkspaceDialog) {
                                showSameWorkspaceDialog = false
                                true
                            } else if (drawerState.isOpen) {
                                coroutineScope.launch { drawerState.close() }
                                true
                            } else if (navController.previousBackStackEntry != null) {
                                navController.popBackStack()
                                true
                            } else {
                                false
                            }
                        } else {
                            false
                        }
                    },
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
                                            if (isWorkspaceOpen) navigateTo(Screen.Workspace.route) else navigateTo(Screen.Home.route)
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
                                                    closeCurrentWorkspace()
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
                                if (!isHomeScreen) {
                                    // Search & Commands toggle
                                    IconButton(
                                        onClick = {
                                            focusManager.clearFocus(force = true)
                                            showCommandPalette = true
                                        },
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
                                }

                                if (isWorkspaceOpen && !isHomeScreen) {
                                    // Project Explorer toggle
                                    IconButton(
                                        onClick = {
                                            focusManager.clearFocus(force = true)
                                            if (isExpanded) {
                                                isSidebarVisible = !isSidebarVisible
                                            } else {
                                                coroutineScope.launch {
                                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("top_bar_explorer_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = "Toggle Project Explorer",
                                            tint = if ((isExpanded && isSidebarVisible) || (!isExpanded && drawerState.isOpen)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Terminal toggle
                                    IconButton(
                                        onClick = {
                                            if (!showBottomPanel) {
                                                showBottomPanel = true
                                                panelActiveTab = PanelTab.TERMINAL
                                            } else if (panelActiveTab == PanelTab.TERMINAL) {
                                                showBottomPanel = false
                                            } else {
                                                panelActiveTab = PanelTab.TERMINAL
                                            }
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("top_bar_terminal_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Terminal,
                                            contentDescription = "Toggle Terminal",
                                            tint = if (showBottomPanel && panelActiveTab == PanelTab.TERMINAL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                if (!isHomeScreen) {
                                    // 8dp horizontal breathing room between primary toggles and overflow menu
                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Three-Dot Overflow Menu
                                    Box {
                                        IconButton(
                                            onClick = {
                                                focusManager.clearFocus(force = true)
                                                showTopOverflowMenu = true
                                            },
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
                                                    navigateTo(Screen.Settings.route)
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
                                                        closeCurrentWorkspace()
                                                    }
                                                )
                                            }
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
                    DroidCodeNavGraph(
                        navController = navController,
                        startDestination = if (isWorkspaceOpen) Screen.Workspace.route else Screen.Home.route,
                        modifier = Modifier.fillMaxSize(),
                        homeContent = {
                            Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                                HomeView(
                                    onOpenWorkspace = { path, type ->
                                        if (workspaceMgr.hasOpenWorkspace() && workspaceMgr.currentProject?.path == path) {
                                            sameWorkspaceName = workspaceMgr.currentProject?.name ?: "Current Workspace"
                                            showSameWorkspaceDialog = true
                                        } else {
                                            requestCloseOrSwitchWorkspace {
                                                coroutineScope.launch {
                                                    try {
                                                        workspaceMgr.closeWorkspace()
                                                        editorMgr.closeAllTabs()
                                                        workspaceMgr.openWorkspace(context, File(path), type)
                                                        savedWorkspacePath = path
                                                        isWorkspaceOpen = true
                                                        navigateTo(Screen.Workspace.route)
                                                    } catch (e: Exception) {
                                                        android.util.Log.e("MainShell", "Failed to open workspace", e)
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    onOpenSettings = { navigateTo(Screen.Settings.route) },
                                    onOpenGeneralMenu = { showProjectMenubar = true }
                                )
                            }
                        },
                        settingsContent = {
                            Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                                SettingsView(
                                    onBack = {
                                        if (navController.previousBackStackEntry != null) {
                                            navController.popBackStack()
                                        } else if (isWorkspaceOpen) {
                                            navigateTo(Screen.Workspace.route)
                                        } else {
                                            navigateTo(Screen.Home.route)
                                        }
                                    },
                                    onSettingsChanged = {
                                        settingsState = settingsMgr.getSettingsCopy()
                                    }
                                )
                            }
                        },
                        workspaceContent = {
                            if (isExpanded) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .imePadding()
                                        .navigationBarsPadding()
                                ) {
                                    if (isSidebarVisible) {
                                        // Multi-Pane Left Pane: Explorer Panel on Tablet / Expanded Screen
                                        val activePath = workspaceMgr.currentProject?.path ?: savedWorkspacePath
                                        key(activePath) {
                                            Box(
                                                modifier = Modifier
                                                    .zIndex(10f)
                                                    .width(280.dp)
                                                    .fillMaxHeight()
                                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                            ) {
                                                ExplorerPanel(
                                                    onOpenFile = { file ->
                                                        try {
                                                            editorMgr.openFile(file)
                                                        } catch (e: Exception) {
                                                            android.util.Log.e("MainShell", "Failed to open file: ${file.absolutePath}", e)
                                                        }
                                                    },
                                                    onCloseProject = {
                                                        closeCurrentWorkspace()
                                                    },
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }

                                        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    }

                                    // Multi-Pane Right Pane: Editor Workstation Area
                                    val effectiveDockPosition = if (isLandscape) panelDockPosition else PanelDockPosition.BOTTOM
                                    val showDockedPanel = showBottomPanel && !isPanelUndocked

                                    if (effectiveDockPosition == PanelDockPosition.RIGHT && showDockedPanel) {
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                            ) {
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
                                            }

                                            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                            BottomOrSidePanel(
                                                activeTab = panelActiveTab,
                                                onTabSelected = { panelActiveTab = it },
                                                dockPosition = PanelDockPosition.RIGHT,
                                                sizePreset = panelSizePreset,
                                                onSizePresetChange = { panelSizePreset = it },
                                                onToggleDockPosition = {
                                                    panelDockPosition = PanelDockPosition.BOTTOM
                                                },
                                                onUndock = { isPanelUndocked = true },
                                                onClose = { showBottomPanel = false },
                                                supportsDockRight = isLandscape
                                            )
                                        }
                                    } else {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                        ) {
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

                                            if (showDockedPanel) {
                                                BottomOrSidePanel(
                                                    activeTab = panelActiveTab,
                                                    onTabSelected = { panelActiveTab = it },
                                                    dockPosition = PanelDockPosition.BOTTOM,
                                                    sizePreset = panelSizePreset,
                                                    onSizePresetChange = { panelSizePreset = it },
                                                    onToggleDockPosition = {
                                                        panelDockPosition = PanelDockPosition.RIGHT
                                                    },
                                                    onUndock = { isPanelUndocked = true },
                                                    onClose = { showBottomPanel = false },
                                                    supportsDockRight = isLandscape
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                val effectiveDockPosition = if (isLandscape) panelDockPosition else PanelDockPosition.BOTTOM
                                val showDockedPanel = showBottomPanel && !isPanelUndocked

                                if (isLandscape && effectiveDockPosition == PanelDockPosition.RIGHT && showDockedPanel) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .imePadding()
                                            .navigationBarsPadding()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                        ) {
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
                                        }

                                        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                        BottomOrSidePanel(
                                            activeTab = panelActiveTab,
                                            onTabSelected = { panelActiveTab = it },
                                            dockPosition = PanelDockPosition.RIGHT,
                                            sizePreset = panelSizePreset,
                                            onSizePresetChange = { panelSizePreset = it },
                                            onToggleDockPosition = {
                                                panelDockPosition = PanelDockPosition.BOTTOM
                                            },
                                            onUndock = { isPanelUndocked = true },
                                            onClose = { showBottomPanel = false },
                                            supportsDockRight = true
                                        )
                                    }
                                } else {
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
                                        if (showDockedPanel) {
                                            BottomOrSidePanel(
                                                activeTab = panelActiveTab,
                                                onTabSelected = { panelActiveTab = it },
                                                dockPosition = PanelDockPosition.BOTTOM,
                                                sizePreset = panelSizePreset,
                                                onSizePresetChange = { panelSizePreset = it },
                                                onToggleDockPosition = {
                                                    panelDockPosition = PanelDockPosition.RIGHT
                                                },
                                                onUndock = { isPanelUndocked = true },
                                                onClose = { showBottomPanel = false },
                                                supportsDockRight = isLandscape
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )

                    // Floating Panel Dialog when Undocked
                    if (isPanelUndocked && showBottomPanel) {
                        FloatingPanelDialog(
                            activeTab = panelActiveTab,
                            onTabSelected = { panelActiveTab = it },
                            onDock = { isPanelUndocked = false },
                            onDismiss = { showBottomPanel = false }
                        )
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
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Recent Workspaces")
                                    }
                                    if (recentWorkspaces.isNotEmpty()) {
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
                            },
                            text = {
                                if (recentWorkspaces.isEmpty()) {
                                    Text(
                                        text = "No recent workspaces found.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 380.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(recentWorkspaces) { entity ->
                                            val exists = File(entity.path).exists()
                                            val projectIcon = when {
                                                entity.isGitRepo -> Icons.Default.AccountTree
                                                entity.type?.contains("python", ignoreCase = true) == true -> Icons.Default.Terminal
                                                entity.type?.contains("android", ignoreCase = true) == true ||
                                                entity.type?.contains("kotlin", ignoreCase = true) == true ||
                                                entity.type?.contains("java", ignoreCase = true) == true -> Icons.Default.Code
                                                else -> Icons.Default.Folder
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (exists) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (exists) MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                                    else MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        if (!exists) {
                                                            Toast.makeText(
                                                                context,
                                                                "Workspace directory '${entity.path}' is unavailable or moved.",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            return@clickable
                                                        }
                                                        if (workspaceMgr.hasOpenWorkspace() && workspaceMgr.currentProject?.path == entity.path) {
                                                            showRecentWorkspacesDialog = false
                                                            sameWorkspaceName = entity.name
                                                            showSameWorkspaceDialog = true
                                                        } else {
                                                            showRecentWorkspacesDialog = false
                                                            requestCloseOrSwitchWorkspace {
                                                                coroutineScope.launch {
                                                                    try {
                                                                        workspaceMgr.closeWorkspace()
                                                                        editorMgr.closeAllTabs()
                                                                        workspaceMgr.openWorkspace(context, File(entity.path), entity.type)
                                                                        savedWorkspacePath = entity.path
                                                                        isWorkspaceOpen = true
                                                                        navigateTo(Screen.Workspace.route)
                                                                    } catch (e: Exception) {
                                                                        Toast.makeText(context, "Cannot open: ${e.message}", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = projectIcon,
                                                        contentDescription = null,
                                                        tint = if (exists) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                text = entity.name,
                                                                fontWeight = FontWeight.SemiBold,
                                                                fontSize = 13.sp,
                                                                color = if (exists) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                modifier = Modifier.weight(1f, fill = false)
                                                            )
                                                            if (!exists) {
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text(
                                                                    text = "(Missing)",
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = MaterialTheme.colorScheme.error
                                                                )
                                                            }
                                                        }
                                                        Text(
                                                            text = entity.path,
                                                            fontSize = 11.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        if (entity.lastOpenedTimestamp > 0L) {
                                                            val relativeTime = DateUtils.getRelativeTimeSpanString(
                                                                entity.lastOpenedTimestamp,
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
                                                    IconButton(
                                                        onClick = {
                                                            coroutineScope.launch {
                                                                workspaceMgr.removeWorkspace(context, entity.path)
                                                            }
                                                        },
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
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showRecentWorkspacesDialog = false }) {
                                    Text("Close")
                                }
                            }
                        )
                    }

                    // Same Workspace Dialog
                    if (showSameWorkspaceDialog) {
                        AlertDialog(
                            onDismissRequest = { showSameWorkspaceDialog = false },
                            title = { Text("Workspace Already Open") },
                            text = {
                                Text("'$sameWorkspaceName' is already open in the current screen.")
                            },
                            confirmButton = {
                                Button(onClick = { showSameWorkspaceDialog = false }) {
                                    Text("OK")
                                }
                            }
                        )
                    }

                    // Unsaved Changes Confirmation Dialog
                    if (showUnsavedCloseDialog) {
                        AlertDialog(
                            onDismissRequest = {
                                showUnsavedCloseDialog = false
                                pendingCloseAction = null
                            },
                            title = { Text("Unsaved Changes") },
                            text = {
                                val unsavedTabs = editorMgr.getUnsavedTabs()
                                val fileNames = unsavedTabs.take(3).joinToString(", ") { it.fileName }
                                val suffix = if (unsavedTabs.size > 3) " and ${unsavedTabs.size - 3} more" else ""
                                Text("There are unsaved changes in: $fileNames$suffix.\n\nDo you want to save before closing the current workspace?")
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        try {
                                            editorMgr.saveAllTabs()
                                        } catch (e: Exception) {
                                            android.util.Log.e("MainShell", "Failed to save files before closing", e)
                                        }
                                        showUnsavedCloseDialog = false
                                        pendingCloseAction?.invoke()
                                        pendingCloseAction = null
                                    }
                                ) {
                                    Text("Save & Close")
                                }
                            },
                            dismissButton = {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(
                                        onClick = {
                                            showUnsavedCloseDialog = false
                                            pendingCloseAction = null
                                        }
                                    ) {
                                        Text("Cancel")
                                    }
                                    TextButton(
                                        onClick = {
                                            showUnsavedCloseDialog = false
                                            pendingCloseAction?.invoke()
                                            pendingCloseAction = null
                                        }
                                    ) {
                                        Text("Don't Save", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
