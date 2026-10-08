package com.droidcode.editor.engine

import android.text.TextUtils
import android.view.KeyEvent
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest

/**
 * Android Input Connection bridge connecting soft keyboards (Gboard, Samsung Keyboard, Indic IMEs)
 * and hardware keyboards directly to DroidCodeEngine's TextBuffer.
 *
 * Implements proper composing region lifecycle (preventing duplicated character bugs),
 * batch edit coalescing, absolute document offsets, code point deletions,
 * and unified key event dispatching.
 */
class EditorInputConnection(
    private val editor: CodeEditorView
) : BaseInputConnection(editor, true) {

    var composingStartOffset: Int = -1
        private set
    var composingEndOffset: Int = -1
        private set

    private var batchEditDepth = 0

    override fun beginBatchEdit(): Boolean {
        batchEditDepth++
        return true
    }

    override fun endBatchEdit(): Boolean {
        if (batchEditDepth > 0) {
            batchEditDepth--
        }
        if (batchEditDepth == 0) {
            editor.notifyImeSelection()
        }
        return true
    }

    override fun setComposingRegion(start: Int, end: Int): Boolean {
        val total = editor.buffer.totalLength
        val clampedStart = start.coerceIn(0, total)
        val clampedEnd = end.coerceIn(clampedStart, total)
        composingStartOffset = clampedStart
        composingEndOffset = clampedEnd
        if (batchEditDepth == 0) {
            editor.notifyImeSelection()
        }
        return true
    }

    override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
        if (text == null) return false
        val str = text.toString()

        if (composingStartOffset != -1 && composingEndOffset != -1 && composingEndOffset >= composingStartOffset) {
            // Replace the active composing span
            val startPos = editor.buffer.offsetToPosition(composingStartOffset)
            val endPos = editor.buffer.offsetToPosition(composingEndOffset)
            val replacedStart = editor.buffer.deleteRange(SelectionRange(startPos, endPos))
            val newCursorPos = editor.buffer.insert(replacedStart.line, replacedStart.col, str)
            composingEndOffset = composingStartOffset + str.length
            applyNewCursorPosition(composingStartOffset, composingEndOffset, newCursorPosition)
        } else if (!editor.selection.isEmpty) {
            val startOff = editor.buffer.positionToOffset(editor.selection.normalizedStart)
            editor.deleteSelectedText()
            editor.insertText(str)
            composingStartOffset = startOff
            composingEndOffset = startOff + str.length
            applyNewCursorPosition(composingStartOffset, composingEndOffset, newCursorPosition)
        } else {
            val curOff = editor.buffer.positionToOffset(editor.cursorPosition)
            editor.insertText(str)
            composingStartOffset = curOff
            composingEndOffset = curOff + str.length
            applyNewCursorPosition(composingStartOffset, composingEndOffset, newCursorPosition)
        }

        if (batchEditDepth == 0) {
            editor.notifyImeSelection()
        }
        return true
    }

    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
        if (text == null) return false
        val str = text.toString()

        if (composingStartOffset != -1 && composingEndOffset != -1 && composingEndOffset >= composingStartOffset) {
            val startPos = editor.buffer.offsetToPosition(composingStartOffset)
            val endPos = editor.buffer.offsetToPosition(composingEndOffset)
            val replacedStart = editor.buffer.deleteRange(SelectionRange(startPos, endPos))
            val newCursorPos = editor.buffer.insert(replacedStart.line, replacedStart.col, str)
            val committedEndOffset = composingStartOffset + str.length
            applyNewCursorPosition(composingStartOffset, committedEndOffset, newCursorPosition)
            composingStartOffset = -1
            composingEndOffset = -1
        } else {
            if (!editor.selection.isEmpty) {
                editor.deleteSelectedText()
            }
            val curOff = editor.buffer.positionToOffset(editor.cursorPosition)
            editor.insertText(str)
            val committedEndOffset = curOff + str.length
            applyNewCursorPosition(curOff, committedEndOffset, newCursorPosition)
        }

        if (batchEditDepth == 0) {
            editor.notifyImeSelection()
        }
        return true
    }

    override fun finishComposingText(): Boolean {
        composingStartOffset = -1
        composingEndOffset = -1
        if (batchEditDepth == 0) {
            editor.notifyImeSelection()
        }
        return true
    }

    private fun applyNewCursorPosition(startOffset: Int, endOffset: Int, newCursorPosition: Int) {
        val targetOffset = if (newCursorPosition > 0) {
            endOffset + (newCursorPosition - 1)
        } else {
            startOffset + newCursorPosition
        }
        val clamped = targetOffset.coerceIn(0, editor.buffer.totalLength)
        val pos = editor.buffer.offsetToPosition(clamped)
        editor.setCursorPosition(pos)
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
        if (batchEditDepth == 0) {
            editor.notifyImeSelection()
        }
        return handled || super.deleteSurroundingText(beforeLength, afterLength)
    }

    override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean {
        return deleteSurroundingText(beforeLength, afterLength)
    }

    override fun sendKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return false
        val handled = editor.handleKeyEvent(event.keyCode, event)
        if (handled) {
            if (batchEditDepth == 0) {
                editor.notifyImeSelection()
            }
            return true
        }
        return super.sendKeyEvent(event)
    }

    override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence {
        val curOffset = editor.buffer.positionToOffset(editor.cursorPosition)
        val start = maxOf(0, curOffset - n)
        return editor.buffer.getTextInRange(start, curOffset)
    }

    override fun getTextAfterCursor(n: Int, flags: Int): CharSequence {
        val curOffset = editor.buffer.positionToOffset(editor.cursorPosition)
        val totalLen = editor.buffer.totalLength
        val end = minOf(totalLen, curOffset + n)
        return editor.buffer.getTextInRange(curOffset, end)
    }

    override fun getSelectedText(flags: Int): CharSequence? {
        val sel = editor.selection
        if (sel.isEmpty) return null
        return editor.buffer.getSelectedText(sel)
    }

    override fun getExtractedText(request: ExtractedTextRequest?, flags: Int): ExtractedText? {
        if (request == null) return null
        val total = editor.buffer.totalLength
        val text = ExtractedText()
        // Provide extracted text for IME
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
        if (batchEditDepth == 0) {
            editor.notifyImeSelection()
        }
        return true
    }

    override fun getCursorCapsMode(reqModes: Int): Int {
        val curOffset = editor.buffer.positionToOffset(editor.cursorPosition)
        val before = getTextBeforeCursor(64, 0)
        return TextUtils.getCapsMode(before, before.length, reqModes)
    }

    override fun performEditorAction(actionCode: Int): Boolean {
        when (actionCode) {
            EditorInfo.IME_ACTION_DONE, EditorInfo.IME_ACTION_GO, EditorInfo.IME_ACTION_SEND -> {
                editor.insertNewlineWithAutoIndent()
                return true
            }
        }
        return super.performEditorAction(actionCode)
    }

    override fun performContextMenuAction(id: Int): Boolean {
        when (id) {
            android.R.id.selectAll -> {
                editor.selectAll()
                return true
            }
            android.R.id.cut -> {
                editor.cutSelection()
                return true
            }
            android.R.id.copy -> {
                editor.copySelectedText()
                return true
            }
            android.R.id.paste -> {
                editor.pasteFromClipboard()
                return true
            }
        }
        return super.performContextMenuAction(id)
    }
}
