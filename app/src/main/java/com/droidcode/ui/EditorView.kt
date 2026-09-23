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
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import com.droidcode.R
import com.droidcode.ui.theme.CornerLarge
import com.droidcode.ui.theme.CornerMedium
import com.droidcode.ui.theme.CornerSmall
import com.droidcode.ui.theme.SpacingL
import com.droidcode.ui.theme.SpacingM
import com.droidcode.ui.theme.SpacingS
import com.droidcode.ui.theme.SpacingXL
import com.droidcode.ui.theme.SpacingXS
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.droidcode.editor.EditorManager
import com.droidcode.editor.EditorTab
import com.droidcode.editor.FileViewerType
import com.droidcode.filesystem.LocalFileSystem
import com.droidcode.settings.AppSettings
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.MoreVert
import com.droidcode.language.SyntaxHighlighter
import com.droidcode.project.WorkspaceManager

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
        // Tab Bar
        if (tabs.isNotEmpty()) {
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

            // Code Canvas or Media Viewer
            when (activeTab.viewerType) {
                FileViewerType.IMAGE -> ImageViewer(file = activeTab.file)
                FileViewerType.VIDEO -> VideoViewer(file = activeTab.file)
                FileViewerType.PDF -> PdfViewer(file = activeTab.file)
                FileViewerType.UNSUPPORTED -> UnsupportedFileViewer(
                    tab = activeTab,
                    onForceOpenAsText = {
                        try {
                            val text = activeTab.file.readText()
                            activeTab.forceOpenAsText(text)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot read file as text: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                FileViewerType.TEXT -> {
                    CodeCanvas(
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

    var textFieldValue by rememberSaveable(tab.filePath, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue(
                text = tab.content,
                selection = TextRange(tab.cursorPosition.coerceIn(0, tab.content.length))
            )
        )
    }

    LaunchedEffect(tab.content) {
        if (tab.content != textFieldValue.text) {
            textFieldValue = TextFieldValue(
                text = tab.content,
                selection = TextRange(tab.cursorPosition.coerceIn(0, tab.content.length))
            )
        }
    }

    LaunchedEffect(pendingActionType) {
        if (pendingActionType != null) {
            val act = pendingActionType
            onClearPendingAction()

            when {
                act == "CUT" -> EditorActionsHandler.cut(context, tab, textFieldValue) {
                    textFieldValue = it; onContentChange(it.text); onCursorChange(it.selection.start)
                }
                act == "COPY" -> EditorActionsHandler.copy(context, tab, textFieldValue)
                act == "PASTE" -> EditorActionsHandler.paste(context, textFieldValue) {
                    textFieldValue = it; onContentChange(it.text); onCursorChange(it.selection.start)
                }
                act == "PASTE_PLAIN" -> EditorActionsHandler.pastePlain(context, textFieldValue) {
                    textFieldValue = it; onContentChange(it.text); onCursorChange(it.selection.start)
                }
                act == "SELECT_ALL" -> EditorActionsHandler.selectAll(textFieldValue) {
                    textFieldValue = it
                }
                act == "SELECT_LINE" -> EditorActionsHandler.selectLine(textFieldValue) {
                    textFieldValue = it
                }
                act == "DUPLICATE_LINE" -> EditorActionsHandler.duplicateLineOrSelection(textFieldValue) {
                    textFieldValue = it; onContentChange(it.text); onCursorChange(it.selection.start)
                }
                act == "DELETE_LINE" -> EditorActionsHandler.deleteLine(textFieldValue) {
                    textFieldValue = it; onContentChange(it.text); onCursorChange(it.selection.start)
                }
                act == "JOIN_LINES" -> EditorActionsHandler.joinLines(textFieldValue) {
                    textFieldValue = it; onContentChange(it.text); onCursorChange(it.selection.start)
                }
                act.startsWith("GO_TO_LINE:") -> {
                    val lineNum = act.removePrefix("GO_TO_LINE:").toIntOrNull() ?: 1
                    EditorActionsHandler.goToLine(tab, lineNum) {
                        textFieldValue = it; onCursorChange(it.selection.start)
                    }
                }
                act == "GO_DEFINITION" || act == "GO_DECLARATION" -> {
                    EditorActionsHandler.goToDefinition(context, tab, textFieldValue) {
                        textFieldValue = it; onCursorChange(it.selection.start)
                    }
                }
                act == "FORMAT_DOC" -> EditorActionsHandler.formatDocument(tab) {
                    textFieldValue = it; onContentChange(it.text); onCursorChange(it.selection.start)
                }
                act == "TOGGLE_COMMENT" -> EditorActionsHandler.toggleCommentLine(tab, textFieldValue) {
                    textFieldValue = it; onContentChange(it.text); onCursorChange(it.selection.start)
                }
                act == "INDENT" -> EditorActionsHandler.indent(textFieldValue) {
                    textFieldValue = it; onContentChange(it.text); onCursorChange(it.selection.start)
                }
                act == "OUTDENT" -> EditorActionsHandler.outdent(textFieldValue) {
                    textFieldValue = it; onContentChange(it.text); onCursorChange(it.selection.start)
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

    val lineStarts = remember(textFieldValue.text) {
        val starts = ArrayList<Int>()
        starts.add(0)
        val text = textFieldValue.text
        for (i in 0 until text.length) {
            if (text[i] == '\n') {
                starts.add(i + 1)
            }
        }
        starts.toIntArray()
    }
    val lineCount = lineStarts.size
    val verticalScroll = rememberSaveable(tab.filePath, saver = ScrollState.Saver) {
        ScrollState(initial = 0)
    }
    val horizontalScroll = rememberSaveable(tab.filePath, saver = ScrollState.Saver) {
        ScrollState(initial = 0)
    }
    val isDark = isSystemInDarkTheme()
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

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
                                if (tab.findQuery.isNotEmpty() && textFieldValue.text.contains(tab.findQuery, true)) {
                                    val nextPos = textFieldValue.text.indexOf(tab.findQuery, textFieldValue.selection.start + 1, true)
                                        .let { if (it < 0) textFieldValue.text.indexOf(tab.findQuery, ignoreCase = true) else it }
                                    if (nextPos >= 0) {
                                        textFieldValue = textFieldValue.copy(selection = TextRange(nextPos, nextPos + tab.findQuery.length))
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
                                        val newText = textFieldValue.text.replaceFirst(tab.findQuery, tab.replaceQuery, ignoreCase = true)
                                        textFieldValue = TextFieldValue(newText, TextRange(textFieldValue.selection.start))
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
                                        val newText = textFieldValue.text.replace(tab.findQuery, tab.replaceQuery, ignoreCase = true)
                                        textFieldValue = TextFieldValue(newText, TextRange(0))
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

        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            // Line Numbers Gutter
            if (settings.isLineNumbersEnabled) {
                val density = LocalDensity.current
                val layout = textLayoutResult
                val primaryColor = MaterialTheme.colorScheme.primary
                val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
                val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer

                val textPaint = remember(activeFontFamily, settings.fontSizeSp, density) {
                    android.graphics.Paint().apply {
                        isAntiAlias = true
                        textSize = with(density) { settings.fontSizeSp.sp.toPx() }
                        textAlign = android.graphics.Paint.Align.RIGHT
                    }
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(46.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 4.dp)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val scrollY = verticalScroll.value.toFloat()
                    val topPaddingPx = with(density) { 8.dp.toPx() }

                    if (layout != null && layout.lineCount > 0) {
                        val visualLineCount = layout.lineCount
                        val firstVisual = layout.getLineForVerticalPosition(scrollY).coerceIn(0, visualLineCount - 1)
                        val lastVisual = layout.getLineForVerticalPosition(scrollY + canvasHeight).coerceIn(firstVisual, visualLineCount - 1)

                        val fullText = textFieldValue.text
                        for (vLine in firstVisual..lastVisual) {
                            val startOffset = layout.getLineStart(vLine)
                            val isLogicalLineStart = (vLine == 0 || (startOffset > 0 && startOffset <= fullText.length && fullText[startOffset - 1] == '\n'))

                            if (isLogicalLineStart) {
                                val lineIndex = lineStarts.binarySearch(startOffset).let { if (it < 0) -it - 2 else it }.coerceAtLeast(0)
                                val lineNumber = lineIndex + 1
                                val isActiveLine = (lineNumber == tab.line)

                                val lineTop = layout.getLineTop(vLine) - scrollY + topPaddingPx
                                val lineBottom = layout.getLineBottom(vLine) - scrollY + topPaddingPx
                                val lineHeight = (lineBottom - lineTop).coerceAtLeast(1f)

                                if (isActiveLine) {
                                    drawRoundRect(
                                        color = primaryContainerColor.copy(alpha = 0.6f),
                                        topLeft = Offset(0f, lineTop),
                                        size = Size(canvasWidth, lineHeight),
                                        cornerRadius = CornerRadius(6f, 6f)
                                    )
                                    textPaint.color = primaryColor.toArgb()
                                    textPaint.isFakeBoldText = true
                                } else {
                                    textPaint.color = onSurfaceVariantColor.copy(alpha = 0.5f).toArgb()
                                    textPaint.isFakeBoldText = false
                                }

                                val fontMetrics = textPaint.fontMetrics
                                val baseline = lineTop + (lineHeight - (fontMetrics.descent - fontMetrics.ascent)) / 2f - fontMetrics.ascent
                                drawContext.canvas.nativeCanvas.drawText(
                                    lineNumber.toString(),
                                    canvasWidth - 4f,
                                    baseline,
                                    textPaint
                                )
                            }
                        }
                    } else {
                        val lineHeightPx = with(density) { (settings.fontSizeSp * 1.4).sp.toPx() }
                        val firstLine = ((scrollY - topPaddingPx) / lineHeightPx).toInt().coerceAtLeast(0)
                        val lastLine = (((scrollY + canvasHeight) - topPaddingPx) / lineHeightPx).toInt().coerceAtMost(lineCount - 1)

                        for (i in firstLine..lastLine) {
                            val lineNumber = i + 1
                            val isActiveLine = (lineNumber == tab.line)
                            val lineTop = i * lineHeightPx - scrollY + topPaddingPx

                            if (isActiveLine) {
                                drawRoundRect(
                                    color = primaryContainerColor.copy(alpha = 0.6f),
                                    topLeft = Offset(0f, lineTop),
                                    size = Size(canvasWidth, lineHeightPx),
                                    cornerRadius = CornerRadius(6f, 6f)
                                )
                                textPaint.color = primaryColor.toArgb()
                                textPaint.isFakeBoldText = true
                            } else {
                                textPaint.color = onSurfaceVariantColor.copy(alpha = 0.5f).toArgb()
                                textPaint.isFakeBoldText = false
                            }

                            val fontMetrics = textPaint.fontMetrics
                            val baseline = lineTop + (lineHeightPx - (fontMetrics.descent - fontMetrics.ascent)) / 2f - fontMetrics.ascent
                            drawContext.canvas.nativeCanvas.drawText(
                                lineNumber.toString(),
                                canvasWidth - 4f,
                                baseline,
                                textPaint
                            )
                        }
                    }
                }
            }

            // Main Editor Canvas Input
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScroll)
                    .then(if (!settings.isWordWrap) Modifier.horizontalScroll(horizontalScroll) else Modifier)
                    .padding(8.dp)
            ) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        if (ctrlActive) {
                            val oldText = textFieldValue.text
                            val newText = newValue.text
                            val addedChar = if (newText.length > oldText.length) {
                                val selStart = newValue.selection.start
                                if (selStart > 0 && selStart <= newText.length) {
                                    newText.substring(selStart - 1, selStart)
                                } else ""
                            } else ""

                            when (addedChar.lowercase()) {
                                "s" -> {
                                    try {
                                        editorMgr.saveActiveTab()
                                        onSaveRequested()
                                    } catch (e: Exception) {
                                        android.util.Log.e("EditorView", "Failed saving file from quick bar", e)
                                        Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                    onResetModifiers()
                                }
                                "z" -> {
                                    editorMgr.undoActiveTab()
                                    onResetModifiers()
                                }
                                "y" -> {
                                    editorMgr.redoActiveTab()
                                    onResetModifiers()
                                }
                                "p" -> {
                                    onOpenCommandPalette()
                                    onResetModifiers()
                                }
                                else -> {
                                    onResetModifiers()
                                }
                            }
                        } else {
                            textFieldValue = newValue
                            tab.updateSelection(newValue.selection.start, newValue.selection.end)
                            onContentChange(newValue.text)
                            onCursorChange(newValue.selection.start)
                        }
                    },
                    onTextLayout = { textLayoutResult = it },
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = settings.fontSizeSp.sp,
                        fontFamily = activeFontFamily,
                        lineHeight = (settings.fontSizeSp * 1.4).sp
                    ),
                    visualTransformation = CodeSyntaxVisualTransformation(tab.languageId, isDark),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxSize()
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown) {
                                if (keyEvent.key == Key.Tab) {
                                    val currentText = textFieldValue.text
                                    val sel = textFieldValue.selection
                                    val indentStr = "    " // Standard 4 spaces
                                    val newText = currentText.substring(0, sel.start) + indentStr + currentText.substring(sel.end)
                                    val newPos = sel.start + indentStr.length
                                    textFieldValue = TextFieldValue(newText, TextRange(newPos))
                                    onContentChange(newText)
                                    onCursorChange(newPos)
                                    true
                                } else if (keyEvent.key == Key.Enter) {
                                    val currentText = textFieldValue.text
                                    val sel = textFieldValue.selection
                                    val lineStart = currentText.lastIndexOf('\n', (sel.start - 1).coerceAtLeast(0)) + 1
                                    val currentLine = currentText.substring(lineStart, sel.start)
                                    val indent = currentLine.takeWhile { it == ' ' || it == '\t' }
                                    val newText = currentText.substring(0, sel.start) + "\n" + indent + currentText.substring(sel.end)
                                    val newPos = sel.start + 1 + indent.length
                                    textFieldValue = TextFieldValue(newText, TextRange(newPos))
                                    onContentChange(newText)
                                    onCursorChange(newPos)
                                    true
                                } else {
                                    val isCtrl = ctrlActive || keyEvent.isCtrlPressed
                                    if (isCtrl) {
                                        when (keyEvent.key) {
                                            Key.S -> {
                                                try {
                                                    editorMgr.saveActiveTab()
                                                    onSaveRequested()
                                                } catch (e: Exception) {
                                                    android.util.Log.e("EditorView", "Failed saving file from shortcut", e)
                                                    Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                                onResetModifiers()
                                                true
                                            }
                                            Key.Z -> {
                                                editorMgr.undoActiveTab()
                                                onResetModifiers()
                                                true
                                            }
                                            Key.Y -> {
                                                editorMgr.redoActiveTab()
                                                onResetModifiers()
                                                true
                                            }
                                            Key.P -> {
                                                onOpenCommandPalette()
                                                onResetModifiers()
                                                true
                                            }
                                            Key.F -> {
                                                tab.showFindBar = true
                                                tab.showReplaceBar = false
                                                onResetModifiers()
                                                true
                                            }
                                            Key.H -> {
                                                tab.showFindBar = true
                                                tab.showReplaceBar = true
                                                onResetModifiers()
                                                true
                                            }
                                            else -> false
                                        }
                                    } else false
                                }
                            } else false
                        }
                        .testTag("code_editor_text_input")
                )
            }
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
    return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

class CodeSyntaxVisualTransformation(
    private val languageId: String,
    private val isDarkTheme: Boolean
) : VisualTransformation {

    companion object {
        private val wordRegex = Regex("\\b[A-Za-z_][A-Za-z0-9_]*\\b")
        private val stringRegex = Regex("\"[^\"]*\"|'[^']*'|`[^`]*`")
        private val numberRegex = Regex("\\b\\d+(\\.\\d+)?\\b")
        private val commentRegex = Regex("//.*|/\\*[\\s\\S]*?\\*/|#.*")
    }

    private var cachedInputText: String? = null
    private var cachedTransformedText: TransformedText? = null

    override fun filter(text: AnnotatedString): TransformedText {
        if (text.text == cachedInputText && cachedTransformedText != null) {
            return cachedTransformedText!!
        }

        val code = text.text
        if (code.isEmpty()) {
            val empty = TransformedText(text, OffsetMapping.Identity)
            cachedInputText = code
            cachedTransformedText = empty
            return empty
        }

        // Limit highlighting for large codebases to avoid locking frame budget
        val shouldHighlight = code.length <= 40000

        val highlighted = buildAnnotatedString {
            append(code)
            if (!shouldHighlight) return@buildAnnotatedString

            val keywordColor = if (isDarkTheme) Color(0xFFCF92D7) else Color(0xFF8E24AA)
            val stringColor = if (isDarkTheme) Color(0xFF81C784) else Color(0xFF2E7D32)
            val numberColor = if (isDarkTheme) Color(0xFFFFB74D) else Color(0xFFE65100)
            val commentColor = if (isDarkTheme) Color(0xFF78909C) else Color(0xFF546E7A)
            val typeColor = if (isDarkTheme) Color(0xFF64B5F6) else Color(0xFF1565C0)

            val keywords = SyntaxHighlighter.getKeywordsForLanguage(languageId)

            for (match in commentRegex.findAll(code)) {
                addStyle(SpanStyle(color = commentColor, fontWeight = FontWeight.Normal), match.range.first, match.range.last + 1)
            }

            for (match in stringRegex.findAll(code)) {
                addStyle(SpanStyle(color = stringColor), match.range.first, match.range.last + 1)
            }

            for (match in numberRegex.findAll(code)) {
                addStyle(SpanStyle(color = numberColor), match.range.first, match.range.last + 1)
            }

            for (match in wordRegex.findAll(code)) {
                val word = match.value
                if (keywords.contains(word) || keywords.contains(word.lowercase())) {
                    addStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
                } else if (word.first().isUpperCase()) {
                    addStyle(SpanStyle(color = typeColor, fontWeight = FontWeight.Medium), match.range.first, match.range.last + 1)
                }
            }
        }
        val result = TransformedText(highlighted, OffsetMapping.Identity)
        cachedInputText = code
        cachedTransformedText = result
        return result
    }
}
