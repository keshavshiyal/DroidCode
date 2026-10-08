package com.droidcode.editor.engine

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.viewinterop.AndroidView
import com.droidcode.editor.EditorTab
import com.droidcode.editor.LineDiffStatus
import com.droidcode.settings.AppSettings

/**
 * Jetpack Compose wrapper for the native virtualized DroidCodeEngine.
 *
 * Replaces non-virtualized BasicTextField with our custom 120 FPS hardware-accelerated
 * CodeEditorView, allowing frictionless editing across 100,000+ line codebases.
 */
@Composable
fun DroidCodeEditor(
    tab: EditorTab,
    settings: AppSettings,
    ctrlActive: Boolean = false,
    shiftActive: Boolean = false,
    altActive: Boolean = false,
    lineDiffMap: Map<Int, LineDiffStatus> = emptyMap(),
    onContentChange: (String) -> Unit,
    onCursorChange: (line: Int, col: Int) -> Unit,
    onSaveRequested: () -> Unit = {},
    onUndoRequested: () -> Unit = {},
    onRedoRequested: () -> Unit = {},
    onOpenCommandPalette: () -> Unit = {},
    onOpenWorkspaceSearch: () -> Unit = {},
    onOpenFind: () -> Unit = {},
    onResetModifiers: () -> Unit = {},
    onEditorFocus: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val theme = remember(settings.themeMode, isDark) {
        EditorTheme.forThemeMode(settings.themeMode, isDark)
    }

    AndroidView(
        modifier = modifier.clipToBounds(),
        factory = { ctx ->
            CodeEditorView(ctx).apply {
                this.theme = theme
                this.languageId = tab.languageId
                this.fontOption = settings.editorFontFamily
                this.fontSizeSp = settings.fontSizeSp.toFloat()
                this.isLineNumbersEnabled = settings.isLineNumbersEnabled
                this.isWordWrap = settings.isWordWrap
                this.lineDiffMap = lineDiffMap
                this.ctrlActive = ctrlActive
                this.shiftActive = shiftActive
                this.altActive = altActive
                this.searchQuery = if (tab.showFindBar) tab.findQuery else null
                this.setBufferText(tab.content, tab.filePath, tab.contentVersion)

                this.onFileContentChanged = { path, text ->
                    if (path == tab.filePath) {
                        onContentChange(text)
                    }
                }
                this.onContentChanged = { text ->
                    if (this.currentFilePath == tab.filePath) {
                        onContentChange(text)
                    }
                }
                this.onCursorChanged = onCursorChange
                this.onScrollPositionChanged = { x, y ->
                    tab.scrollX = x
                    tab.scrollY = y
                }
                this.onSelectionChanged = { start, end ->
                    tab.updateSelection(start, end)
                }
                this.onSaveShortcut = onSaveRequested
                this.onUndoShortcut = onUndoRequested
                this.onRedoShortcut = onRedoRequested
                this.onCommandPaletteShortcut = onOpenCommandPalette
                this.onWorkspaceSearchShortcut = onOpenWorkspaceSearch
                this.onFindShortcut = onOpenFind
                this.onResetModifiers = onResetModifiers
                this.onEditorFocus = onEditorFocus

                if (tab.line > 0 && tab.column > 0) {
                    this.setCursorPosition(CursorPos(tab.line - 1, tab.column - 1))
                }
                if (tab.selectionStart != tab.selectionEnd) {
                    this.setSelectionOffsets(tab.selectionStart, tab.selectionEnd)
                }
                if (tab.scrollX > 0 || tab.scrollY > 0) {
                    this.setScrollPositions(tab.scrollX, tab.scrollY)
                }
            }
        },
        update = { view ->
            view.theme = theme
            view.languageId = tab.languageId
            view.fontOption = settings.editorFontFamily
            view.fontSizeSp = settings.fontSizeSp.toFloat()
            view.isLineNumbersEnabled = settings.isLineNumbersEnabled
            view.isWordWrap = settings.isWordWrap
            view.lineDiffMap = lineDiffMap
            view.ctrlActive = ctrlActive
            view.shiftActive = shiftActive
            view.altActive = altActive
            view.searchQuery = if (tab.showFindBar) tab.findQuery else null

            // Sync buffer if file changed or external content change
            val isDifferentFile = view.currentFilePath != tab.filePath
            val isContentChanged = view.lastSyncedContentVersion != tab.contentVersion
            if (isDifferentFile) {
                view.setBufferText(tab.content, tab.filePath, tab.contentVersion)
                if (tab.scrollX > 0 || tab.scrollY > 0) {
                    view.setScrollPositions(tab.scrollX, tab.scrollY)
                }
                if (tab.line > 0 && tab.column > 0) {
                    view.setCursorPosition(CursorPos(tab.line - 1, tab.column - 1))
                }
            } else if (isContentChanged) {
                view.setBufferText(tab.content, tab.filePath, tab.contentVersion)
            }

            view.onFileContentChanged = { path, text ->
                if (path == tab.filePath) {
                    onContentChange(text)
                }
            }
            view.onContentChanged = { text ->
                if (view.currentFilePath == tab.filePath) {
                    onContentChange(text)
                }
            }
            view.onCursorChanged = onCursorChange
            view.onScrollPositionChanged = { x, y ->
                tab.scrollX = x
                tab.scrollY = y
            }
            view.onSelectionChanged = { start, end ->
                tab.updateSelection(start, end)
            }
            view.onSaveShortcut = onSaveRequested
            view.onUndoShortcut = onUndoRequested
            view.onRedoShortcut = onRedoRequested
            view.onCommandPaletteShortcut = onOpenCommandPalette
            view.onWorkspaceSearchShortcut = onOpenWorkspaceSearch
            view.onFindShortcut = onOpenFind
            view.onResetModifiers = onResetModifiers
            view.onEditorFocus = onEditorFocus

            // Handle pending selection requests (Find Next, Go To Line, Select All, etc.)
            val pendingReq = tab.pendingSelectionRequest
            if (pendingReq != null && pendingReq.requestVersion != view.lastAppliedSelectionVersion) {
                view.lastAppliedSelectionVersion = pendingReq.requestVersion
                view.setSelectionOffsets(pendingReq.start, pendingReq.end)
                view.scrollToCursor()
            } else if (!view.isFocused) {
                // Invariant: when view is focused, view is the sole authority for cursor & selection.
                // Never overwrite the active typing cursor with stale Compose values.
                if (tab.selectionStart != tab.selectionEnd) {
                    val (viewStart, viewEnd) = view.getSelectionOffsets()
                    if (tab.selectionStart != viewStart || tab.selectionEnd != viewEnd) {
                        view.setSelectionOffsets(tab.selectionStart, tab.selectionEnd)
                    }
                } else if (tab.line > 0 && tab.column > 0) {
                    val targetPos = CursorPos(tab.line - 1, tab.column - 1)
                    if (view.cursorPosition != targetPos) {
                        view.setCursorPosition(targetPos)
                    }
                }
            }
        },
        onRelease = { view ->
            view.flushContent()
        }
    )
}
