package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.Command
import com.example.core.CommandRegistry
import com.example.editor.EditorManager
import com.example.project.WorkspaceManager
import com.example.settings.SettingsManager
import com.example.ui.theme.DroidCodeTheme
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

    var isWorkspaceOpen by remember { mutableStateOf(workspaceMgr.hasOpenWorkspace()) }
    var currentView by remember { mutableStateOf(if (isWorkspaceOpen) "IDE" else "HOME") }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    var showBottomPanel by remember { mutableStateOf(true) }
    var showCommandPalette by remember { mutableStateOf(false) }

    // Quick Key Bar sticky modifiers
    var ctrlActive by remember { mutableStateOf(false) }
    var shiftActive by remember { mutableStateOf(false) }
    var altActive by remember { mutableStateOf(false) }

    // Register Commands in CommandRegistry
    remember {
        commandRegistry.registerCommand(Command("file.save", "Save Active File", "File", "Ctrl+S") {
            try { editorMgr.saveActiveTab() } catch (e: Exception) {}
        })
        commandRegistry.registerCommand(Command("file.save_all", "Save All Files", "File", "Ctrl+Shift+S") {
            try { editorMgr.saveAllTabs() } catch (e: Exception) {}
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
        commandRegistry.registerCommand(Command("workspace.close", "Close Workspace", "Workspace", null) {
            workspaceMgr.closeWorkspace()
            editorMgr.closeAllTabs()
            isWorkspaceOpen = false
            currentView = "HOME"
        })
        commandRegistry.registerCommand(Command("settings.open", "Open Settings", "Preferences", null) {
            currentView = "SETTINGS"
        })
    }

    val triggerActionKey: (String) -> Unit = { action ->
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

    val insertText: (String) -> Unit = { text ->
        val tab = editorMgr.activeTab
        if (tab != null) {
            // Check modifier shortcut triggers
            if (ctrlActive) {
                when (text.lowercase()) {
                    "s" -> {
                        try { editorMgr.saveActiveTab() } catch (e: Exception) {}
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
                                try { editorMgr.openFile(file) } catch (e: Exception) {}
                                coroutineScope.launch { drawerState.close() }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        ) {
            Scaffold(
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
                                .border(1.dp, MaterialTheme.colorScheme.outline)
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
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
                                        .padding(end = 12.dp)
                                )

                                if (isWorkspaceOpen && workspaceMgr.currentProject != null) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = workspaceMgr.currentProject.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            Row {
                                IconButton(
                                    onClick = { showCommandPalette = true },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("top_bar_command_palette_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Command Palette",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (isWorkspaceOpen) {
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = "Toggle Project Explorer",
                                            tint = if (drawerState.isOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = { showBottomPanel = !showBottomPanel },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Terminal,
                                            contentDescription = "Toggle Terminal",
                                            tint = if (showBottomPanel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            workspaceMgr.closeWorkspace()
                                            editorMgr.closeAllTabs()
                                            isWorkspaceOpen = false
                                            currentView = "HOME"
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close Workspace",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { currentView = "SETTINGS" },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("top_bar_settings_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
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
                            HomeView(
                                onOpenWorkspace = { path, type ->
                                    workspaceMgr.openWorkspace(context, File(path), type)
                                    isWorkspaceOpen = true
                                    currentView = "IDE"
                                },
                                onOpenSettings = { currentView = "SETTINGS" }
                            )
                        }

                        "SETTINGS" -> {
                            SettingsView(
                                onBack = {
                                    if (isWorkspaceOpen) currentView = "IDE" else currentView = "HOME"
                                },
                                onSettingsChanged = {
                                    settingsState = settingsMgr.getSettingsCopy()
                                }
                            )
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
                }
            }
        }
    }
}
