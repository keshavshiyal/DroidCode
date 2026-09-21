package com.example.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
import com.example.editor.EditorManager
import com.example.editor.EditorTab
import com.example.settings.AppSettings

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
                            imageVector = getEditorFileIcon(tab.extension),
                            contentDescription = null,
                            tint = getEditorFileIconColor(tab.extension),
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

            // Code Canvas
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

    var textFieldValue by remember(tab.id, tab.content) {
        mutableStateOf(
            TextFieldValue(
                text = tab.content,
                selection = TextRange(tab.cursorPosition)
            )
        )
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
                    .width(44.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .verticalScroll(verticalScroll)
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.End
            ) {
                for (i in 1..lineCount) {
                    Text(
                        text = i.toString(),
                        fontSize = settings.fontSizeSp.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        lineHeight = (settings.fontSizeSp * 1.4).sp
                    )
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
private fun getEditorFileIcon(ext: String) = FileIconUtils.getFileIcon(ext)

@Composable
private fun getEditorFileIconColor(ext: String) = FileIconUtils.getFileIconColor(ext)

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
