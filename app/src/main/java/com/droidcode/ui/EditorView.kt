package com.droidcode.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.webkit.MimeTypeMap
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.ScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import com.droidcode.R
import com.droidcode.ui.theme.CornerSmall
import com.droidcode.ui.theme.SpacingM
import com.droidcode.ui.theme.SpacingS
import com.droidcode.ui.theme.SpacingXS
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.droidcode.editor.EditorManager
import com.droidcode.editor.EditorTab
import com.droidcode.editor.FileViewerType
import com.droidcode.editor.engine.DroidCodeEditor
import com.droidcode.settings.AppSettings
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.zIndex
import com.droidcode.editor.LineDiffCalculator
import com.droidcode.editor.LineDiffStatus
import com.droidcode.project.WorkspaceManager

enum class EditorSplitMode {
    NONE,
    HORIZONTAL,
    VERTICAL
}

@Composable
fun EditorView(
    settings: AppSettings,
    ctrlActive: Boolean = false,
    shiftActive: Boolean = false,
    altActive: Boolean = false,
    onResetModifiers: () -> Unit = {},
    onOpenCommandPalette: () -> Unit = {},
    onOpenGeneralMenu: (() -> Unit)? = null,
    onSaveRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val editorMgr = remember { EditorManager.getInstance() }
    val workspaceMgr = remember { WorkspaceManager.getInstance() }
    val currentProject by workspaceMgr.currentProjectFlow.collectAsState()
    val tabs = editorMgr.tabs
    val activeTab = editorMgr.activeTab

    var savedOpenPaths by rememberSaveable {
        mutableStateOf(ArrayList(editorMgr.tabs.map { it.filePath }))
    }
    var savedActiveTabIndex by rememberSaveable {
        mutableStateOf(editorMgr.activeTabIndex)
    }

    LaunchedEffect(Unit) {
        if (editorMgr.tabs.isEmpty() && savedOpenPaths.isNotEmpty()) {
            for (path in savedOpenPaths) {
                val f = File(path)
                if (f.exists() && f.isFile) {
                    try {
                        editorMgr.openFile(f)
                    } catch (e: Exception) {
                        android.util.Log.e("EditorView", "Failed to restore tab for $path", e)
                    }
                }
            }
            if (savedActiveTabIndex in 0 until editorMgr.tabs.size) {
                editorMgr.activeTabIndex = savedActiveTabIndex
            }
        }
    }

    LaunchedEffect(tabs.size, editorMgr.activeTabIndex) {
        savedOpenPaths = ArrayList(tabs.map { it.filePath })
        savedActiveTabIndex = editorMgr.activeTabIndex
    }

    var tabToPromptCloseIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    var showContextMenu by rememberSaveable { mutableStateOf(false) }
    var showGoToLineDialog by rememberSaveable { mutableStateOf(false) }
    var showChangeLanguageDialog by rememberSaveable { mutableStateOf(false) }
    var showChangeEncodingDialog by rememberSaveable { mutableStateOf(false) }
    var showChangeLineEndingDialog by rememberSaveable { mutableStateOf(false) }
    var showSaveAsDialog by rememberSaveable { mutableStateOf(false) }
    var showWorkspaceSearchDialog by rememberSaveable { mutableStateOf(false) }
    var splitMode by rememberSaveable { mutableStateOf(EditorSplitMode.NONE) }
    var secondaryTabIndex by rememberSaveable { mutableIntStateOf(-1) }
    var infoDialogTitle by rememberSaveable { mutableStateOf<String?>(null) }
    var infoDialogText by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingActionType by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        EditorCommandRegistration.registerAllCommands(
            context = context,
            editorMgr = editorMgr,
            workspaceMgr = workspaceMgr,
            settings = settings,
            onOpenCommandPalette = onOpenCommandPalette,
            onShowGoToLineDialog = { showGoToLineDialog = true },
            onShowChangeLanguageDialog = { showChangeLanguageDialog = true },
            onShowChangeEncodingDialog = { showChangeEncodingDialog = true },
            onShowChangeLineEndingDialog = { showChangeLineEndingDialog = true },
            onShowSaveAsDialog = { showSaveAsDialog = true },
            onShowWorkspaceSearch = { showWorkspaceSearchDialog = true },
            onToggleSplitEditor = {
                splitMode = when (splitMode) {
                    EditorSplitMode.NONE -> EditorSplitMode.HORIZONTAL
                    EditorSplitMode.HORIZONTAL -> EditorSplitMode.VERTICAL
                    EditorSplitMode.VERTICAL -> EditorSplitMode.NONE
                }
            },
            onShowInfoDialog = { title, text ->
                infoDialogTitle = title
                infoDialogText = text
            },
            onOpenTerminalPanel = {
                infoDialogTitle = "Terminal Panel"
                infoDialogText = "Terminal Session Opened at ${activeTab?.filePath ?: workspaceMgr.currentProject?.path ?: "~"}\n$ "
            },
            onExecuteAction = { actionType ->
                pendingActionType = actionType
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (tabs.isNotEmpty() && activeTab != null) {
            // Top Navigation Header Bars (Tab Bar, Breadcrumbs, Editor Toolbar)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(5f)
            ) {
                // Tab Bar
                Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isActive = index == editorMgr.activeTabIndex
                    val bg = if (isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                    val tabBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(bg)
                            .border(1.dp, tabBorderColor)
                            .clickable { editorMgr.activeTabIndex = index }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = getEditorFileIcon(tab.fileName),
                            contentDescription = null,
                            tint = getEditorFileIconColor(tab.fileName),
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 4.dp)
                        )

                        Text(
                            text = tab.fileName + (if (tab.isModified) " *" else ""),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.width(SpacingS))

                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.action_close_tab),
                            tint = if (isActive) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(14.dp)
                                .clickable {
                                    val tabToClose = tabs[index]
                                    if (tabToClose.isModified) {
                                        tabToPromptCloseIndex = index
                                    } else {
                                        editorMgr.closeTab(index)
                                    }
                                }
                        )
                    }
                }
            }
        }

        // Unsaved Changes Confirmation Dialog
        if (tabToPromptCloseIndex != null && tabToPromptCloseIndex!! in tabs.indices) {
            val tabToClose = tabs[tabToPromptCloseIndex!!]
            AlertDialog(
                onDismissRequest = { tabToPromptCloseIndex = null },
                title = { Text(stringResource(R.string.unsaved_changes_title)) },
                text = { Text(stringResource(R.string.unsaved_changes_message, tabToClose.fileName)) },
                confirmButton = {
                    Button(
                        onClick = {
                            try {
                                editorMgr.saveTab(tabToClose)
                                onSaveRequested()
                            } catch (e: Exception) {
                                android.util.Log.e("EditorView", "Failed saving tab: ${tabToClose.fileName}", e)
                                Toast.makeText(context, "Failed to save ${tabToClose.fileName}: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                            editorMgr.closeTab(tabToPromptCloseIndex!!)
                            tabToPromptCloseIndex = null
                        }
                    ) { Text(stringResource(R.string.action_save)) }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = {
                                editorMgr.closeTab(tabToPromptCloseIndex!!)
                                tabToPromptCloseIndex = null
                            }
                        ) { Text(stringResource(R.string.action_dont_save)) }
                        Spacer(modifier = Modifier.width(SpacingXS))
                        TextButton(
                            onClick = { tabToPromptCloseIndex = null }
                        ) { Text(stringResource(R.string.action_cancel)) }
                    }
                }
            )
        }

        // Breadcrumbs Navigation Bar
        if (activeTab != null) {
            BreadcrumbsBar(
                tab = activeTab,
                workspaceDir = currentProject?.directory,
                onOpenFile = { file ->
                    try {
                        editorMgr.openFile(file)
                    } catch (e: Exception) {
                        android.util.Log.e("EditorView", "Failed to open file: ${file.name}", e)
                    }
                },
                onShowGoToLine = { showGoToLineDialog = true },
                onShowFind = {
                    activeTab.showFindBar = true
                    activeTab.showReplaceBar = false
                }
            )
        }

        // Editor Toolbar & Language / Line Stats
        if (activeTab != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = activeTab.languageId.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clickable { showChangeLanguageDialog = true }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = activeTab.encoding,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clickable { showChangeEncodingDialog = true }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = activeTab.lineEnding,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clickable { showChangeLineEndingDialog = true }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(SpacingM))
                    Text(
                        text = stringResource(R.string.status_line_col, activeTab.line, activeTab.column),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showWorkspaceSearchDialog = true },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("editor_find_in_files_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Find in Files (Ctrl+Shift+F)",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            splitMode = when (splitMode) {
                                EditorSplitMode.NONE -> EditorSplitMode.HORIZONTAL
                                EditorSplitMode.HORIZONTAL -> EditorSplitMode.VERTICAL
                                EditorSplitMode.VERTICAL -> EditorSplitMode.NONE
                            }
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("editor_split_mode_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Splitscreen,
                            contentDescription = "Toggle Split Editor",
                            tint = if (splitMode != EditorSplitMode.NONE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            try {
                                editorMgr.saveActiveTab()
                                onSaveRequested()
                            } catch (e: Exception) {
                                android.util.Log.e("EditorView", "Failed saving active tab", e)
                                Toast.makeText(context, "Failed to save file: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("editor_save_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save",
                            tint = if (activeTab.isModified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (onOpenGeneralMenu != null) {
                        IconButton(
                            onClick = { onOpenGeneralMenu() },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("editor_general_menu_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "General Menu",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { showContextMenu = true },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("editor_context_menu_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "IDE Context Menu",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Split-Pane or Single Editor Container
        val secondTab = if (splitMode != EditorSplitMode.NONE && tabs.isNotEmpty()) {
            if (secondaryTabIndex in tabs.indices && tabs[secondaryTabIndex] != activeTab) {
                tabs[secondaryTabIndex]
            } else {
                tabs.firstOrNull { it != activeTab } ?: activeTab
            }
        } else null

        if (splitMode == EditorSplitMode.NONE || secondTab == null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                EditorTabContent(
                    tab = activeTab,
                    settings = settings,
                    ctrlActive = ctrlActive,
                    shiftActive = shiftActive,
                    altActive = altActive,
                    pendingActionType = pendingActionType,
                    onClearPendingAction = { pendingActionType = null },
                    onResetModifiers = onResetModifiers,
                    onOpenCommandPalette = onOpenCommandPalette,
                    onOpenContextMenu = { showContextMenu = true },
                    onOpenWorkspaceSearch = { showWorkspaceSearchDialog = true },
                    onShowInfoDialog = { title, text ->
                        infoDialogTitle = title
                        infoDialogText = text
                    },
                    onSaveRequested = onSaveRequested,
                    onContentChange = { newText: String ->
                        editorMgr.updateActiveTabContent(newText)
                    },
                    onCursorChange = { pos: Int ->
                        activeTab.updateCursor(pos)
                    }
                )
            }
        } else if (splitMode == EditorSplitMode.HORIZONTAL) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    EditorTabContent(
                        tab = activeTab,
                        settings = settings,
                        ctrlActive = ctrlActive,
                        shiftActive = shiftActive,
                        altActive = altActive,
                        pendingActionType = pendingActionType,
                        onClearPendingAction = { pendingActionType = null },
                        onResetModifiers = onResetModifiers,
                        onOpenCommandPalette = onOpenCommandPalette,
                        onOpenContextMenu = { showContextMenu = true },
                        onOpenWorkspaceSearch = { showWorkspaceSearchDialog = true },
                        onShowInfoDialog = { title, text ->
                            infoDialogTitle = title
                            infoDialogText = text
                        },
                        onSaveRequested = onSaveRequested,
                        onContentChange = { newText: String ->
                            editorMgr.updateActiveTabContent(newText)
                        },
                        onCursorChange = { pos: Int ->
                            activeTab.updateCursor(pos)
                        }
                    )
                }
                VerticalDivider(
                    modifier = Modifier.width(1.dp).fillMaxHeight(),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    SecondaryEditorPane(
                        tab = secondTab,
                        allTabs = tabs,
                        settings = settings,
                        workspaceDir = currentProject?.directory,
                        onSelectTab = { selectedTab ->
                            secondaryTabIndex = tabs.indexOf(selectedTab)
                        },
                        onCloseSplit = {
                            splitMode = EditorSplitMode.NONE
                        },
                        onSaveRequested = onSaveRequested,
                        onOpenWorkspaceSearch = { showWorkspaceSearchDialog = true }
                    )
                }
            }
        } else {
            Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    EditorTabContent(
                        tab = activeTab,
                        settings = settings,
                        ctrlActive = ctrlActive,
                        shiftActive = shiftActive,
                        altActive = altActive,
                        pendingActionType = pendingActionType,
                        onClearPendingAction = { pendingActionType = null },
                        onResetModifiers = onResetModifiers,
                        onOpenCommandPalette = onOpenCommandPalette,
                        onOpenContextMenu = { showContextMenu = true },
                        onOpenWorkspaceSearch = { showWorkspaceSearchDialog = true },
                        onShowInfoDialog = { title, text ->
                            infoDialogTitle = title
                            infoDialogText = text
                        },
                        onSaveRequested = onSaveRequested,
                        onContentChange = { newText: String ->
                            editorMgr.updateActiveTabContent(newText)
                        },
                        onCursorChange = { pos: Int ->
                            activeTab.updateCursor(pos)
                        }
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.height(1.dp).fillMaxWidth(),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    SecondaryEditorPane(
                        tab = secondTab,
                        allTabs = tabs,
                        settings = settings,
                        workspaceDir = currentProject?.directory,
                        onSelectTab = { selectedTab ->
                            secondaryTabIndex = tabs.indexOf(selectedTab)
                        },
                        onCloseSplit = {
                            splitMode = EditorSplitMode.NONE
                        },
                        onSaveRequested = onSaveRequested,
                        onOpenWorkspaceSearch = { showWorkspaceSearchDialog = true }
                    )
                }
            }
        } else {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(SpacingM))
                    Text(
                        text = stringResource(R.string.no_open_files),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.no_open_files_subtitle),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = SpacingXS)
                    )
                }
            }
        }
    }

    // --- Dialogs ---
    if (showContextMenu) {
        EditorContextMenuDialog(
            onDismiss = { showContextMenu = false },
            onExecuteCommand = { cmd -> cmd.execute() }
        )
    }

    if (showGoToLineDialog && activeTab != null) {
        val totalLines = activeTab.content.split("\n").size
        GoToLineDialog(
            currentLine = activeTab.line,
            totalLines = totalLines,
            onDismiss = { showGoToLineDialog = false },
            onJumpToLine = { target ->
                pendingActionType = "GO_TO_LINE:$target"
            }
        )
    }

    if (showChangeLanguageDialog && activeTab != null) {
        ChangeLanguageDialog(
            currentLanguage = activeTab.languageId,
            onDismiss = { showChangeLanguageDialog = false },
            onLanguageSelected = { newLang ->
                activeTab.languageId = newLang
            }
        )
    }

    if (showChangeEncodingDialog && activeTab != null) {
        ChangeEncodingDialog(
            currentEncoding = activeTab.encoding,
            onDismiss = { showChangeEncodingDialog = false },
            onEncodingSelected = { enc ->
                activeTab.encoding = enc
            }
        )
    }

    if (showChangeLineEndingDialog && activeTab != null) {
        ChangeLineEndingDialog(
            currentEnding = activeTab.lineEnding,
            onDismiss = { showChangeLineEndingDialog = false },
            onEndingSelected = { ending ->
                activeTab.lineEnding = ending
            }
        )
    }

    if (showSaveAsDialog && activeTab != null) {
        SaveAsDialog(
            currentFilePath = activeTab.filePath,
            onDismiss = { showSaveAsDialog = false },
            onSaveAs = { newPath ->
                val f = File(newPath)
                f.parentFile?.mkdirs()
                f.writeText(activeTab.content)
                Toast.makeText(context, "Saved as ${f.name}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showWorkspaceSearchDialog) {
        WorkspaceSearchDialog(
            rootDirectory = workspaceMgr.currentProject?.directory,
            onDismiss = { showWorkspaceSearchDialog = false },
            onResultClick = { file, lineNum ->
                showWorkspaceSearchDialog = false
                editorMgr.openFile(file)
                pendingActionType = "GO_TO_LINE:$lineNum"
            }
        )
    }

    if (infoDialogTitle != null && infoDialogText != null) {
        InfoDetailDialog(
            title = infoDialogTitle!!,
            content = infoDialogText!!,
            onDismiss = {
                infoDialogTitle = null
                infoDialogText = null
            }
        )
    }
}

private data class BreadcrumbNode(
    val name: String,
    val file: File?,
    val isDirectory: Boolean
)

@Composable
private fun BreadcrumbsBar(
    tab: EditorTab,
    workspaceDir: File?,
    onOpenFile: (File) -> Unit,
    onShowGoToLine: (() -> Unit)? = null,
    onShowFind: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var activeDropdownIndex by remember { mutableStateOf<Int?>(null) }

    val nodes = remember(tab.filePath, workspaceDir?.absolutePath) {
        val tabFile = tab.file
        val rootDir = workspaceDir?.canonicalFile ?: workspaceDir
        val fileCanonical = try { tabFile.canonicalFile } catch (_: Exception) { tabFile.absoluteFile }

        val result = mutableListOf<BreadcrumbNode>()
        if (rootDir != null && (fileCanonical.startsWith(rootDir) || tabFile.startsWith(rootDir))) {
            result.add(BreadcrumbNode(rootDir.name.ifEmpty { "Project" }, rootDir, true))
            val rel = try {
                fileCanonical.relativeTo(rootDir).path
            } catch (_: Exception) {
                tabFile.name
            }
            val parts = rel.split(File.separatorChar, '/').filter { it.isNotEmpty() }
            var currentDir = rootDir
            for (i in 0 until parts.size - 1) {
                val seg = parts[i]
                currentDir = File(currentDir, seg)
                result.add(BreadcrumbNode(seg, currentDir, true))
            }
            result.add(BreadcrumbNode(parts.lastOrNull() ?: tab.fileName, tabFile, false))
        } else {
            val parents = mutableListOf<File>()
            var p = tabFile.parentFile
            while (p != null && parents.size < 3) {
                parents.add(0, p)
                p = p.parentFile
            }
            for (dir in parents) {
                result.add(BreadcrumbNode(dir.name.ifEmpty { "Files" }, dir, true))
            }
            if (result.isEmpty()) {
                result.add(BreadcrumbNode("Files", tabFile.parentFile, true))
            }
            result.add(BreadcrumbNode(tab.fileName, tabFile, false))
        }
        result
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp)
            .background(MaterialTheme.colorScheme.surface)
            .border(width = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        nodes.forEachIndexed { index, node ->
            if (index > 0) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
            }

            val isLast = index == nodes.size - 1
            val isExpanded = activeDropdownIndex == index

            Box {
                Row(
                    modifier = Modifier
                        .clickable { activeDropdownIndex = if (isExpanded) null else index }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (node.isDirectory) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                    } else {
                        Icon(
                            imageVector = getEditorFileIcon(node.name),
                            contentDescription = null,
                            tint = getEditorFileIconColor(node.name),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = node.name,
                        fontSize = 11.sp,
                        fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                        color = if (isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace
                    )
                    if (node.isDirectory) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "▾",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                if (isExpanded) {
                    DropdownMenu(
                        expanded = true,
                        onDismissRequest = { activeDropdownIndex = null }
                    ) {
                        if (node.isDirectory && node.file != null && node.file.isDirectory) {
                            val children = remember(node.file.path) {
                                try {
                                    node.file.listFiles()
                                        ?.filter { !it.name.startsWith(".") }
                                        ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                                        ?.take(40) ?: emptyList()
                                } catch (_: Exception) {
                                    emptyList()
                                }
                            }
                            if (children.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("(Empty folder)", fontSize = 12.sp) },
                                    onClick = { activeDropdownIndex = null }
                                )
                            } else {
                                children.forEach { child ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (child.isDirectory) {
                                                    Icon(
                                                        imageVector = Icons.Default.Folder,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = getEditorFileIcon(child.name),
                                                        contentDescription = null,
                                                        tint = getEditorFileIconColor(child.name),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = child.name,
                                                    fontSize = 12.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = if (child.absolutePath == tab.filePath) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        },
                                        onClick = {
                                            activeDropdownIndex = null
                                            if (child.isFile) {
                                                onOpenFile(child)
                                            }
                                        }
                                    )
                                }
                            }
                        } else {
                            // File Actions Dropdown
                            DropdownMenuItem(
                                text = { Text("Copy Full Path", fontSize = 12.sp) },
                                onClick = {
                                    activeDropdownIndex = null
                                    clipboardManager.setText(AnnotatedString(tab.filePath))
                                    Toast.makeText(context, "Full path copied", Toast.LENGTH_SHORT).show()
                                }
                            )
                            val rel = workspaceDir?.let { tab.file.relativeToOrNull(it)?.path }
                            if (rel != null) {
                                DropdownMenuItem(
                                    text = { Text("Copy Relative Path", fontSize = 12.sp) },
                                    onClick = {
                                        activeDropdownIndex = null
                                        clipboardManager.setText(AnnotatedString(rel))
                                        Toast.makeText(context, "Relative path copied", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                            if (onShowGoToLine != null) {
                                DropdownMenuItem(
                                    text = { Text("Go to Line...", fontSize = 12.sp) },
                                    onClick = {
                                        activeDropdownIndex = null
                                        onShowGoToLine()
                                    }
                                )
                            }
                            if (onShowFind != null) {
                                DropdownMenuItem(
                                    text = { Text("Find in File", fontSize = 12.sp) },
                                    onClick = {
                                        activeDropdownIndex = null
                                        onShowFind()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "${tab.fileName} (${tab.lineStartOffsets.size.coerceAtLeast(1)} lines)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = { activeDropdownIndex = null }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorTabContent(
    tab: EditorTab,
    settings: AppSettings,
    ctrlActive: Boolean,
    shiftActive: Boolean,
    altActive: Boolean,
    pendingActionType: String?,
    onClearPendingAction: () -> Unit,
    onResetModifiers: () -> Unit,
    onOpenCommandPalette: () -> Unit,
    onOpenContextMenu: () -> Unit,
    onOpenWorkspaceSearch: () -> Unit,
    onShowInfoDialog: (String, String) -> Unit,
    onSaveRequested: () -> Unit,
    onContentChange: (String) -> Unit,
    onCursorChange: (Int) -> Unit
) {
    val context = LocalContext.current
    when (tab.viewerType) {
        FileViewerType.IMAGE -> ImageViewer(file = tab.file)
        FileViewerType.VIDEO -> VideoViewer(file = tab.file)
        FileViewerType.PDF -> PdfViewer(file = tab.file)
        FileViewerType.UNSUPPORTED -> UnsupportedFileViewer(
            tab = tab,
            onForceOpenAsText = {
                try {
                    val text = tab.file.readText()
                    tab.forceOpenAsText(text)
                } catch (e: Exception) {
                    Toast.makeText(context, "Cannot read file as text: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
        FileViewerType.TEXT -> {
            CodeCanvas(
                tab = tab,
                settings = settings,
                ctrlActive = ctrlActive,
                shiftActive = shiftActive,
                altActive = altActive,
                pendingActionType = pendingActionType,
                onClearPendingAction = onClearPendingAction,
                onResetModifiers = onResetModifiers,
                onOpenCommandPalette = onOpenCommandPalette,
                onOpenContextMenu = onOpenContextMenu,
                onOpenWorkspaceSearch = onOpenWorkspaceSearch,
                onShowInfoDialog = onShowInfoDialog,
                onSaveRequested = onSaveRequested,
                onContentChange = onContentChange,
                onCursorChange = onCursorChange
            )
        }
    }
}

@Composable
private fun SecondaryEditorPane(
    tab: EditorTab,
    allTabs: List<EditorTab>,
    settings: AppSettings,
    workspaceDir: File? = null,
    onSelectTab: (EditorTab) -> Unit,
    onCloseSplit: () -> Unit,
    onSaveRequested: () -> Unit,
    onOpenWorkspaceSearch: () -> Unit = {}
) {
    var showTabDropdown by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { showTabDropdown = true }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = tab.fileName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "▾",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showTabDropdown,
                    onDismissRequest = { showTabDropdown = false }
                ) {
                    allTabs.forEach { itemTab ->
                        DropdownMenuItem(
                            text = { Text(itemTab.fileName) },
                            onClick = {
                                onSelectTab(itemTab)
                                showTabDropdown = false
                            }
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (tab.isModified) {
                    IconButton(
                        onClick = {
                            try {
                                EditorManager.getInstance().saveTab(tab)
                                onSaveRequested()
                            } catch (e: Exception) {
                                android.util.Log.e("EditorView", "Failed saving split tab", e)
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save Split Tab",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onCloseSplit,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Split",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        BreadcrumbsBar(
            tab = tab,
            workspaceDir = workspaceDir,
            onOpenFile = { file ->
                val existing = allTabs.firstOrNull { it.filePath == file.absolutePath }
                if (existing != null) {
                    onSelectTab(existing)
                } else {
                    try {
                        val opened = EditorManager.getInstance().openFile(file)
                        onSelectTab(opened)
                    } catch (e: Exception) {
                        android.util.Log.e("SecondaryEditorPane", "Failed to open file: ${file.name}", e)
                    }
                }
            }
        )

        Box(modifier = Modifier.fillMaxSize()) {
            EditorTabContent(
                tab = tab,
                settings = settings,
                ctrlActive = false,
                shiftActive = false,
                altActive = false,
                pendingActionType = null,
                onClearPendingAction = {},
                onResetModifiers = {},
                onOpenCommandPalette = {},
                onOpenContextMenu = {},
                onOpenWorkspaceSearch = onOpenWorkspaceSearch,
                onShowInfoDialog = { _, _ -> },
                onSaveRequested = onSaveRequested,
                onContentChange = { newText ->
                    EditorManager.getInstance().updateTabContent(tab, newText)
                },
                onCursorChange = { pos ->
                    tab.updateCursor(pos)
                }
            )
        }
    }
}

@Composable
private fun CodeCanvas(
    tab: EditorTab,
    settings: AppSettings,
    ctrlActive: Boolean,
    shiftActive: Boolean,
    altActive: Boolean,
    pendingActionType: String?,
    onClearPendingAction: () -> Unit,
    onResetModifiers: () -> Unit,
    onOpenCommandPalette: () -> Unit,
    onOpenContextMenu: () -> Unit,
    onOpenWorkspaceSearch: () -> Unit,
    onShowInfoDialog: (String, String) -> Unit,
    onSaveRequested: () -> Unit,
    onContentChange: (String) -> Unit,
    onCursorChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val editorMgr = remember { EditorManager.getInstance() }
    val activeFontFamily = remember(settings.editorFontFamily) {
        EditorFontHelper.getFontFamily(settings.editorFontFamily)
    }

    LaunchedEffect(pendingActionType) {
        if (pendingActionType != null) {
            val act = pendingActionType
            onClearPendingAction()
            val textValue = TextFieldValue(
                text = tab.content,
                selection = TextRange(tab.cursorPosition.coerceIn(0, tab.content.length))
            )

            when {
                act == "CUT" -> EditorActionsHandler.cut(context, tab, textValue) {
                    onContentChange(it.text)
                    tab.updateCursor(it.selection.start)
                    onCursorChange(it.selection.start)
                }
                act == "COPY" -> EditorActionsHandler.copy(context, tab, textValue)
                act == "PASTE" -> EditorActionsHandler.paste(context, textValue) {
                    onContentChange(it.text)
                    tab.updateCursor(it.selection.start)
                    onCursorChange(it.selection.start)
                }
                act == "PASTE_PLAIN" -> EditorActionsHandler.pastePlain(context, textValue) {
                    onContentChange(it.text)
                    tab.updateCursor(it.selection.start)
                    onCursorChange(it.selection.start)
                }
                act == "SELECT_ALL" -> EditorActionsHandler.selectAll(textValue) {
                    tab.updateSelection(it.selection.start, it.selection.end)
                }
                act == "SELECT_LINE" -> EditorActionsHandler.selectLine(textValue) {
                    tab.updateSelection(it.selection.start, it.selection.end)
                }
                act == "DUPLICATE_LINE" -> EditorActionsHandler.duplicateLineOrSelection(textValue) {
                    onContentChange(it.text)
                    tab.updateCursor(it.selection.start)
                    onCursorChange(it.selection.start)
                }
                act == "DELETE_LINE" -> EditorActionsHandler.deleteLine(textValue) {
                    onContentChange(it.text)
                    tab.updateCursor(it.selection.start)
                    onCursorChange(it.selection.start)
                }
                act == "JOIN_LINES" -> EditorActionsHandler.joinLines(textValue) {
                    onContentChange(it.text)
                    tab.updateCursor(it.selection.start)
                    onCursorChange(it.selection.start)
                }
                act.startsWith("GO_TO_LINE:") -> {
                    val lineNum = act.removePrefix("GO_TO_LINE:").toIntOrNull() ?: 1
                    EditorActionsHandler.goToLine(tab, lineNum) {
                        tab.updateCursor(it.selection.start)
                        onCursorChange(it.selection.start)
                    }
                }
                act == "GO_DEFINITION" || act == "GO_DECLARATION" -> {
                    EditorActionsHandler.goToDefinition(context, tab, textValue) {
                        tab.updateCursor(it.selection.start)
                        onCursorChange(it.selection.start)
                    }
                }
                act == "FORMAT_DOC" -> EditorActionsHandler.formatDocument(tab) {
                    onContentChange(it.text)
                    tab.updateCursor(it.selection.start)
                    onCursorChange(it.selection.start)
                }
                act == "TOGGLE_COMMENT" -> EditorActionsHandler.toggleCommentLine(tab, textValue) {
                    onContentChange(it.text)
                    tab.updateCursor(it.selection.start)
                    onCursorChange(it.selection.start)
                }
                act == "INDENT" -> EditorActionsHandler.indent(textValue) {
                    onContentChange(it.text)
                    tab.updateCursor(it.selection.start)
                    onCursorChange(it.selection.start)
                }
                act == "OUTDENT" -> EditorActionsHandler.outdent(textValue) {
                    onContentChange(it.text)
                    tab.updateCursor(it.selection.start)
                    onCursorChange(it.selection.start)
                }
                act == "SHOW_FIND" -> {
                    tab.showFindBar = true
                    tab.showReplaceBar = false
                }
                act == "SHOW_REPLACE" -> {
                    tab.showFindBar = true
                    tab.showReplaceBar = true
                }
                act == "RUN" -> EditorActionsHandler.runFile(context, tab) { title, res ->
                    onShowInfoDialog(title, res)
                }
                act == "DEBUG" -> EditorActionsHandler.debugFile(context, tab) { title, res ->
                    onShowInfoDialog(title, res)
                }
                act == "GIT_DIFF" -> EditorActionsHandler.gitDiff(context, tab) { title, res ->
                    onShowInfoDialog(title, res)
                }
                act == "GIT_BLAME" -> EditorActionsHandler.gitBlame(context, tab) { title, res ->
                    onShowInfoDialog(title, res)
                }
                act == "GIT_HISTORY" -> EditorActionsHandler.gitHistory(context, tab) { title, res ->
                    onShowInfoDialog(title, res)
                }
            }
        }
    }

    var lineDiffMap by remember(tab.filePath) { mutableStateOf<Map<Int, LineDiffStatus>>(emptyMap()) }
    LaunchedEffect(tab.content, tab.originalContent) {
        kotlinx.coroutines.delay(350L)
        withContext(Dispatchers.Default) {
            val diff = LineDiffCalculator.computeDiff(tab.originalContent, tab.content)
            withContext(Dispatchers.Main) {
                lineDiffMap = diff
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Find / Replace Bar Overlay
        if (tab.showFindBar) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    .padding(6.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(CornerSmall))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(CornerSmall))
                                .padding(horizontal = SpacingM, vertical = SpacingS),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (tab.findQuery.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.find_placeholder),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                            BasicTextField(
                                value = tab.findQuery,
                                onValueChange = { tab.findQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFamily = activeFontFamily
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        IconButton(
                            onClick = {
                                if (tab.findQuery.isNotEmpty() && tab.content.contains(tab.findQuery, ignoreCase = true)) {
                                    val nextPos = tab.content.indexOf(tab.findQuery, tab.cursorPosition + 1, ignoreCase = true)
                                        .let { if (it < 0) tab.content.indexOf(tab.findQuery, ignoreCase = true) else it }
                                    if (nextPos >= 0) {
                                        tab.updateCursor(nextPos)
                                        onCursorChange(nextPos)
                                    }
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Next", modifier = Modifier.size(16.dp))
                        }

                        IconButton(
                            onClick = {
                                if (!tab.showReplaceBar) tab.showReplaceBar = true
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.FindReplace, contentDescription = "Replace Mode", modifier = Modifier.size(16.dp))
                        }

                        IconButton(
                            onClick = { tab.showFindBar = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_close), modifier = Modifier.size(16.dp))
                        }
                    }

                    if (tab.showReplaceBar) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(CornerSmall))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(CornerSmall))
                                    .padding(horizontal = SpacingM, vertical = SpacingS),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (tab.replaceQuery.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.replace_placeholder),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                                BasicTextField(
                                    value = tab.replaceQuery,
                                    onValueChange = { tab.replaceQuery = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontFamily = activeFontFamily
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Button(
                                onClick = {
                                    if (tab.findQuery.isNotEmpty()) {
                                        val newText = tab.content.replaceFirst(tab.findQuery, tab.replaceQuery, ignoreCase = true)
                                        onContentChange(newText)
                                    }
                                },
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text(stringResource(R.string.action_replace), fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    if (tab.findQuery.isNotEmpty()) {
                                        val newText = tab.content.replace(tab.findQuery, tab.replaceQuery, ignoreCase = true)
                                        onContentChange(newText)
                                    }
                                },
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text(stringResource(R.string.action_replace_all), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // High-performance virtualized DroidCodeEngine
        DroidCodeEditor(
            tab = tab,
            settings = settings,
            ctrlActive = ctrlActive,
            shiftActive = shiftActive,
            altActive = altActive,
            lineDiffMap = lineDiffMap,
            onContentChange = onContentChange,
            onCursorChange = { line, col ->
                tab.updateCursor(line, col)
                onCursorChange(tab.cursorPosition)
            },
            onSaveRequested = onSaveRequested,
            onUndoRequested = { editorMgr.undoActiveTab() },
            onRedoRequested = { editorMgr.redoActiveTab() },
            onOpenCommandPalette = onOpenCommandPalette,
            onOpenWorkspaceSearch = onOpenWorkspaceSearch,
            onOpenFind = {
                tab.showFindBar = true
                tab.showReplaceBar = false
            },
            onResetModifiers = onResetModifiers,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("code_editor_text_input")
        )
    }
}

@Composable
private fun getEditorFileIcon(nameOrExt: String) = FileIconUtils.getFileIcon(nameOrExt)

@Composable
private fun getEditorFileIconColor(nameOrExt: String) = FileIconUtils.getFileIconColor(nameOrExt)

@Composable
private fun ImageViewer(file: File) {
    val context = LocalContext.current
    var bitmap by remember(file.absolutePath) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(file.absolutePath) { mutableStateOf(true) }
    var errorMsg by remember(file.absolutePath) { mutableStateOf<String?>(null) }

    LaunchedEffect(file.absolutePath) {
        withContext(Dispatchers.IO) {
            try {
                bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap == null) {
                    errorMsg = "Unable to decode image"
                }
            } catch (e: Exception) {
                errorMsg = e.message
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = file.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (bitmap != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(${bitmap!!.width} x ${bitmap!!.height} px • ${formatFileSize(file.length())})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Button(
                    onClick = { openFileWithSystemApp(context, file) },
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Open with System App", fontSize = 11.sp)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (errorMsg != null || bitmap == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Failed to load image: ${errorMsg ?: "Unknown error"}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = file.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun VideoViewer(file: File) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = file.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${formatFileSize(file.length())})",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { openFileWithSystemApp(context, file) },
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Open with System Player", fontSize = 11.sp)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        val videoView = this
                        setMediaController(MediaController(ctx).apply { setAnchorView(videoView) })
                        setVideoPath(file.absolutePath)
                        requestFocus()
                        start()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun PdfViewer(file: File) {
    val context = LocalContext.current
    var pages by remember(file.absolutePath) { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoading by remember(file.absolutePath) { mutableStateOf(true) }
    var errorMsg by remember(file.absolutePath) { mutableStateOf<String?>(null) }

    LaunchedEffect(file.absolutePath) {
        withContext(Dispatchers.IO) {
            try {
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val pdfRenderer = PdfRenderer(pfd)
                val pageList = mutableListOf<Bitmap>()
                val pageCount = pdfRenderer.pageCount
                for (i in 0 until pageCount) {
                    val page = pdfRenderer.openPage(i)
                    val width = page.width * 2
                    val height = page.height * 2
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    pageList.add(bitmap)
                    page.close()
                }
                pdfRenderer.close()
                pfd.close()
                pages = pageList
            } catch (e: Exception) {
                errorMsg = e.message
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = file.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (pages.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(${pages.size} pages)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Button(
                    onClick = { openFileWithSystemApp(context, file) },
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Open in System Reader", fontSize = 11.sp)
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (errorMsg != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Could not render PDF: $errorMsg",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { openFileWithSystemApp(context, file) }) {
                        Text("Open with External PDF Viewer")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    itemsIndexed(pages) { index, pageBitmap ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text(
                                    text = "Page ${index + 1} of ${pages.size}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Image(
                                    bitmap = pageBitmap.asImageBitmap(),
                                    contentDescription = "PDF Page ${index + 1}",
                                    modifier = Modifier.fillMaxWidth()
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
private fun UnsupportedFileViewer(
    tab: EditorTab,
    onForceOpenAsText: () -> Unit
) {
    val context = LocalContext.current
    val file = tab.file

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.widthIn(max = 480.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = getEditorFileIcon(file.name),
                    contentDescription = null,
                    tint = getEditorFileIconColor(file.name),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = file.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "File Format: ${if (file.extension.isNotEmpty()) file.extension.uppercase() else "BINARY"} • ${formatFileSize(file.length())}",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "This file extension cannot be edited directly in text mode. You can open it using an external Android app selection.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { openFileWithSystemApp(context, file) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open with System App", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onForceOpenAsText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Force Open as Raw Text")
                }
            }
        }
    }
}

private fun openFileWithSystemApp(context: Context, file: File) {
    try {
        val uri: Uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Uri.fromFile(file)
        }
        val ext = file.extension.lowercase()
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: when (ext) {
            "png", "jpg", "jpeg", "gif", "webp", "bmp", "svg" -> "image/*"
            "mp4", "mkv", "webm", "avi", "mov", "3gp" -> "video/*"
            "pdf" -> "application/pdf"
            "apk" -> "application/vnd.android.package-archive"
            "zip", "rar", "7z", "tar", "gz" -> "application/zip"
            else -> "*/*"
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Open with...")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "No application available to open this file (${e.message})", Toast.LENGTH_LONG).show()
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    return String.format(java.util.Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

