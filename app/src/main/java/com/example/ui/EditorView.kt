package com.example.ui

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
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
import com.example.editor.EditorManager
import com.example.editor.EditorTab
import com.example.editor.FileViewerType
import com.example.filesystem.LocalFileSystem
import com.example.settings.AppSettings
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun EditorView(
    settings: AppSettings,
    ctrlActive: Boolean = false,
    shiftActive: Boolean = false,
    altActive: Boolean = false,
    onResetModifiers: () -> Unit = {},
    onOpenCommandPalette: () -> Unit = {},
    onSaveRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    val editorMgr = remember { EditorManager.getInstance() }
    val tabs = editorMgr.tabs
    val activeTab = editorMgr.activeTab

    var tabToPromptCloseIndex by remember { mutableStateOf<Int?>(null) }

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
                    val border = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(bg)
                            .border(1.dp, border)
                            .clickable { editorMgr.activeTabIndex = index }
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
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close tab",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                title = { Text("Unsaved Changes") },
                text = { Text("Do you want to save changes to '${tabToClose.fileName}' before closing?") },
                confirmButton = {
                    Button(
                        onClick = {
                            try {
                                editorMgr.saveTab(tabToClose)
                                onSaveRequested()
                            } catch (e: Exception) {}
                            editorMgr.closeTab(tabToPromptCloseIndex!!)
                            tabToPromptCloseIndex = null
                        }
                    ) { Text("Save") }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = {
                                editorMgr.closeTab(tabToPromptCloseIndex!!)
                                tabToPromptCloseIndex = null
                            }
                        ) { Text("Don't Save") }
                        Spacer(modifier = Modifier.width(4.dp))
                        TextButton(
                            onClick = { tabToPromptCloseIndex = null }
                        ) { Text("Cancel") }
                    }
                }
            )
        }

        // Editor Toolbar & Language / Line Stats
        if (activeTab != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
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
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Ln ${activeTab.line}, Col ${activeTab.column}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { editorMgr.undoActiveTab() },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = { editorMgr.redoActiveTab() },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            try {
                                editorMgr.saveActiveTab()
                                onSaveRequested()
                            } catch (e: Exception) {}
                        },
                        modifier = Modifier
                            .size(26.dp)
                            .testTag("editor_save_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save",
                            tint = if (activeTab.isModified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Code Canvas or Media Viewer
            when (activeTab.viewerType) {
                FileViewerType.IMAGE -> ImageViewer(file = activeTab.file)
                FileViewerType.VIDEO -> VideoViewer(file = activeTab.file)
                FileViewerType.PDF -> PdfViewer(file = activeTab.file)
                FileViewerType.UNSUPPORTED -> UnsupportedFileViewer(file = activeTab.file)
                FileViewerType.TEXT -> {
                    CodeCanvas(
                        tab = activeTab,
                        settings = settings,
                        ctrlActive = ctrlActive,
                        shiftActive = shiftActive,
                        altActive = altActive,
                        onResetModifiers = onResetModifiers,
                        onOpenCommandPalette = onOpenCommandPalette,
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
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No file open",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Open Project Explorer drawer to select a file",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
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
    onResetModifiers: () -> Unit,
    onOpenCommandPalette: () -> Unit,
    onSaveRequested: () -> Unit,
    onContentChange: (String) -> Unit,
    onCursorChange: (Int) -> Unit
) {
    val editorMgr = remember { EditorManager.getInstance() }

    var textFieldValue by remember(tab.id) {
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

    val lines = textFieldValue.text.split("\n")
    val lineCount = lines.size
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()
    val isDark = isSystemInDarkTheme()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // Line Numbers Gutter
        if (settings.isLineNumbersEnabled) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(46.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .verticalScroll(verticalScroll)
                    .padding(vertical = 8.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.End
            ) {
                for (i in 1..lineCount) {
                    val isActiveLine = (i == tab.line)
                    val bgColor = if (isActiveLine) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else Color.Transparent
                    val textColor = if (isActiveLine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bgColor, RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = i.toString(),
                            fontSize = settings.fontSizeSp.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isActiveLine) FontWeight.Bold else FontWeight.Normal,
                            color = textColor,
                            lineHeight = (settings.fontSizeSp * 1.4).sp
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
                                } catch (e: Exception) {}
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
                        onContentChange(newValue.text)
                        onCursorChange(newValue.selection.start)
                    }
                },
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = settings.fontSizeSp.sp,
                    fontFamily = FontFamily.Monospace,
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
                                            try { editorMgr.saveActiveTab(); onSaveRequested() } catch (e: Exception) {}
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
                        setMediaController(MediaController(ctx).apply { setAnchorView(this@apply) })
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

    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = buildAnnotatedString {
            append(text.text)
            val code = text.text
            if (code.isEmpty()) return@buildAnnotatedString

            val keywordColor = if (isDarkTheme) Color(0xFFCF92D7) else Color(0xFF8E24AA)
            val stringColor = if (isDarkTheme) Color(0xFF81C784) else Color(0xFF2E7D32)
            val numberColor = if (isDarkTheme) Color(0xFFFFB74D) else Color(0xFFE65100)
            val commentColor = if (isDarkTheme) Color(0xFF78909C) else Color(0xFF546E7A)
            val typeColor = if (isDarkTheme) Color(0xFF64B5F6) else Color(0xFF1565C0)

            val keywords = when (languageId.lowercase()) {
                "kotlin", "java" -> setOf("package", "import", "class", "interface", "fun", "val", "var", "public", "private", "protected", "return", "if", "else", "for", "while", "when", "try", "catch", "throw", "object", "sealed", "data", "override", "final", "static", "new", "null", "true", "false", "void")
                "javascript", "typescript" -> setOf("import", "export", "from", "class", "function", "const", "let", "var", "return", "if", "else", "for", "while", "switch", "case", "async", "await", "try", "catch", "default", "null", "undefined", "true", "false")
                "python" -> setOf("def", "class", "import", "from", "return", "if", "elif", "else", "for", "while", "try", "except", "with", "as", "pass", "None", "True", "False", "lambda", "yield")
                "html", "xml" -> setOf("div", "span", "p", "a", "body", "head", "html", "script", "style", "link", "meta", "resources", "string", "layout", "manifest")
                "sql" -> setOf("SELECT", "FROM", "WHERE", "INSERT", "INTO", "UPDATE", "DELETE", "JOIN", "LEFT", "RIGHT", "CREATE", "TABLE", "PRIMARY", "KEY", "ORDER", "BY", "GROUP", "LIMIT", "AND", "OR", "NOT")
                else -> setOf("val", "var", "fun", "def", "class", "function", "return", "if", "else", "import", "public", "private")
            }

            val wordRegex = Regex("\\b[A-Za-z_][A-Za-z0-9_]*\\b")
            val stringRegex = Regex("\"[^\"]*\"|'[^']*'|`[^`]*`")
            val numberRegex = Regex("\\b\\d+(\\.\\d+)?\\b")
            val commentRegex = Regex("//.*|/\\*[\\s\\S]*?\\*/|#.*")

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
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}
