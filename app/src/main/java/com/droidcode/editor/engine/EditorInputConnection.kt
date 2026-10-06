package com.droidcode.editor.engine

import android.view.KeyEvent
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest

/**
 * Android Input Connection bridge connecting soft keyboards (Gboard, etc.) and
 * hardware keyboards directly to DroidCodeEngine's TextBuffer.
 */
class EditorInputConnection(
    private val editor: CodeEditorView
) : BaseInputConnection(editor, true) {

    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
        if (text == null) return false
        editor.insertText(text.toString())
        return true
    }

    override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
        if (text == null) return false
        editor.insertText(text.toString())
        return true
    }

    override fun finishComposingText(): Boolean {
        return true
    }

    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
        var handled = false
        if (beforeLength > 0) {
            editor.deleteBeforeCursor(beforeLength)
            handled = true
        }
        if (afterLength > 0) {
            editor.deleteAfterCursor(afterLength)
            handled = true
        }
        return if (handled) true else super.deleteSurroundingText(beforeLength, afterLength)
    }

    override fun sendKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return false
        if (event.action == KeyEvent.ACTION_DOWN) {
            val isCtrl = event.isCtrlPressed || editor.ctrlActive
            val isShift = event.isShiftPressed || editor.shiftActive

            if (isCtrl) {
                if (isShift && event.keyCode == KeyEvent.KEYCODE_F) {
                    editor.onWorkspaceSearchShortcut?.invoke()
                    editor.onResetModifiers?.invoke()
                    return true
                }
                when (event.keyCode) {
                    KeyEvent.KEYCODE_A -> {
                        editor.selectAll()
                        editor.onResetModifiers?.invoke()
                        return true
                    }
                    KeyEvent.KEYCODE_C -> {
                        editor.copySelectedText()
                        editor.onResetModifiers?.invoke()
                        return true
                    }
                    KeyEvent.KEYCODE_X -> {
                        editor.copySelectedText()
                        editor.deleteSelectedText()
                        editor.onResetModifiers?.invoke()
                        return true
                    }
                    KeyEvent.KEYCODE_V -> {
                        editor.pasteFromClipboard()
                        editor.onResetModifiers?.invoke()
                        return true
                    }
                    KeyEvent.KEYCODE_S -> {
                        editor.save()
                        editor.onResetModifiers?.invoke()
                        return true
                    }
                    KeyEvent.KEYCODE_Z -> {
                        editor.onUndoShortcut?.invoke()
                        editor.onResetModifiers?.invoke()
                        return true
                    }
                    KeyEvent.KEYCODE_Y -> {
                        editor.onRedoShortcut?.invoke()
                        editor.onResetModifiers?.invoke()
                        return true
                    }
                    KeyEvent.KEYCODE_P -> {
                        editor.onCommandPaletteShortcut?.invoke()
                        editor.onResetModifiers?.invoke()
                        return true
                    }
                    KeyEvent.KEYCODE_F -> {
                        editor.onFindShortcut?.invoke()
                        editor.onResetModifiers?.invoke()
                        return true
                    }
                }
            }

            when (event.keyCode) {
                KeyEvent.KEYCODE_DEL -> {
                    editor.deleteBeforeCursor(1)
                    return true
                }
                KeyEvent.KEYCODE_FORWARD_DEL -> {
                    editor.deleteAfterCursor(1)
                    return true
                }
                KeyEvent.KEYCODE_ENTER -> {
                    editor.insertNewlineWithAutoIndent()
                    return true
                }
                KeyEvent.KEYCODE_TAB -> {
                    editor.insertText("    ")
                    return true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    editor.moveCursorLeft(isShift)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    editor.moveCursorRight(isShift)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_UP -> {
                    editor.moveCursorUp(isShift)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    editor.moveCursorDown(isShift)
                    return true
                }
            }
        }
        return super.sendKeyEvent(event)
    }

    override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence {
        val cursor = editor.cursorPosition
        val line = editor.buffer.getLine(cursor.line)
        val start = (cursor.col - n).coerceAtLeast(0)
        return line.substring(start, cursor.col)
    }

    override fun getTextAfterCursor(n: Int, flags: Int): CharSequence {
        val cursor = editor.cursorPosition
        val line = editor.buffer.getLine(cursor.line)
        val end = (cursor.col + n).coerceAtMost(line.length)
        return line.substring(cursor.col, end)
    }

    override fun getSelectedText(flags: Int): CharSequence? {
        val sel = editor.selection
        if (sel.isEmpty) return null
        return editor.buffer.getSelectedText(sel)
    }

    override fun getExtractedText(request: ExtractedTextRequest?, flags: Int): ExtractedText {
        val text = ExtractedText()
        text.text = editor.buffer.getText()
        text.startOffset = 0
        text.selectionStart = editor.buffer.positionToOffset(editor.selection.start)
        text.selectionEnd = editor.buffer.positionToOffset(editor.selection.end)
        return text
    }

    override fun setSelection(start: Int, end: Int): Boolean {
        val startPos = editor.buffer.offsetToPosition(start)
        val endPos = editor.buffer.offsetToPosition(end)
        editor.setSelection(startPos, endPos)
        return true
    }
}
