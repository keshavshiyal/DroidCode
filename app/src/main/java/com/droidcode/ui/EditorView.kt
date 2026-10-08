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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material3.MaterialTheme
import com.droidcode.database.sqlite.DatabaseConsoleDialog
import com.droidcode.language.css.ColorPickerDialog
import com.droidcode.language.js.JsDiagnostic
import com.droidcode.language.js.JsProblemsDialog
import com.droidcode.language.js.JsSyntaxChecker
import com.droidcode.language.json.JsonToolHelper
import com.droidcode.language.json.JsonTreeViewerDialog
import com.droidcode.web.HtmlPreviewDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.zIndex
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.runtime.key
import kotlin.math.roundToInt
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
    var showHtmlPreviewDialog by rememberSaveable { mutableStateOf(false) }
    var showJsonTreeDialog by rememberSaveable { mutableStateOf(false) }
    var showColorPickerDialog by rememberSaveable { mutableStateOf(false) }
    var showJsProblemsDialog by rememberSaveable { mutableStateOf(false) }
    var showDatabaseConsoleDialog by rememberSaveable { mutableStateOf(false) }
    var activeDbFile by remember { mutableStateOf<File?>(null) }
    var jsDiagnosticsList by remember { mutableStateOf<List<JsDiagnostic>>(emptyList()) }
    var splitMode by rememberSaveable { mutableStateOf(EditorSplitMode.NONE) }
    var activePane by rememberSaveable { mutableStateOf("PRIMARY") }
    var primaryTabId by rememberSaveable { mutableStateOf<String?>(null) }
    var secondaryTabId by rememberSaveable { mutableStateOf<String?>(null) }
    var infoDialogTitle by rememberSaveable { mutableStateOf<String?>(null) }
    var infoDialogText by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingActionType by rememberSaveable { mutableStateOf<String?>(null) }
    var tabContextMenuIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var draggingTabIndex by remember { mutableStateOf<Int?>(null) }
    var dragCurrentWindowPos by remember { mutableStateOf<Offset?>(null) }
    var editorContainerBounds by remember { mutableStateOf<Rect?>(null) }

    LaunchedEffect(tabs.size) {
        if (tabs.size <= 1 && splitMode != EditorSplitMode.NONE) {
            splitMode = EditorSplitMode.NONE
            primaryTabId = null
            secondaryTabId = null
            activePane = "PRIMARY"
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.smallestScreenWidthDp >= 600

    val toggleSplit: () -> Unit = {
        val nextMode = if (!isTablet) {
            if (splitMode == EditorSplitMode.NONE) {
                if (isLandscape) EditorSplitMode.HORIZONTAL else EditorSplitMode.VERTICAL
            } else {
                EditorSplitMode.NONE
            }
        } else {
            when (splitMode) {
                EditorSplitMode.NONE -> EditorSplitMode.HORIZONTAL
                EditorSplitMode.HORIZONTAL -> EditorSplitMode.VERTICAL
                EditorSplitMode.VERTICAL -> EditorSplitMode.NONE
            }
        }
        splitMode = nextMode
        if (nextMode == EditorSplitMode.NONE) {
            val keepTab = if (activePane == "SECONDARY" && secondaryTabId != null) {
                tabs.firstOrNull { it.id == secondaryTabId }
            } else {
                tabs.firstOrNull { it.id == primaryTabId } ?: activeTab
            }
            primaryTabId = null
            secondaryTabId = null
            activePane = "PRIMARY"
            if (keepTab != null) {
                editorMgr.selectTab(keepTab.id)
            }
        } else {
            val currentActiveId = activeTab?.id ?: tabs.firstOrNull()?.id
            primaryTabId = currentActiveId
            secondaryTabId = tabs.firstOrNull { it.id != currentActiveId }?.id
            activePane = "PRIMARY"
        }
    }

    val effectiveSplitMode = remember(splitMode, isTablet, isLandscape) {
        if (splitMode == EditorSplitMode.NONE) {
            EditorSplitMode.NONE
        } else if (!isTablet) {
            if (isLandscape) EditorSplitMode.HORIZONTAL else EditorSplitMode.VERTICAL
        } else {
            splitMode
        }
    }

    // Opens the split with `targetId` in the secondary pane. The pane that was
    // focused/active stays in the primary pane (never the same tab in both panes).
    val startSplitWith: (String, EditorSplitMode) -> Unit = { targetId, mode ->
        val currentActiveId = activeTab?.id ?: tabs.firstOrNull()?.id
        if (targetId == currentActiveId) {
            primaryTabId = currentActiveId
            secondaryTabId = tabs.firstOrNull { it.id != currentActiveId }?.id
        } else {
            primaryTabId = currentActiveId
            secondaryTabId = targetId
        }
        splitMode = mode
        activePane = "PRIMARY"
    }

    val currentToggleSplit by rememberUpdatedState(toggleSplit)

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
            onToggleSplitEditor = { currentToggleSplit() },
            onShowHtmlPreview = { showHtmlPreviewDialog = true },
            onShowJsonTree = { showJsonTreeDialog = true },
            onFormatJson = {
                activeTab?.let { tab ->
                    val res = JsonToolHelper.formatJson(tab.content)
                    if (res.isSuccess) {
                        tab.updateContent(res.getOrThrow())
                        Toast.makeText(context, "JSON formatted", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Cannot format: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onMinifyJson = {
                activeTab?.let { tab ->
                    val res = JsonToolHelper.minifyJson(tab.content)
                    if (res.isSuccess) {
                        tab.updateContent(res.getOrThrow())
                        Toast.makeText(context, "JSON minified", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Cannot minify: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onValidateJson = {
                activeTab?.let { tab ->
                    val res = JsonToolHelper.validateJson(tab.content)
                    if (res.isValid) {
                        Toast.makeText(context, "Valid JSON syntax", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Invalid JSON: ${res.errorMessage} (Line ${res.line})", Toast.LENGTH_LONG).show()
                        pendingActionType = "GO_TO_LINE:${res.line}"
                    }
                }
            },
            onShowColorPicker = { showColorPickerDialog = true },
            onCheckJsSyntax = {
                activeTab?.let { tab ->
                    jsDiagnosticsList = JsSyntaxChecker.checkSyntax(tab.content)
                    showJsProblemsDialog = true
                }
            },
            onShowDatabaseConsole = {
                activeTab?.let { tab ->
                    activeDbFile = File(tab.filePath)
                    showDatabaseConsoleDialog = true
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
            // Pinned Top Navigation Header Bars (Tab Bar, Breadcrumbs, Editor Toolbar)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(5f)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // Tab Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .background(MaterialTheme.colorScheme.surface),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            val isActive = index == editorMgr.activeTabIndex
                            val bg = if (isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                            val tabBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            var tabGlobalPos by remember { mutableStateOf(Offset.Zero) }

                            Row(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .background(bg)
                                    .border(1.dp, tabBorderColor)
                                    .onGloballyPositioned { coords ->
                                        tabGlobalPos = coords.positionInWindow()
                                    }
                                    .pointerInput(tab.filePath) {
                                        var totalDrag = Offset.Zero
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { localOffset ->
                                                totalDrag = Offset.Zero
                                                draggingTabIndex = index
                                                dragCurrentWindowPos = tabGlobalPos + localOffset
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                totalDrag += dragAmount
                                                dragCurrentWindowPos = (dragCurrentWindowPos ?: tabGlobalPos) + dragAmount
                                            },
                                            onDragEnd = {
                                                val pos = dragCurrentWindowPos
                                                val bounds = editorContainerBounds
                                                if (totalDrag.getDistance() < 12f) {
                                                    tabContextMenuIndex = index
                                                } else if (pos != null && bounds != null && pos.x in bounds.left..bounds.right && pos.y in bounds.top..bounds.bottom) {
                                                    val isRight = isLandscape && pos.x > bounds.left + bounds.width * 0.4f
                                                    startSplitWith(
                                                        tabs[index].id,
                                                        if (isRight) EditorSplitMode.HORIZONTAL else EditorSplitMode.VERTICAL
                                                    )
                                                }
                                                draggingTabIndex = null
                                                dragCurrentWindowPos = null
                                            },
                                            onDragCancel = {
                                                draggingTabIndex = null
                                                dragCurrentWindowPos = null
                                            }
                                        )
                                    }
                                    .clickable {
                                        val clickedTab = tabs[index]
                                        if (effectiveSplitMode != EditorSplitMode.NONE) {
                                            if (activePane == "SECONDARY") {
                                                secondaryTabId = clickedTab.id
                                            } else {
                                                primaryTabId = clickedTab.id
                                            }
                                        } else {
                                            primaryTabId = null
                                            secondaryTabId = null
                                            activePane = "PRIMARY"
                                        }
                                        editorMgr.activeTabIndex = index
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
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

                                if (tabs.size > 1) {
                                    Icon(
                                        imageVector = Icons.Default.Splitscreen,
                                        contentDescription = "Split with this tab",
                                        tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .size(13.dp)
                                            .clickable {
                                                startSplitWith(
                                                    tabs[index].id,
                                                    if (isLandscape) EditorSplitMode.HORIZONTAL else EditorSplitMode.VERTICAL
                                                )
                                            }
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }

                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.action_close_tab),
                                    tint = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable {
                                            val tabToClose = tabs[index]
                                            if (tabToClose.isModified) {
                                                tabToPromptCloseIndex = index
                                            } else {
                                                val closingId = tabToClose.id
                                                if (primaryTabId == closingId) primaryTabId = null
                                                if (secondaryTabId == closingId) secondaryTabId = null
                                                editorMgr.closeTab(index)
                                                if (editorMgr.tabs.size <= 1) {
                                                    splitMode = EditorSplitMode.NONE
                                                    primaryTabId = null
                                                    secondaryTabId = null
                                                    activePane = "PRIMARY"
                                                }
                                            }
                                        }
                                )

                                DropdownMenu(
                                    expanded = tabContextMenuIndex == index,
                                    onDismissRequest = { tabContextMenuIndex = null }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Split Down (Vertical)") },
                                        leadingIcon = { Icon(Icons.Default.Splitscreen, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        onClick = {
                                            startSplitWith(tabs[index].id, EditorSplitMode.VERTICAL)
                                            tabContextMenuIndex = null
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Split Right (Horizontal)") },
                                        leadingIcon = { Icon(Icons.Default.Splitscreen, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        onClick = {
                                            startSplitWith(tabs[index].id, EditorSplitMode.HORIZONTAL)
                                            tabContextMenuIndex = null
                                        }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("Close Tab") },
                                        leadingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        onClick = {
                                            if (tab.isModified) {
                                                tabToPromptCloseIndex = index
                                            } else {
                                                editorMgr.closeTab(index)
                                            }
                                            tabContextMenuIndex = null
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Close Other Tabs") },
                                        onClick = {
                                            editorMgr.closeOtherTabs(index)
                                            tabContextMenuIndex = null
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Close Tabs to Right") },
                                        onClick = {
                                            editorMgr.closeTabsToRight(index)
                                            tabContextMenuIndex = null
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Pinned File & Tab Actions Menu at the right of the Tab Bar
                    Surface(
                        onClick = {
                            if (onOpenGeneralMenu != null) {
                                onOpenGeneralMenu()
                            } else {
                                showContextMenu = true
                            }
                        },
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(30.dp)
                            .testTag("tab_bar_file_menu_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "File & Editor Menu",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (settings.isBreadcrumbsEnabled) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Breadcrumbs Navigation Bar
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

                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Editor Toolbar & Language / Line Stats
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Section: File Stats & Quick Configuration Chips
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Language Badge
                        Surface(
                            onClick = { showChangeLanguageDialog = true },
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = activeTab.languageId.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "▾",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Encoding Badge
                        Surface(
                            onClick = { showChangeEncodingDialog = true },
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = activeTab.encoding,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "▾",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Line Ending Badge
                        Surface(
                            onClick = { showChangeLineEndingDialog = true },
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = activeTab.lineEnding,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "▾",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Line / Col Position Badge (Clickable to Jump to Line)
                        Surface(
                            onClick = { showGoToLineDialog = true },
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.status_line_col, activeTab.line, activeTab.column),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    VerticalDivider(
                        modifier = Modifier
                            .height(18.dp)
                            .padding(horizontal = 2.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Right Section: Action Buttons
                    Row(
                        modifier = Modifier.wrapContentWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Contextual Action Buttons for active file type
                        val activeExt = activeTab.fileName.substringAfterLast('.', "").lowercase()
                        when (activeExt) {
                            "html", "htm" -> {
                                Surface(
                                    onClick = { showHtmlPreviewDialog = true },
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            imageVector = Icons.Outlined.OpenInBrowser,
                                            contentDescription = "Live HTML Preview",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            "json" -> {
                                Surface(
                                    onClick = { showJsonTreeDialog = true },
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            imageVector = Icons.Outlined.AccountTree,
                                            contentDescription = "JSON Tree Viewer",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            "css", "scss", "less" -> {
                                Surface(
                                    onClick = { showColorPickerDialog = true },
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            imageVector = Icons.Default.Palette,
                                            contentDescription = "Color Picker",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            "js", "jsx", "ts", "tsx" -> {
                                Surface(
                                    onClick = {
                                        jsDiagnosticsList = JsSyntaxChecker.checkSyntax(activeTab.content)
                                        showJsProblemsDialog = true
                                    },
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            imageVector = Icons.Outlined.BugReport,
                                            contentDescription = "Check Syntax & Lint",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            "db", "sqlite", "sqlite3" -> {
                                Surface(
                                    onClick = {
                                        activeDbFile = File(activeTab.filePath)
                                        showDatabaseConsoleDialog = true
                                    },
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            imageVector = Icons.Default.Storage,
                                            contentDescription = "SQLite Console",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Find in Files
                        Surface(
                            onClick = { showWorkspaceSearchDialog = true },
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("editor_find_in_files_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Find in Files (Ctrl+Shift+F)",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Split Editor
                        Surface(
                            onClick = toggleSplit,
                            shape = RoundedCornerShape(4.dp),
                            color = if (effectiveSplitMode != EditorSplitMode.NONE) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, if (effectiveSplitMode != EditorSplitMode.NONE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("editor_split_mode_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.Default.Splitscreen,
                                    contentDescription = "Toggle Split Editor",
                                    tint = if (effectiveSplitMode != EditorSplitMode.NONE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Save Active File
                        Surface(
                            onClick = {
                                try {
                                    editorMgr.saveActiveTab()
                                    onSaveRequested()
                                } catch (e: Exception) {
                                    android.util.Log.e("EditorView", "Failed saving active tab", e)
                                    Toast.makeText(context, "Failed to save file: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(4.dp),
                            color = if (activeTab.isModified) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, if (activeTab.isModified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("editor_save_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = "Save",
                                    tint = if (activeTab.isModified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // File & Project Menubar
                        Surface(
                            onClick = {
                                if (onOpenGeneralMenu != null) {
                                    onOpenGeneralMenu()
                                } else {
                                    showContextMenu = true
                                }
                            },
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("editor_general_menu_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "File & Project Menubar",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        // IDE Context Menu
                        Surface(
                            onClick = { showContextMenu = true },
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("editor_context_menu_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "IDE Context Menu",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
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
                            val closingId = tabToClose.id
                            if (primaryTabId == closingId) primaryTabId = null
                            if (secondaryTabId == closingId) secondaryTabId = null
                            editorMgr.closeTab(tabToPromptCloseIndex!!)
                            if (editorMgr.tabs.size <= 1) {
                                splitMode = EditorSplitMode.NONE
                                primaryTabId = null
                                secondaryTabId = null
                                activePane = "PRIMARY"
                            }
                            tabToPromptCloseIndex = null
                        }
                    ) { Text(stringResource(R.string.action_save)) }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = {
                                val closingId = tabToClose.id
                                if (primaryTabId == closingId) primaryTabId = null
                                if (secondaryTabId == closingId) secondaryTabId = null
                                editorMgr.closeTab(tabToPromptCloseIndex!!)
                                if (editorMgr.tabs.size <= 1) {
                                    splitMode = EditorSplitMode.NONE
                                    primaryTabId = null
                                    secondaryTabId = null
                                    activePane = "PRIMARY"
                                }
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

        // Split-Pane or Single Editor Container
        val singleTab: EditorTab? = activeTab ?: tabs.firstOrNull()
        val splitPrimaryTab: EditorTab? = if (effectiveSplitMode == EditorSplitMode.NONE) {
            singleTab
        } else {
            tabs.firstOrNull { it.id == primaryTabId } ?: activeTab ?: tabs.firstOrNull()
        }
        val splitSecondTab: EditorTab? = if (effectiveSplitMode == EditorSplitMode.NONE) {
            null
        } else {
            tabs.firstOrNull { it.id == secondaryTabId && it.id != splitPrimaryTab?.id }
                ?: tabs.firstOrNull { it.id != splitPrimaryTab?.id }
        }

        val closeSplitAction: () -> Unit = {
            val keepTab = if (activePane == "SECONDARY" && splitSecondTab != null) {
                splitSecondTab
            } else {
                splitPrimaryTab ?: singleTab
            }
            splitMode = EditorSplitMode.NONE
            primaryTabId = null
            secondaryTabId = null
            activePane = "PRIMARY"
            if (keepTab != null) {
                editorMgr.selectTab(keepTab.id)
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    editorContainerBounds = coords.boundsInWindow()
                }
        ) {
            if (effectiveSplitMode == EditorSplitMode.NONE && singleTab != null) {
                val boundTab = singleTab
                key("single_${boundTab.filePath}") {
                    EditorTabContent(
                        tab = boundTab,
                        paneId = "single",
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
                            editorMgr.updateTabContentByPath(boundTab.filePath, newText)
                        },
                        onCursorChange = { pos: Int ->
                            boundTab.updateCursor(pos)
                        },
                        onOpenDatabaseConsole = { file ->
                            activeDbFile = file
                            showDatabaseConsoleDialog = true
                        }
                    )
                }
            } else if (effectiveSplitMode == EditorSplitMode.HORIZONTAL && splitPrimaryTab != null) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        key("horizontal_p1_${splitPrimaryTab.filePath}") {
                            SplitEditorViewPane(
                                tab = splitPrimaryTab,
                                allTabs = tabs,
                                isFocused = activePane == "PRIMARY",
                                paneTitle = "Pane 1",
                                paneId = "horizontal_pane1",
                                settings = settings,
                                ctrlActive = if (activePane == "PRIMARY") ctrlActive else false,
                                shiftActive = if (activePane == "PRIMARY") shiftActive else false,
                                altActive = if (activePane == "PRIMARY") altActive else false,
                                pendingActionType = if (activePane == "PRIMARY") pendingActionType else null,
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
                                    editorMgr.updateTabContentByPath(splitPrimaryTab.filePath, newText)
                                },
                                onCursorChange = { pos: Int ->
                                    splitPrimaryTab.updateCursor(pos)
                                },
                                onSelectTab = { selectedTab ->
                                    if (selectedTab.id == secondaryTabId) {
                                        secondaryTabId = primaryTabId
                                    }
                                    primaryTabId = selectedTab.id
                                    activePane = "PRIMARY"
                                    editorMgr.selectTab(selectedTab.id)
                                },
                                onFocusPane = {
                                    activePane = "PRIMARY"
                                    editorMgr.selectTab(splitPrimaryTab.id)
                                },
                                onCloseSplit = null,
                                onOpenDatabaseConsole = { file ->
                                    activeDbFile = file
                                    showDatabaseConsoleDialog = true
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    VerticalDivider(
                        modifier = Modifier.width(1.dp).fillMaxHeight(),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        if (splitSecondTab != null) {
                            key("horizontal_p2_${splitSecondTab.filePath}") {
                                SplitEditorViewPane(
                                    tab = splitSecondTab,
                                    allTabs = tabs,
                                    isFocused = activePane == "SECONDARY",
                                    paneTitle = "Pane 2",
                                    paneId = "horizontal_pane2",
                                    settings = settings,
                                    ctrlActive = if (activePane == "SECONDARY") ctrlActive else false,
                                    shiftActive = if (activePane == "SECONDARY") shiftActive else false,
                                    altActive = if (activePane == "SECONDARY") altActive else false,
                                    pendingActionType = if (activePane == "SECONDARY") pendingActionType else null,
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
                                        editorMgr.updateTabContentByPath(splitSecondTab.filePath, newText)
                                    },
                                    onCursorChange = { pos: Int ->
                                        splitSecondTab.updateCursor(pos)
                                    },
                                    onSelectTab = { selectedTab ->
                                        if (selectedTab.id == primaryTabId) {
                                            primaryTabId = secondaryTabId
                                        }
                                        secondaryTabId = selectedTab.id
                                        activePane = "SECONDARY"
                                        editorMgr.selectTab(selectedTab.id)
                                    },
                                    onFocusPane = {
                                        activePane = "SECONDARY"
                                        editorMgr.selectTab(splitSecondTab.id)
                                    },
                                    onCloseSplit = closeSplitAction,
                                    onOpenDatabaseConsole = { file ->
                                        activeDbFile = file
                                        showDatabaseConsoleDialog = true
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            SplitSelectFileView(
                                workspaceDir = currentProject?.directory,
                                onFileSelected = { file ->
                                    val openedTab = editorMgr.openFile(file)
                                    secondaryTabId = openedTab.id
                                    activePane = "SECONDARY"
                                },
                                onCloseSplit = closeSplitAction
                            )
                        }
                    }
                }
            } else if (splitPrimaryTab != null) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        key("vertical_p1_${splitPrimaryTab.filePath}") {
                            SplitEditorViewPane(
                                tab = splitPrimaryTab,
                                allTabs = tabs,
                                isFocused = activePane == "PRIMARY",
                                paneTitle = "Pane 1",
                                paneId = "vertical_pane1",
                                settings = settings,
                                ctrlActive = if (activePane == "PRIMARY") ctrlActive else false,
                                shiftActive = if (activePane == "PRIMARY") shiftActive else false,
                                altActive = if (activePane == "PRIMARY") altActive else false,
                                pendingActionType = if (activePane == "PRIMARY") pendingActionType else null,
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
                                    editorMgr.updateTabContentByPath(splitPrimaryTab.filePath, newText)
                                },
                                onCursorChange = { pos: Int ->
                                    splitPrimaryTab.updateCursor(pos)
                                },
                                onSelectTab = { selectedTab ->
                                    if (selectedTab.id == secondaryTabId) {
                                        secondaryTabId = primaryTabId
                                    }
                                    primaryTabId = selectedTab.id
                                    activePane = "PRIMARY"
                                    editorMgr.selectTab(selectedTab.id)
                                },
                                onFocusPane = {
                                    activePane = "PRIMARY"
                                    editorMgr.selectTab(splitPrimaryTab.id)
                                },
                                onCloseSplit = null,
                                onOpenDatabaseConsole = { file ->
                                    activeDbFile = file
                                    showDatabaseConsoleDialog = true
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.height(1.dp).fillMaxWidth(),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (splitSecondTab != null) {
                            key("vertical_p2_${splitSecondTab.filePath}") {
                                SplitEditorViewPane(
                                    tab = splitSecondTab,
                                    allTabs = tabs,
                                    isFocused = activePane == "SECONDARY",
                                    paneTitle = "Pane 2",
                                    paneId = "vertical_pane2",
                                    settings = settings,
                                    ctrlActive = if (activePane == "SECONDARY") ctrlActive else false,
                                    shiftActive = if (activePane == "SECONDARY") shiftActive else false,
                                    altActive = if (activePane == "SECONDARY") altActive else false,
                                    pendingActionType = if (activePane == "SECONDARY") pendingActionType else null,
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
                                        editorMgr.updateTabContentByPath(splitSecondTab.filePath, newText)
                                    },
                                    onCursorChange = { pos: Int ->
                                        splitSecondTab.updateCursor(pos)
                                    },
                                    onSelectTab = { selectedTab ->
                                        if (selectedTab.id == primaryTabId) {
                                            primaryTabId = secondaryTabId
                                        }
                                        secondaryTabId = selectedTab.id
                                        activePane = "SECONDARY"
                                        editorMgr.selectTab(selectedTab.id)
                                    },
                                    onFocusPane = {
                                        activePane = "SECONDARY"
                                        editorMgr.selectTab(splitSecondTab.id)
                                    },
                                    onCloseSplit = closeSplitAction,
                                    onOpenDatabaseConsole = { file ->
                                        activeDbFile = file
                                        showDatabaseConsoleDialog = true
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            SplitSelectFileView(
                                workspaceDir = currentProject?.directory,
                                onFileSelected = { file ->
                                    val openedTab = editorMgr.openFile(file)
                                    secondaryTabId = openedTab.id
                                    activePane = "SECONDARY"
                                },
                                onCloseSplit = closeSplitAction
                            )
                        }
                    }
                }
            }

            // Drag Drop Target Overlay over Editor Container
            if (draggingTabIndex != null && draggingTabIndex!! in tabs.indices) {
                val draggedTab = tabs[draggingTabIndex!!]
                val pos = dragCurrentWindowPos
                val bounds = editorContainerBounds
                val isOverContainer = pos != null && bounds != null &&
                        pos.x in bounds.left..bounds.right && pos.y in bounds.top..bounds.bottom

                if (isOverContainer) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.25f))
                            .zIndex(10f)
                    ) {
                        if (isLandscape) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight()
                                    .fillMaxWidth(0.5f)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Splitscreen,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "Drop to Split Right with ${draggedTab.fileName}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.5f)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Splitscreen,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "Drop to Split Down with ${draggedTab.fileName}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
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

    // Floating drag badge under finger during tab drag
    if (draggingTabIndex != null && draggingTabIndex!! in tabs.indices && dragCurrentWindowPos != null) {
        val draggedTab = tabs[draggingTabIndex!!]
        val pos = dragCurrentWindowPos!!
        Popup(
            offset = IntOffset(
                (pos.x - 30).roundToInt().coerceAtLeast(0),
                (pos.y - 45).roundToInt().coerceAtLeast(0)
            )
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = getEditorFileIcon(draggedTab.fileName),
                        contentDescription = null,
                        tint = getEditorFileIconColor(draggedTab.fileName),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = draggedTab.fileName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
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

    if (showHtmlPreviewDialog && activeTab != null) {
        HtmlPreviewDialog(
            htmlContent = activeTab.content,
            file = File(activeTab.filePath),
            onDismiss = { showHtmlPreviewDialog = false }
        )
    }

    if (showJsonTreeDialog && activeTab != null) {
        JsonTreeViewerDialog(
            jsonContent = activeTab.content,
            fileName = activeTab.fileName,
            onDismiss = { showJsonTreeDialog = false }
        )
    }

    if (showColorPickerDialog && activeTab != null) {
        ColorPickerDialog(
            onDismiss = { showColorPickerDialog = false },
            onColorSelected = { hexString ->
                pendingActionType = "INSERT_TEXT:$hexString"
            }
        )
    }

    if (showJsProblemsDialog) {
        JsProblemsDialog(
            diagnostics = jsDiagnosticsList,
            onDismiss = { showJsProblemsDialog = false },
            onSelectDiagnostic = { diag ->
                showJsProblemsDialog = false
                pendingActionType = "GO_TO_LINE:${diag.line}"
            }
        )
    }

    if (showDatabaseConsoleDialog && activeDbFile != null) {
        DatabaseConsoleDialog(
            dbFile = activeDbFile!!,
            onDismiss = { showDatabaseConsoleDialog = false }
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
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        nodes.forEachIndexed { index, node ->
            if (index > 0) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }

            val isLast = index == nodes.size - 1
            val isExpanded = activeDropdownIndex == index

            Box {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isLast) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                    border = if (isLast) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)) else null,
                    modifier = Modifier.clickable { activeDropdownIndex = if (isExpanded) null else index }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
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
                                modifier = Modifier.size(15.dp)
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
                        Spacer(modifier = Modifier.width(2.dp))
                        if (node.isDirectory) {
                            Text(
                                text = "▾",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "File actions menu",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
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
    paneId: String = "primary",
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
    onCursorChange: (Int) -> Unit,
    onFocusPane: () -> Unit = {},
    onOpenDatabaseConsole: ((File) -> Unit)? = null
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
            },
            onOpenDatabaseConsole = onOpenDatabaseConsole
        )
        FileViewerType.TEXT -> {
            CodeCanvas(
                tab = tab,
                paneId = paneId,
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
                onCursorChange = onCursorChange,
                onFocusPane = onFocusPane
            )
        }
    }
}

@Composable
private fun SplitSelectFileView(
    workspaceDir: File?,
    onFileSelected: (File) -> Unit,
    onCloseSplit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val allFiles = remember(workspaceDir) {
        if (workspaceDir == null || !workspaceDir.exists()) emptyList<File>()
        else {
            try {
                workspaceDir.walkTopDown()
                    .maxDepth(4)
                    .filter { it.isFile && !it.name.startsWith(".") && it.length() < 5_000_000 }
                    .take(60)
                    .toList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    val filteredFiles = remember(allFiles, searchQuery) {
        if (searchQuery.isBlank()) allFiles
        else allFiles.filter { it.name.contains(searchQuery, ignoreCase = true) || it.path.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Splitscreen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Split Editor - Select File",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Surface(
                onClick = onCloseSplit,
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                modifier = Modifier.height(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Split",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Cancel Split",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Search & File List
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Text(
                text = "Open a second file to edit side-by-side",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Select any workspace file below to load it into this split pane",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                placeholder = { Text("Filter workspace files...", fontSize = 12.sp) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                        }
                    }
                },
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No files matching \"$searchQuery\"" else "No files found in workspace",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredFiles) { file ->
                        val relPath = if (workspaceDir != null) {
                            file.relativeToOrSelf(workspaceDir).path
                        } else file.name

                        Surface(
                            onClick = { onFileSelected(file) },
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = getEditorFileIcon(file.name),
                                    contentDescription = null,
                                    tint = getEditorFileIconColor(file.name),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = relPath,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SplitEditorViewPane(
    tab: EditorTab,
    allTabs: List<EditorTab>,
    isFocused: Boolean,
    paneTitle: String,
    paneId: String,
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
    onCursorChange: (Int) -> Unit,
    onSelectTab: (EditorTab) -> Unit,
    onFocusPane: () -> Unit,
    onCloseSplit: (() -> Unit)? = null,
    onOpenDatabaseConsole: ((File) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showTabDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable { onFocusPane() }
    ) {
        // Pane Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .background(
                    if (isFocused) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.surface
                )
                .border(
                    1.dp,
                    if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Focus / Pane Badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = paneTitle,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }

                Box {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                        modifier = Modifier.clickable {
                            onFocusPane()
                            showTabDropdown = true
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = getEditorFileIcon(tab.fileName),
                                contentDescription = null,
                                tint = getEditorFileIconColor(tab.fileName),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = tab.fileName + (if (tab.isModified) " ●" else ""),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "▾",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showTabDropdown,
                        onDismissRequest = { showTabDropdown = false }
                    ) {
                        allTabs.forEach { itemTab ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = getEditorFileIcon(itemTab.fileName),
                                            contentDescription = null,
                                            tint = getEditorFileIconColor(itemTab.fileName),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = itemTab.fileName,
                                            fontWeight = if (itemTab == tab) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                },
                                onClick = {
                                    onSelectTab(itemTab)
                                    showTabDropdown = false
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Search Workspace Files...",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            onClick = {
                                showTabDropdown = false
                                onOpenWorkspaceSearch()
                            }
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (tab.isModified) {
                    Surface(
                        onClick = {
                            try {
                                EditorManager.getInstance().saveTab(tab)
                                onSaveRequested()
                            } catch (e: Exception) {
                                android.util.Log.e("EditorView", "Failed saving pane tab", e)
                            }
                        },
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Save Pane Tab",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                if (onCloseSplit != null) {
                    Surface(
                        onClick = onCloseSplit,
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Split",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Close",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val boundPaneTab = tab
            EditorTabContent(
                tab = boundPaneTab,
                paneId = paneId,
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
                onContentChange = { newText ->
                    EditorManager.getInstance().updateTabContentByPath(boundPaneTab.filePath, newText)
                },
                onCursorChange = onCursorChange,
                onFocusPane = onFocusPane,
                onOpenDatabaseConsole = onOpenDatabaseConsole
            )
        }
    }
}

@Composable
private fun CodeCanvas(
    tab: EditorTab,
    paneId: String = "primary",
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
    onCursorChange: (Int) -> Unit,
    onFocusPane: () -> Unit = {}
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
            val selStart = tab.selectionStart.coerceIn(0, tab.content.length)
            val selEnd = tab.selectionEnd.coerceIn(0, tab.content.length)
            val textValue = TextFieldValue(
                text = tab.content,
                selection = if (tab.selectionStart != tab.selectionEnd) {
                    TextRange(selStart, selEnd)
                } else {
                    TextRange(tab.cursorPosition.coerceIn(0, tab.content.length))
                }
            )

            when {
                act == "CUT" -> EditorActionsHandler.cut(context, tab, textValue) {
                    onContentChange(it.text)
                    tab.requestSelection(it.selection.start, it.selection.end)
                    onCursorChange(it.selection.start)
                }
                act == "COPY" -> EditorActionsHandler.copy(context, tab, textValue)
                act == "PASTE" -> EditorActionsHandler.paste(context, textValue) {
                    onContentChange(it.text)
                    tab.requestSelection(it.selection.start, it.selection.end)
                    onCursorChange(it.selection.start)
                }
                act == "PASTE_PLAIN" -> EditorActionsHandler.pastePlain(context, textValue) {
                    onContentChange(it.text)
                    tab.requestSelection(it.selection.start, it.selection.end)
                    onCursorChange(it.selection.start)
                }
                act == "SELECT_ALL" -> EditorActionsHandler.selectAll(textValue) {
                    tab.requestSelection(it.selection.start, it.selection.end)
                }
                act == "SELECT_LINE" -> EditorActionsHandler.selectLine(textValue) {
                    tab.requestSelection(it.selection.start, it.selection.end)
                }
                act == "DUPLICATE_LINE" -> EditorActionsHandler.duplicateLineOrSelection(textValue) {
                    onContentChange(it.text)
                    tab.requestSelection(it.selection.start, it.selection.end)
                    onCursorChange(it.selection.start)
                }
                act == "DELETE_LINE" -> EditorActionsHandler.deleteLine(textValue) {
                    onContentChange(it.text)
                    tab.requestSelection(it.selection.start, it.selection.end)
                    onCursorChange(it.selection.start)
                }
                act == "JOIN_LINES" -> EditorActionsHandler.joinLines(textValue) {
                    onContentChange(it.text)
                    tab.requestSelection(it.selection.start, it.selection.end)
                    onCursorChange(it.selection.start)
                }
                act.startsWith("GO_TO_LINE:") -> {
                    val lineNum = act.removePrefix("GO_TO_LINE:").toIntOrNull() ?: 1
                    EditorActionsHandler.goToLine(tab, lineNum) {
                        tab.requestSelection(it.selection.start, it.selection.end)
                        onCursorChange(it.selection.start)
                    }
                }
                act.startsWith("INSERT_TEXT:") -> {
                    val insertStr = act.removePrefix("INSERT_TEXT:")
                    val curPos = tab.cursorPosition.coerceIn(0, tab.content.length)
                    val newText = StringBuilder(tab.content).insert(curPos, insertStr).toString()
                    onContentChange(newText)
                    val newPos = curPos + insertStr.length
                    tab.requestSelection(newPos, newPos)
                    onCursorChange(newPos)
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
                                val query = tab.findQuery
                                if (query.isNotEmpty() && tab.content.contains(query, ignoreCase = true)) {
                                    val searchFrom = (tab.selectionStart - 1).coerceAtLeast(0)
                                    val prevPos = tab.content.lastIndexOf(query, searchFrom, ignoreCase = true)
                                        .let { if (it < 0) tab.content.lastIndexOf(query, ignoreCase = true) else it }
                                    if (prevPos >= 0) {
                                        tab.requestSelection(prevPos, prevPos + query.length)
                                        onCursorChange(prevPos)
                                    }
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Previous", modifier = Modifier.size(16.dp))
                        }

                        IconButton(
                            onClick = {
                                val query = tab.findQuery
                                if (query.isNotEmpty() && tab.content.contains(query, ignoreCase = true)) {
                                    val searchFrom = tab.selectionEnd.coerceAtLeast(tab.cursorPosition + 1)
                                    val nextPos = tab.content.indexOf(query, searchFrom, ignoreCase = true)
                                        .let { if (it < 0) tab.content.indexOf(query, 0, ignoreCase = true) else it }
                                    if (nextPos >= 0) {
                                        tab.requestSelection(nextPos, nextPos + query.length)
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
                                    val query = tab.findQuery
                                    if (query.isNotEmpty()) {
                                        val s = minOf(tab.selectionStart, tab.selectionEnd)
                                        val e = maxOf(tab.selectionStart, tab.selectionEnd)
                                        val selectedMatches = (s != e && e <= tab.content.length &&
                                                tab.content.substring(s, e).equals(query, ignoreCase = true))
                                        if (selectedMatches) {
                                            val newText = tab.content.substring(0, s) + tab.replaceQuery + tab.content.substring(e)
                                            onContentChange(newText)
                                            val nextSearchFrom = s + tab.replaceQuery.length
                                            val nextPos = newText.indexOf(query, nextSearchFrom, ignoreCase = true)
                                                .let { if (it < 0) newText.indexOf(query, 0, ignoreCase = true) else it }
                                            if (nextPos >= 0) {
                                                tab.requestSelection(nextPos, nextPos + query.length)
                                            } else {
                                                tab.requestSelection(nextSearchFrom, nextSearchFrom)
                                            }
                                        } else {
                                            val nextPos = tab.content.indexOf(query, tab.cursorPosition, ignoreCase = true)
                                                .let { if (it < 0) tab.content.indexOf(query, 0, ignoreCase = true) else it }
                                            if (nextPos >= 0) {
                                                tab.requestSelection(nextPos, nextPos + query.length)
                                            }
                                        }
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
        key("${paneId}_${tab.filePath}") {
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
                },
                onSaveRequested = onSaveRequested,
                onUndoRequested = { editorMgr.undoTab(tab) },
                onRedoRequested = { editorMgr.redoTab(tab) },
                onOpenCommandPalette = onOpenCommandPalette,
                onOpenWorkspaceSearch = onOpenWorkspaceSearch,
                onOpenFind = {
                    tab.showFindBar = true
                    tab.showReplaceBar = false
                },
                onResetModifiers = onResetModifiers,
                onEditorFocus = onFocusPane,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("code_editor_text_input")
            )
        }
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
    onForceOpenAsText: () -> Unit,
    onOpenDatabaseConsole: ((File) -> Unit)? = null
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

                val isDatabase = file.extension.lowercase() in listOf("db", "sqlite", "sqlite3")
                if (isDatabase && onOpenDatabaseConsole != null) {
                    Button(
                        onClick = { onOpenDatabaseConsole(file) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open in SQLite Console", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

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

