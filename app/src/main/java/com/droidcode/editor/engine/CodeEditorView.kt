package com.droidcode.editor.engine

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.SystemClock
import android.text.InputType
import android.util.AttributeSet
import android.view.ActionMode
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.OverScroller
import com.droidcode.editor.LineDiffStatus
import com.droidcode.settings.AppSettings
import com.droidcode.ui.EditorFontHelper
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * High-performance virtualized Code Editor View for DroidCodeEngine.
 *
 * Implements direct hardware-accelerated Canvas viewport rendering, Monospace O(1)
 * spatial projection, native Android IME InputConnection, and smooth 120 FPS fling
 * scrolling. Capable of editing 100,000+ line documents without UI freezing.
 */
class CodeEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    val buffer = TextBuffer()
    var theme: EditorTheme = EditorTheme.darkTheme()
        set(value) {
            field = value
            tokenizer.updateConfig(languageId, value)
            invalidate()
        }

    var languageId: String = "kotlin"
        set(value) {
            field = value
            tokenizer.updateConfig(value, theme)
            invalidate()
        }

    val textPaddingStart: Float
        get() = 12f * resources.displayMetrics.density

    var fontOption: AppSettings.EditorFontFamily? = null
        set(value) {
            if (field != value) {
                field = value
                applyFontFamily()
            }
        }

    var fontSizeSp: Float = 14f
        set(value) {
            field = value
            updateMetrics()
            clampScroll()
            requestLayout()
            invalidate()
        }

    var isLineNumbersEnabled: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    var lineDiffMap: Map<Int, LineDiffStatus> = emptyMap()
        set(value) {
            field = value
            invalidate()
        }

    var onContentChanged: ((String) -> Unit)? = null
    var onCursorChanged: ((line: Int, col: Int) -> Unit)? = null
    var onSelectionChanged: ((start: Int, end: Int) -> Unit)? = null
    var onSaveShortcut: (() -> Unit)? = null
    var onUndoShortcut: (() -> Unit)? = null
    var onRedoShortcut: (() -> Unit)? = null
    var onCommandPaletteShortcut: (() -> Unit)? = null
    var onWorkspaceSearchShortcut: (() -> Unit)? = null
    var onFindShortcut: (() -> Unit)? = null
    var onResetModifiers: (() -> Unit)? = null

    var ctrlActive: Boolean = false
    var shiftActive: Boolean = false
    var altActive: Boolean = false

    var lastSyncedText: String? = null

    private val notifyContentRunnable = Runnable {
        val text = buffer.getText()
        lastSyncedText = text
        onContentChanged?.invoke(text)
    }

    fun flushContent() {
        removeCallbacks(notifyContentRunnable)
        val text = buffer.getText()
        lastSyncedText = text
        onContentChanged?.invoke(text)
    }

    private fun scheduleContentNotification() {
        removeCallbacks(notifyContentRunnable)
        postDelayed(notifyContentRunnable, 250L)
    }

    fun save() {
        flushContent()
        onSaveShortcut?.invoke()
    }

    fun setBufferText(text: String) {
        if (lastSyncedText === text) return
        lastSyncedText = text
        removeCallbacks(notifyContentRunnable)
        buffer.setText(text)
        tokenizer.clearCache()
        invalidate()
    }

    var cursorPosition = CursorPos(0, 0)
        private set
    var selection = SelectionRange(cursorPosition, cursorPosition)
        private set

    private var charWidth = 0f
    private var lineHeight = 0f
    private var baselineOffset = 0f

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.MONOSPACE
    }
    private val boldTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
    private val gutterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.RIGHT
    }
    private val uiPaint = Paint()

    private val tokenizer = LineTokenizer(languageId, theme)
    private val scroller = OverScroller(context)

    private var cursorVisible = true
    private var lastCursorBlinkTime = 0L
    private val cursorBlinkInterval = 500L

    private enum class HandleType { START, END }
    private var activeHandleDrag: HandleType? = null
    private var isLongPressDragging: Boolean = false
    private var selectionAnchor: CursorPos? = null

    private var selectionActionMode: ActionMode? = null

    private val cursorBlinkRunnable = object : Runnable {
        override fun run() {
            if (isFocused) {
                cursorVisible = !cursorVisible
                invalidate()
                postDelayed(this, cursorBlinkInterval)
            }
        }
    }

    private val actionModeCallback = object : ActionMode.Callback2() {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            menu.add(Menu.NONE, android.R.id.cut, 1, android.R.string.cut)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            menu.add(Menu.NONE, android.R.id.copy, 2, android.R.string.copy)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            menu.add(Menu.NONE, android.R.id.paste, 3, android.R.string.paste)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            menu.add(Menu.NONE, android.R.id.selectAll, 4, android.R.string.selectAll)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            val hasSelection = !selection.isEmpty
            menu.findItem(android.R.id.cut)?.isVisible = hasSelection
            menu.findItem(android.R.id.copy)?.isVisible = hasSelection
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            menu.findItem(android.R.id.paste)?.isVisible = clipboard?.hasPrimaryClip() == true
            return true
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                android.R.id.cut -> {
                    copySelectedText()
                    deleteSelectedText()
                    mode.finish()
                    return true
                }
                android.R.id.copy -> {
                    copySelectedText()
                    mode.finish()
                    return true
                }
                android.R.id.paste -> {
                    pasteFromClipboard()
                    mode.finish()
                    return true
                }
                android.R.id.selectAll -> {
                    selectAll()
                    return true
                }
            }
            return false
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            selectionActionMode = null
        }

        override fun onGetContentRect(mode: ActionMode, view: View, outRect: Rect) {
            val gutterW = calculateGutterWidth()
            val padStart = textPaddingStart
            if (!selection.isEmpty) {
                val normStart = selection.normalizedStart
                val normEnd = selection.normalizedEnd
                val left = (gutterW + padStart + normStart.col * charWidth - scrollX).toInt().coerceIn(0, width)
                val right = (gutterW + padStart + normEnd.col * charWidth - scrollX).toInt().coerceIn(left, width)
                val top = (normStart.line * lineHeight - scrollY).toInt().coerceIn(0, height)
                val bottom = ((normEnd.line + 1) * lineHeight - scrollY).toInt().coerceIn(top, height)
                outRect.set(left, top, max(left + 1, right), max(top + 1, bottom))
            } else {
                val x = (gutterW + padStart + cursorPosition.col * charWidth - scrollX).toInt().coerceIn(0, width)
                val y = (cursorPosition.line * lineHeight - scrollY).toInt().coerceIn(0, height)
                outRect.set(x, y, x + 1, (y + lineHeight).toInt())
            }
        }
    }

    fun showSelectionActionMode() {
        if (selection.isEmpty) {
            dismissSelectionActionMode()
            return
        }
        if (selectionActionMode == null) {
            selectionActionMode = startActionMode(actionModeCallback, ActionMode.TYPE_FLOATING)
        } else {
            selectionActionMode?.invalidate()
        }
    }

    fun dismissSelectionActionMode() {
        selectionActionMode?.finish()
        selectionActionMode = null
    }

    private fun notifySelectionAndCursor() {
        onCursorChanged?.invoke(cursorPosition.line + 1, cursorPosition.col + 1)
        val startOffset = buffer.positionToOffset(selection.start)
        val endOffset = buffer.positionToOffset(selection.end)
        onSelectionChanged?.invoke(startOffset, endOffset)
    }

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            if (!scroller.isFinished) {
                scroller.forceFinished(true)
            }
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            requestFocus()
            showSoftKeyboard()
            val pos = screenToCursor(e.x, e.y)
            setCursorPositionInternal(pos)
            selection = SelectionRange(pos, pos)
            dismissSelectionActionMode()
            notifySelectionAndCursor()
            resetCursorBlink()
            invalidate()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            val pos = screenToCursor(e.x, e.y)
            selectWordAt(pos)
            showSelectionActionMode()
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            val pos = screenToCursor(e.x, e.y)
            selectWordAt(pos)
            isLongPressDragging = true
            selectionAnchor = selection.normalizedStart
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            showSelectionActionMode()
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            parent?.requestDisallowInterceptTouchEvent(true)
            scrollBy(distanceX.toInt(), distanceY.toInt())
            clampScroll()
            selectionActionMode?.invalidate()
            invalidate()
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            scroller.fling(
                scrollX, scrollY,
                -velocityX.toInt(), -velocityY.toInt(),
                0, getMaxScrollX(),
                0, getMaxScrollY()
            )
            selectionActionMode?.invalidate()
            invalidate()
            return true
        }
    })

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        applyFontFamily()

        buffer.onContentChanged = {
            invalidate()
            onContentChanged?.invoke(buffer.getText())
        }
    }

    private fun applyFontFamily() {
        val tf = EditorFontHelper.getTypeface(context, fontOption)
        textPaint.typeface = tf
        boldTextPaint.typeface = Typeface.create(tf, Typeface.BOLD)
        gutterPaint.typeface = tf
        updateMetrics()
        clampScroll()
        requestLayout()
        invalidate()
    }

    private fun updateMetrics() {
        val density = resources.displayMetrics.scaledDensity
        val pxSize = fontSizeSp * density
        textPaint.textSize = pxSize
        boldTextPaint.textSize = pxSize
        gutterPaint.textSize = pxSize * 0.9f

        charWidth = textPaint.measureText("M")
        val fm = textPaint.fontMetrics
        val textHeight = fm.descent - fm.ascent
        lineHeight = textHeight * 1.35f
        baselineOffset = -fm.ascent + (lineHeight - textHeight) / 2f
    }

    private fun calculateGutterWidth(): Float {
        if (!isLineNumbersEnabled) return 0f
        val digits = max(3, buffer.lineCount.toString().length)
        return (digits * charWidth * 0.9f) + (16f * resources.displayMetrics.density)
    }

    private fun getMaxScrollX(): Int {
        val maxLen = (0 until min(buffer.lineCount, 500)).maxOfOrNull { buffer.getLineLength(it) } ?: 80
        return max(0, (maxLen * charWidth + textPaddingStart + width * 0.5f).toInt())
    }

    private fun getMaxScrollY(): Int {
        val contentHeight = (buffer.lineCount * lineHeight).toInt()
        val visibleHeight = height
        return if (contentHeight <= visibleHeight) {
            0
        } else {
            max(0, contentHeight - (visibleHeight * 0.3f).toInt())
        }
    }

    private fun clampScroll() {
        val clampedX = scrollX.coerceIn(0, getMaxScrollX())
        val clampedY = scrollY.coerceIn(0, getMaxScrollY())
        if (clampedX != scrollX || clampedY != scrollY) {
            scrollTo(clampedX, clampedY)
        }
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.currX, scroller.currY)
            clampScroll()
            selectionActionMode?.invalidate()
            invalidate()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)

                if (!selection.isEmpty) {
                    val density = resources.displayMetrics.density
                    val handleRadius = 10f * density
                    val touchRadius = 36f * density
                    val rSq = touchRadius * touchRadius
                    val gutterW = calculateGutterWidth()
                    val padStart = textPaddingStart

                    val normStart = selection.normalizedStart
                    val normEnd = selection.normalizedEnd

                    val startCenterX = gutterW + padStart + normStart.col * charWidth - scrollX - handleRadius / 2f
                    val startCenterY = (normStart.line + 1) * lineHeight - scrollY + handleRadius

                    val endCenterX = gutterW + padStart + normEnd.col * charWidth - scrollX + handleRadius / 2f
                    val endCenterY = (normEnd.line + 1) * lineHeight - scrollY + handleRadius

                    val distStartSq = (event.x - startCenterX) * (event.x - startCenterX) +
                            (event.y - startCenterY) * (event.y - startCenterY)
                    val distEndSq = (event.x - endCenterX) * (event.x - endCenterX) +
                            (event.y - endCenterY) * (event.y - endCenterY)

                    if (distStartSq <= rSq && distStartSq <= distEndSq) {
                        activeHandleDrag = HandleType.START
                        return true
                    } else if (distEndSq <= rSq) {
                        activeHandleDrag = HandleType.END
                        return true
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)

                if (activeHandleDrag != null) {
                    val touchCursor = screenToCursor(event.x, event.y - lineHeight / 2f)
                    if (activeHandleDrag == HandleType.START) {
                        selection = SelectionRange(touchCursor, selection.normalizedEnd)
                    } else {
                        selection = SelectionRange(selection.normalizedStart, touchCursor)
                    }
                    cursorPosition = touchCursor
                    notifySelectionAndCursor()
                    selectionActionMode?.invalidate()
                    scrollToCursor()
                    invalidate()
                    return true
                }

                if (isLongPressDragging) {
                    val touchCursor = screenToCursor(event.x, event.y)
                    val anchor = selectionAnchor ?: selection.normalizedStart
                    selection = SelectionRange(anchor, touchCursor)
                    cursorPosition = touchCursor
                    notifySelectionAndCursor()
                    selectionActionMode?.invalidate()
                    scrollToCursor()
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                if (activeHandleDrag != null || isLongPressDragging) {
                    activeHandleDrag = null
                    isLongPressDragging = false
                    selectionAnchor = null
                    if (!selection.isEmpty) {
                        showSelectionActionMode()
                    }
                    invalidate()
                    return true
                }
            }
        }

        if (gestureDetector.onTouchEvent(event)) {
            return true
        }

        // Support mouse or stylus drag-to-select (not finger scrolling)
        val isMouseOrStylus = event.getToolType(0) == MotionEvent.TOOL_TYPE_MOUSE ||
                event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS
        if (isMouseOrStylus && event.action == MotionEvent.ACTION_MOVE && event.historySize > 0) {
            val pos = screenToCursor(event.x, event.y)
            selection = SelectionRange(selection.start, pos)
            cursorPosition = pos
            notifySelectionAndCursor()
            scrollToCursor()
            invalidate()
            return true
        }

        return super.onTouchEvent(event)
    }

    private fun screenToCursor(screenX: Float, screenY: Float): CursorPos {
        val gutterW = calculateGutterWidth()
        val padStart = textPaddingStart
        val contentX = screenX + scrollX - gutterW - padStart
        val contentY = screenY + scrollY

        val line = (contentY / lineHeight).toInt().coerceIn(0, (buffer.lineCount - 1).coerceAtLeast(0))
        val lineStr = buffer.getLine(line)
        val col = if (contentX > 0 && charWidth > 0f) {
            (contentX / charWidth).roundToInt().coerceIn(0, lineStr.length)
        } else {
            0
        }
        return CursorPos(line, col)
    }

    private fun selectWordAt(pos: CursorPos) {
        val line = buffer.getLine(pos.line)
        if (line.isEmpty()) return

        var start = pos.col.coerceIn(0, line.length - 1)
        var end = start

        while (start > 0 && (line[start - 1].isLetterOrDigit() || line[start - 1] == '_')) {
            start--
        }
        while (end < line.length && (line[end].isLetterOrDigit() || line[end] == '_')) {
            end++
        }

        selection = SelectionRange(CursorPos(pos.line, start), CursorPos(pos.line, end))
        cursorPosition = selection.end
        notifySelectionAndCursor()
        resetCursorBlink()
        invalidate()
    }

    fun setCursorPosition(pos: CursorPos) {
        setCursorPositionInternal(buffer.clampPosition(pos))
        selection = SelectionRange(cursorPosition, cursorPosition)
        dismissSelectionActionMode()
        notifySelectionAndCursor()
        scrollToCursor()
        invalidate()
    }

    private fun setCursorPositionInternal(pos: CursorPos) {
        cursorPosition = pos
    }

    fun setSelection(start: CursorPos, end: CursorPos) {
        selection = SelectionRange(buffer.clampPosition(start), buffer.clampPosition(end))
        cursorPosition = selection.end
        notifySelectionAndCursor()
        if (!selection.isEmpty) {
            showSelectionActionMode()
        } else {
            dismissSelectionActionMode()
        }
        scrollToCursor()
        invalidate()
    }

    fun setSelectionOffsets(startOffset: Int, endOffset: Int) {
        val startPos = buffer.offsetToPosition(startOffset)
        val endPos = buffer.offsetToPosition(endOffset)
        setSelection(startPos, endPos)
    }

    fun getSelectionOffsets(): Pair<Int, Int> {
        val start = buffer.positionToOffset(selection.start)
        val end = buffer.positionToOffset(selection.end)
        return Pair(start, end)
    }

    fun copySelectedText() {
        if (selection.isEmpty) return
        val selectedText = buffer.getSelectedText(selection)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("DroidCode", selectedText)
        clipboard?.setPrimaryClip(clip)
    }

    fun deleteSelectedText() {
        if (selection.isEmpty) return
        cursorPosition = buffer.deleteRange(selection)
        selection = SelectionRange(cursorPosition, cursorPosition)
        notifySelectionAndCursor()
        scheduleContentNotification()
        invalidate()
    }

    fun pasteFromClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = clipboard?.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).coerceToText(context)?.toString() ?: ""
            if (text.isNotEmpty()) {
                insertText(text)
            }
        }
    }

    fun selectAll() {
        val lastLine = (buffer.lineCount - 1).coerceAtLeast(0)
        val lastCol = buffer.getLineLength(lastLine)
        selection = SelectionRange(CursorPos(0, 0), CursorPos(lastLine, lastCol))
        cursorPosition = selection.end
        notifySelectionAndCursor()
        showSelectionActionMode()
        scrollToCursor()
        invalidate()
    }

    fun scrollToCursor() {
        val gutterW = calculateGutterWidth()
        val padStart = textPaddingStart
        val cursorX = gutterW + padStart + cursorPosition.col * charWidth
        val cursorY = cursorPosition.line * lineHeight

        var targetScrollX = scrollX
        var targetScrollY = scrollY

        val padding = 40f
        if (cursorX < scrollX + gutterW + padStart + padding) {
            targetScrollX = max(0, (cursorX - gutterW - padStart - padding).toInt())
        } else if (cursorX > scrollX + width - padding) {
            targetScrollX = (cursorX - width + padding).toInt()
        }

        if (cursorY < scrollY + padding) {
            targetScrollY = max(0, (cursorY - padding).toInt())
        } else if (cursorY > scrollY + height - lineHeight - padding) {
            targetScrollY = (cursorY - height + lineHeight + padding).toInt()
        }

        targetScrollY = targetScrollY.coerceIn(0, getMaxScrollY())

        if (targetScrollX != scrollX || targetScrollY != scrollY) {
            scrollTo(targetScrollX, targetScrollY)
            clampScroll()
        }
    }

    fun insertText(text: String) {
        if (ctrlActive) {
            when (text.lowercase()) {
                "a" -> { selectAll(); onResetModifiers?.invoke(); return }
                "c" -> { copySelectedText(); onResetModifiers?.invoke(); return }
                "x" -> { copySelectedText(); deleteSelectedText(); onResetModifiers?.invoke(); return }
                "v" -> { pasteFromClipboard(); onResetModifiers?.invoke(); return }
                "s" -> { save(); onResetModifiers?.invoke(); return }
                "z" -> { onUndoShortcut?.invoke(); onResetModifiers?.invoke(); return }
                "y" -> { onRedoShortcut?.invoke(); onResetModifiers?.invoke(); return }
                "p" -> { onCommandPaletteShortcut?.invoke(); onResetModifiers?.invoke(); return }
                "f" -> {
                    if (shiftActive) {
                        onWorkspaceSearchShortcut?.invoke()
                    } else {
                        onFindShortcut?.invoke()
                    }
                    onResetModifiers?.invoke()
                    return
                }
                else -> { onResetModifiers?.invoke(); return }
            }
        }

        if (!selection.isEmpty) {
            cursorPosition = buffer.deleteRange(selection)
            selection = SelectionRange(cursorPosition, cursorPosition)
            dismissSelectionActionMode()
        }
        cursorPosition = buffer.insert(cursorPosition.line, cursorPosition.col, text)
        selection = SelectionRange(cursorPosition, cursorPosition)
        scheduleContentNotification()
        notifySelectionAndCursor()
        resetCursorBlink()
        scrollToCursor()
        invalidate()
    }

    fun deleteBeforeCursor(count: Int = 1) {
        if (!selection.isEmpty) {
            cursorPosition = buffer.deleteRange(selection)
            selection = SelectionRange(cursorPosition, cursorPosition)
            dismissSelectionActionMode()
        } else {
            cursorPosition = buffer.deleteBefore(cursorPosition, count)
            selection = SelectionRange(cursorPosition, cursorPosition)
        }
        scheduleContentNotification()
        notifySelectionAndCursor()
        resetCursorBlink()
        scrollToCursor()
        invalidate()
    }

    fun deleteAfterCursor(count: Int = 1) {
        if (!selection.isEmpty) {
            cursorPosition = buffer.deleteRange(selection)
            selection = SelectionRange(cursorPosition, cursorPosition)
            dismissSelectionActionMode()
        } else {
            val offset = buffer.positionToOffset(cursorPosition)
            val nextPos = buffer.offsetToPosition(offset + count)
            buffer.deleteRange(SelectionRange(cursorPosition, nextPos))
        }
        scheduleContentNotification()
        notifySelectionAndCursor()
        resetCursorBlink()
        scrollToCursor()
        invalidate()
    }

    fun insertNewlineWithAutoIndent() {
        val currentLine = buffer.getLine(cursorPosition.line)
        val prefixIndent = currentLine.takeWhile { it == ' ' || it == '\t' }
        insertText("\n$prefixIndent")
    }

    fun moveCursorLeft(isShift: Boolean) {
        val newPos = if (cursorPosition.col > 0) {
            CursorPos(cursorPosition.line, cursorPosition.col - 1)
        } else if (cursorPosition.line > 0) {
            val prevLine = cursorPosition.line - 1
            CursorPos(prevLine, buffer.getLineLength(prevLine))
        } else {
            cursorPosition
        }
        updateCursorMove(newPos, isShift)
    }

    fun moveCursorRight(isShift: Boolean) {
        val lineLen = buffer.getLineLength(cursorPosition.line)
        val newPos = if (cursorPosition.col < lineLen) {
            CursorPos(cursorPosition.line, cursorPosition.col + 1)
        } else if (cursorPosition.line < buffer.lineCount - 1) {
            CursorPos(cursorPosition.line + 1, 0)
        } else {
            cursorPosition
        }
        updateCursorMove(newPos, isShift)
    }

    fun moveCursorUp(isShift: Boolean) {
        if (cursorPosition.line > 0) {
            val targetLine = cursorPosition.line - 1
            val targetCol = cursorPosition.col.coerceIn(0, buffer.getLineLength(targetLine))
            updateCursorMove(CursorPos(targetLine, targetCol), isShift)
        }
    }

    fun moveCursorDown(isShift: Boolean) {
        if (cursorPosition.line < buffer.lineCount - 1) {
            val targetLine = cursorPosition.line + 1
            val targetCol = cursorPosition.col.coerceIn(0, buffer.getLineLength(targetLine))
            updateCursorMove(CursorPos(targetLine, targetCol), isShift)
        }
    }

    private fun updateCursorMove(newPos: CursorPos, isShift: Boolean) {
        cursorPosition = newPos
        selection = if (isShift) {
            SelectionRange(selection.start, newPos)
        } else {
            dismissSelectionActionMode()
            SelectionRange(newPos, newPos)
        }
        notifySelectionAndCursor()
        if (isShift && !selection.isEmpty) {
            showSelectionActionMode()
        }
        resetCursorBlink()
        scrollToCursor()
        invalidate()
    }

    private fun resetCursorBlink() {
        cursorVisible = true
        lastCursorBlinkTime = SystemClock.uptimeMillis()
        removeCallbacks(cursorBlinkRunnable)
        postDelayed(cursorBlinkRunnable, cursorBlinkInterval)
    }

    fun showSoftKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        postDelayed(cursorBlinkRunnable, cursorBlinkInterval)
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        if (!gainFocus) {
            dismissSelectionActionMode()
            flushContent()
            cursorVisible = false
            removeCallbacks(cursorBlinkRunnable)
            invalidate()
        } else {
            resetCursorBlink()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        dismissSelectionActionMode()
        flushContent()
        removeCallbacks(cursorBlinkRunnable)
        removeCallbacks(notifyContentRunnable)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null) {
            val isCtrl = event.isCtrlPressed || ctrlActive
            val isShift = event.isShiftPressed || shiftActive
            if (isCtrl) {
                if (isShift && keyCode == KeyEvent.KEYCODE_F) {
                    onWorkspaceSearchShortcut?.invoke()
                    onResetModifiers?.invoke()
                    return true
                }
                when (keyCode) {
                    KeyEvent.KEYCODE_A -> {
                        selectAll()
                        onResetModifiers?.invoke()
                        return true
                    }
                    KeyEvent.KEYCODE_C -> {
                        copySelectedText()
                        onResetModifiers?.invoke()
                        return true
                    }
                    KeyEvent.KEYCODE_X -> {
                        copySelectedText()
                        deleteSelectedText()
                        onResetModifiers?.invoke()
                        return true
                    }
                    KeyEvent.KEYCODE_V -> {
                        pasteFromClipboard()
                        onResetModifiers?.invoke()
                        return true
                    }
                    KeyEvent.KEYCODE_S -> {
                        save()
                        onResetModifiers?.invoke()
                        return true
                    }
                    KeyEvent.KEYCODE_Z -> {
                        onUndoShortcut?.invoke()
                        onResetModifiers?.invoke()
                        return true
                    }
                    KeyEvent.KEYCODE_Y -> {
                        onRedoShortcut?.invoke()
                        onResetModifiers?.invoke()
                        return true
                    }
                    KeyEvent.KEYCODE_P -> {
                        onCommandPaletteShortcut?.invoke()
                        onResetModifiers?.invoke()
                        return true
                    }
                    KeyEvent.KEYCODE_F -> {
                        onFindShortcut?.invoke()
                        onResetModifiers?.invoke()
                        return true
                    }
                }
            }
            when (keyCode) {
                KeyEvent.KEYCODE_DEL -> {
                    deleteBeforeCursor(1)
                    return true
                }
                KeyEvent.KEYCODE_FORWARD_DEL -> {
                    deleteAfterCursor(1)
                    return true
                }
                KeyEvent.KEYCODE_ENTER -> {
                    insertNewlineWithAutoIndent()
                    return true
                }
                KeyEvent.KEYCODE_TAB -> {
                    insertText("    ")
                    return true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    moveCursorLeft(isShift)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    moveCursorRight(isShift)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_UP -> {
                    moveCursorUp(isShift)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    moveCursorDown(isShift)
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN or EditorInfo.IME_ACTION_NONE
        return EditorInputConnection(this)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Fill editor background
        canvas.drawColor(theme.backgroundColor)

        val gutterW = calculateGutterWidth()
        val firstVisibleLine = (scrollY / lineHeight).toInt().coerceIn(0, (buffer.lineCount - 1).coerceAtLeast(0))
        val lastVisibleLine = ((scrollY + height) / lineHeight + 1).toInt().coerceIn(firstVisibleLine, (buffer.lineCount - 1).coerceAtLeast(0))

        // 2. Draw current line background highlight
        val currentLineTop = cursorPosition.line * lineHeight
        uiPaint.color = theme.currentLineBackgroundColor
        canvas.drawRect(
            gutterW,
            currentLineTop,
            width.toFloat() + scrollX,
            currentLineTop + lineHeight,
            uiPaint
        )

        // 3. Draw text selection background
        if (!selection.isEmpty) {
            uiPaint.color = theme.selectionColor
            val normStart = selection.normalizedStart
            val normEnd = selection.normalizedEnd
            val lineContentStartX = gutterW + textPaddingStart

            for (l in normStart.line..normEnd.line) {
                if (l in firstVisibleLine..lastVisibleLine) {
                    val lineTop = l * lineHeight
                    val lineBottom = lineTop + lineHeight
                    val lineStr = buffer.getLine(l)

                    val startX = if (l == normStart.line) lineContentStartX + normStart.col * charWidth else lineContentStartX
                    val endX = if (l == normEnd.line) {
                        lineContentStartX + normEnd.col * charWidth
                    } else {
                        lineContentStartX + (lineStr.length + 1) * charWidth
                    }

                    canvas.drawRect(startX, lineTop, endX, lineBottom, uiPaint)
                }
            }
        }

        // 4. Render visible lines
        val density = resources.displayMetrics.density
        val gutterPaddingRight = 8f * density
        val diffBarWidth = 3f * density
        val padStart = textPaddingStart

        for (lineIndex in firstVisibleLine..lastVisibleLine) {
            val lineTop = lineIndex * lineHeight
            val baseline = lineTop + baselineOffset
            val lineText = buffer.getLine(lineIndex)

            // Draw line text with syntax highlighting
            val tokens = tokenizer.tokenizeLine(lineText)
            var currentCol = 0
            val textStartX = gutterW + padStart

            for (token in tokens) {
                // Unstyled prefix before token
                if (token.startCol > currentCol) {
                    val plain = lineText.substring(currentCol, token.startCol)
                    textPaint.color = theme.textColor
                    canvas.drawText(plain, textStartX + currentCol * charWidth, baseline, textPaint)
                }

                // Styled token
                val tokenStr = lineText.substring(
                    token.startCol.coerceIn(0, lineText.length),
                    token.endCol.coerceIn(token.startCol, lineText.length)
                )
                val paintToUse = if (token.isBold) boldTextPaint else textPaint
                paintToUse.color = token.color
                canvas.drawText(tokenStr, textStartX + token.startCol * charWidth, baseline, paintToUse)
                currentCol = token.endCol
            }

            // Remainder of line after last token
            if (currentCol < lineText.length) {
                val remaining = lineText.substring(currentCol)
                textPaint.color = theme.textColor
                canvas.drawText(remaining, textStartX + currentCol * charWidth, baseline, textPaint)
            }
        }

        // 5. Draw line number gutter (Pinned to left viewport)
        if (isLineNumbersEnabled) {
            val gutterScreenLeft = scrollX.toFloat()
            val gutterScreenRight = scrollX + gutterW

            // Gutter background
            uiPaint.color = theme.gutterBackgroundColor
            canvas.drawRect(gutterScreenLeft, scrollY.toFloat(), gutterScreenRight, (scrollY + height).toFloat(), uiPaint)

            // Gutter vertical divider line
            uiPaint.color = theme.gutterDividerColor
            canvas.drawRect(gutterScreenRight - 1f * density, scrollY.toFloat(), gutterScreenRight, (scrollY + height).toFloat(), uiPaint)

            for (lineIndex in firstVisibleLine..lastVisibleLine) {
                val lineTop = lineIndex * lineHeight
                val baseline = lineTop + baselineOffset
                val lineNumber = lineIndex + 1
                val isActiveLine = (lineIndex == cursorPosition.line)

                // Line number text
                gutterPaint.color = if (isActiveLine) theme.activeLineNumberColor else theme.lineNumberColor
                canvas.drawText(
                    lineNumber.toString(),
                    gutterScreenRight - gutterPaddingRight,
                    baseline,
                    gutterPaint
                )

                // Git diff status indicator on gutter edge
                when (lineDiffMap[lineIndex]) {
                    LineDiffStatus.ADDED -> {
                        uiPaint.color = theme.addedGutterColor
                        canvas.drawRect(
                            gutterScreenRight - diffBarWidth,
                            lineTop,
                            gutterScreenRight,
                            lineTop + lineHeight,
                            uiPaint
                        )
                    }
                    LineDiffStatus.MODIFIED -> {
                        uiPaint.color = theme.modifiedGutterColor
                        canvas.drawRect(
                            gutterScreenRight - diffBarWidth,
                            lineTop,
                            gutterScreenRight,
                            lineTop + lineHeight,
                            uiPaint
                        )
                    }
                    else -> {}
                }
            }
        }

        // 6. Draw Selection Handles & Blinking Cursor
        val padStart = textPaddingStart
        if (!selection.isEmpty) {
            val normStart = selection.normalizedStart
            val normEnd = selection.normalizedEnd
            val handleRadius = 9f * density
            uiPaint.color = theme.cursorColor

            // Start handle
            if (normStart.line in firstVisibleLine..lastVisibleLine) {
                val startX = gutterW + padStart + normStart.col * charWidth
                val startY = (normStart.line + 1) * lineHeight
                canvas.drawRect(startX - 1f * density, startY - lineHeight, startX + 1f * density, startY, uiPaint)
                canvas.drawCircle(startX - handleRadius / 2f, startY + handleRadius, handleRadius, uiPaint)
            }

            // End handle
            if (normEnd.line in firstVisibleLine..lastVisibleLine) {
                val endX = gutterW + padStart + normEnd.col * charWidth
                val endY = (normEnd.line + 1) * lineHeight
                canvas.drawRect(endX - 1f * density, endY - lineHeight, endX + 1f * density, endY, uiPaint)
                canvas.drawCircle(endX + handleRadius / 2f, endY + handleRadius, handleRadius, uiPaint)
            }
        } else if (isFocused && cursorVisible) {
            val cursorX = gutterW + padStart + cursorPosition.col * charWidth
            val cursorY = cursorPosition.line * lineHeight
            val cursorW = 2f * density

            uiPaint.color = theme.cursorColor
            canvas.drawRoundRect(
                RectF(cursorX, cursorY + 2f * density, cursorX + cursorW, cursorY + lineHeight - 2f * density),
                1f, 1f,
                uiPaint
            )
        }
    }
}
