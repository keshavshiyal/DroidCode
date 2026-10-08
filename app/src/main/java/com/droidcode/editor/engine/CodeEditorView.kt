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
import android.util.TypedValue
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
    var onFileContentChanged: ((filePath: String, content: String) -> Unit)? = null
    var onCursorChanged: ((line: Int, col: Int) -> Unit)? = null
    var onSelectionChanged: ((start: Int, end: Int) -> Unit)? = null
    var onSaveShortcut: (() -> Unit)? = null
    var onUndoShortcut: (() -> Unit)? = null
    var onRedoShortcut: (() -> Unit)? = null
    var onCommandPaletteShortcut: (() -> Unit)? = null
    var onWorkspaceSearchShortcut: (() -> Unit)? = null
    var onFindShortcut: (() -> Unit)? = null
    var onResetModifiers: (() -> Unit)? = null
    var onEditorFocus: (() -> Unit)? = null

    var ctrlActive: Boolean = false
    var shiftActive: Boolean = false
    var altActive: Boolean = false

    var lastSyncedText: String? = null

    private val notifyContentRunnable = Runnable {
        if (suppressExternalCallback) return@Runnable
        val text = buffer.getText()
        if (text == lastSyncedText) return@Runnable
        lastSyncedText = text
        val path = currentFilePath
        if (path != null) {
            onFileContentChanged?.invoke(path, text)
        }
        onContentChanged?.invoke(text)
    }

    private var suppressExternalCallback = false

    fun flushContent() {
        removeCallbacks(notifyContentRunnable)
        if (suppressExternalCallback) return
        val text = buffer.getText()
        if (text == lastSyncedText) return
        lastSyncedText = text
        val path = currentFilePath
        if (path != null) {
            onFileContentChanged?.invoke(path, text)
        }
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

    var lastSyncedContentVersion: Long = -1L
    var lastAppliedSelectionVersion: Long = 0L

    var searchQuery: String? = null
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var currentFilePath: String? = null
    var onScrollPositionChanged: ((x: Int, y: Int) -> Unit)? = null

    fun setBufferText(text: String, filePath: String? = null, contentVersion: Long = -1L) {
        val fileChanged = filePath != null && filePath != currentFilePath
        if (fileChanged) {
            flushContent()
            currentFilePath = filePath
        } else if (filePath != null) {
            currentFilePath = filePath
        }
        if (!fileChanged && contentVersion != -1L && lastSyncedContentVersion == contentVersion) return
        if (!fileChanged && lastSyncedText === text && buffer.getText() == text) return

        suppressExternalCallback = true
        try {
            lastSyncedText = text
            lastSyncedContentVersion = contentVersion
            removeCallbacks(notifyContentRunnable)
            buffer.setText(text)
            tokenizer.clearCache()
            if (fileChanged) {
                cursorPosition = CursorPos(0, 0)
                selection = SelectionRange(cursorPosition, cursorPosition)
                scrollTo(0, 0)
            } else {
                cursorPosition = buffer.clampPosition(cursorPosition)
                selection = SelectionRange(
                    buffer.clampPosition(selection.start),
                    buffer.clampPosition(selection.end)
                )
                clampScroll()
            }
        } finally {
            suppressExternalCallback = false
        }
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
            menu.findItem(android.R.id.selectAll)?.isVisible = true
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

    private var lastDoubleTapTime: Long = 0L
    private var residualScrollX: Float = 0f
    private var residualScrollY: Float = 0f

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            onEditorFocus?.invoke()
            residualScrollX = 0f
            residualScrollY = 0f
            if (!scroller.isFinished) {
                scroller.forceFinished(true)
            }
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            onEditorFocus?.invoke()
            requestFocus()
            showSoftKeyboard()
            val pos = screenToCursor(e.x, e.y)
            val now = SystemClock.uptimeMillis()
            val doubleTapTimeout = android.view.ViewConfiguration.getDoubleTapTimeout().toLong()
            if (now - lastDoubleTapTime < doubleTapTimeout) {
                // Triple tap: Select entire line!
                lastDoubleTapTime = 0L
                val lineLen = buffer.getLineLength(pos.line)
                selection = SelectionRange(CursorPos(pos.line, 0), CursorPos(pos.line, lineLen))
                cursorPosition = selection.end
                showSelectionActionMode()
                notifySelectionAndCursor()
                invalidate()
                return true
            }

            setCursorPositionInternal(pos)
            selection = SelectionRange(pos, pos)
            dismissSelectionActionMode()
            notifySelectionAndCursor()
            resetCursorBlink()
            invalidate()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            onEditorFocus?.invoke()
            lastDoubleTapTime = SystemClock.uptimeMillis()
            val pos = screenToCursor(e.x, e.y)
            selectWordAt(pos)
            showSelectionActionMode()
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            onEditorFocus?.invoke()
            requestFocus()
            showSoftKeyboard()
            val pos = screenToCursor(e.x, e.y)
            setCursorPositionInternal(pos)
            val wordSelected = selectWordAt(pos)
            if (wordSelected) {
                isLongPressDragging = true
                selectionAnchor = selection.normalizedStart
            } else {
                isLongPressDragging = false
                selectionAnchor = null
                selection = SelectionRange(pos, pos)
            }
            notifySelectionAndCursor()
            resetCursorBlink()
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            showSelectionActionMode()
            invalidate()
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            parent?.requestDisallowInterceptTouchEvent(true)
            residualScrollX += distanceX
            residualScrollY += distanceY
            val dx = residualScrollX.toInt()
            val dy = residualScrollY.toInt()
            if (dx != 0 || dy != 0) {
                residualScrollX -= dx
                residualScrollY -= dy
                scrollBy(dx, dy)
                clampScroll()
                selectionActionMode?.invalidate()
                invalidate()
            }
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
            postInvalidateOnAnimation()
            return true
        }
    })

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        applyFontFamily()

        buffer.onContentChanged = {
            invalidate()
            scheduleContentNotification()
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

    private var cachedMaxLineLength: Int = 80
    private val lineEndStates = HashMap<Int, LineState>()

    fun getLineStartState(lineIndex: Int): LineState {
        if (lineIndex <= 0) return LineState.NORMAL
        return lineEndStates[lineIndex - 1] ?: LineState.NORMAL
    }

    fun setLineEndState(lineIndex: Int, state: LineState) {
        lineEndStates[lineIndex] = state
    }

    fun recalculateMaxLineLength() {
        val count = buffer.lineCount
        var maxLen = 0
        for (i in 0 until count) {
            val len = buffer.getLineLength(i)
            if (len > maxLen) {
                maxLen = len
            }
        }
        cachedMaxLineLength = maxOf(maxLen, 40)
    }

    private fun updateMetrics() {
        val pxSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            fontSizeSp,
            resources.displayMetrics
        )
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

    fun getMaxScrollX(): Int {
        val gutterW = calculateGutterWidth()
        val available = (width - gutterW - textPaddingStart).coerceAtLeast(0f)
        val contentWidth = cachedMaxLineLength * charWidth + textPaddingStart + 100f
        return max(0, (contentWidth - available).toInt())
    }

    fun getMaxScrollY(): Int {
        val contentHeight = (buffer.lineCount * lineHeight).toInt()
        val visibleHeight = height
        return if (visibleHeight <= 0 || contentHeight <= visibleHeight) {
            0
        } else {
            max(0, contentHeight - (visibleHeight * 0.3f).toInt())
        }
    }

    fun clampScroll() {
        if (height <= 0 || width <= 0) return
        val clampedX = scrollX.coerceIn(0, getMaxScrollX())
        val clampedY = scrollY.coerceIn(0, getMaxScrollY())
        if (clampedX != scrollX || clampedY != scrollY) {
            scrollTo(clampedX, clampedY)
        }
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        onScrollPositionChanged?.invoke(l, t)
    }

    fun setScrollPositions(x: Int, y: Int) {
        if (height <= 0 || width <= 0) {
            post {
                if (height > 0 && width > 0) {
                    scrollTo(x.coerceIn(0, getMaxScrollX()), y.coerceIn(0, getMaxScrollY()))
                    clampScroll()
                    invalidate()
                }
            }
            return
        }
        scrollTo(x.coerceIn(0, getMaxScrollX()), y.coerceIn(0, getMaxScrollY()))
        clampScroll()
        invalidate()
    }

    fun getScrollPositions(): Pair<Int, Int> = Pair(scrollX, scrollY)

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.currX, scroller.currY)
            clampScroll()
            selectionActionMode?.invalidate()
            postInvalidateOnAnimation()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        clampScroll()
        selectionActionMode?.invalidate()
        if (h < oldh && isFocused) {
            post { scrollToCursor() }
        }
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                onEditorFocus?.invoke()
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

                    val startLineStr = buffer.getLine(normStart.line)
                    val endLineStr = buffer.getLine(normEnd.line)
                    val startVisualCol = VisualColumnHelper.charIndexToVisualColumn(startLineStr, normStart.col)
                    val endVisualCol = VisualColumnHelper.charIndexToVisualColumn(endLineStr, normEnd.col)

                    val startCenterX = gutterW + padStart + startVisualCol * charWidth - scrollX - handleRadius / 2f
                    val startCenterY = (normStart.line + 1) * lineHeight - scrollY + handleRadius

                    val endCenterX = gutterW + padStart + endVisualCol * charWidth - scrollX + handleRadius / 2f
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

                if (activeHandleDrag != null || isLongPressDragging) {
                    val density = resources.displayMetrics.density
                    val edgeMargin = 40f * density
                    if (event.y < edgeMargin) {
                        scrollBy(0, -((edgeMargin - event.y) * 0.5f).toInt().coerceAtLeast(1))
                        clampScroll()
                    } else if (event.y > height - edgeMargin) {
                        scrollBy(0, ((event.y - (height - edgeMargin)) * 0.5f).toInt().coerceAtLeast(1))
                        clampScroll()
                    }
                    val gutterW = calculateGutterWidth()
                    if (event.x < gutterW + edgeMargin) {
                        scrollBy(-((gutterW + edgeMargin - event.x) * 0.5f).toInt().coerceAtLeast(1), 0)
                        clampScroll()
                    } else if (event.x > width - edgeMargin) {
                        scrollBy(((event.x - (width - edgeMargin)) * 0.5f).toInt().coerceAtLeast(1), 0)
                        clampScroll()
                    }
                }

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
            val visualCol = (contentX / charWidth).roundToInt()
            VisualColumnHelper.visualColumnToCharIndex(lineStr, visualCol).coerceIn(0, lineStr.length)
        } else {
            0
        }
        return buffer.clampPosition(CursorPos(line, col))
    }

    private fun selectWordAt(pos: CursorPos): Boolean {
        val line = buffer.getLine(pos.line)
        if (line.isEmpty()) {
            selection = SelectionRange(pos, pos)
            cursorPosition = pos
            return false
        }

        val col = pos.col.coerceIn(0, line.length)
        var start = (if (col >= line.length) col - 1 else col).coerceAtLeast(0)
        if (start >= line.length || (!line[start].isLetterOrDigit() && line[start] != '_')) {
            if (start > 0 && (line[start - 1].isLetterOrDigit() || line[start - 1] == '_')) {
                start--
            } else {
                selection = SelectionRange(pos, pos)
                cursorPosition = pos
                return false
            }
        }

        var end = start
        while (start > 0 && (line[start - 1].isLetterOrDigit() || line[start - 1] == '_')) {
            start--
        }
        while (end < line.length && (line[end].isLetterOrDigit() || line[end] == '_')) {
            end++
        }

        if (start == end) {
            selection = SelectionRange(pos, pos)
            cursorPosition = pos
            return false
        }

        selection = SelectionRange(CursorPos(pos.line, start), CursorPos(pos.line, end))
        cursorPosition = selection.end
        notifySelectionAndCursor()
        resetCursorBlink()
        invalidate()
        return true
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
        if (width <= 0 || height <= 0) return
        val gutterW = calculateGutterWidth()
        val padStart = textPaddingStart
        val lineStr = buffer.getLine(cursorPosition.line)
        val visualCol = VisualColumnHelper.charIndexToVisualColumn(lineStr, cursorPosition.col)
        val cursorX = gutterW + padStart + visualCol * charWidth
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

    private fun getMatchingClosingPair(c: Char): Char? = when (c) {
        '(' -> ')'
        '[' -> ']'
        '{' -> '}'
        '"' -> '"'
        '\'' -> '\''
        '`' -> '`'
        else -> null
    }

    fun insertText(text: String) {
        if (ctrlActive) {
            when (text.lowercase()) {
                "a" -> { selectAll(); onResetModifiers?.invoke(); return }
                "c" -> { copySelectedText(); onResetModifiers?.invoke(); return }
                "x" -> { cutSelection(); onResetModifiers?.invoke(); return }
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
                "d" -> { duplicateLines(); onResetModifiers?.invoke(); return }
                "l" -> { selectCurrentLine(); onResetModifiers?.invoke(); return }
                "/" -> { toggleComment(); onResetModifiers?.invoke(); return }
                else -> { onResetModifiers?.invoke(); return }
            }
        }

        // Bracket & quote wrapping when selection is not empty
        if (!selection.isEmpty && text.length == 1) {
            val char = text[0]
            val closing = getMatchingClosingPair(char)
            if (closing != null) {
                val selected = buffer.getSelectedText(selection)
                val wrapped = "$char$selected$closing"
                val startPos = buffer.deleteRange(selection)
                val endPos = buffer.insert(startPos.line, startPos.col, wrapped)
                selection = SelectionRange(startPos, endPos)
                cursorPosition = endPos
                scheduleContentNotification()
                notifySelectionAndCursor()
                resetCursorBlink()
                scrollToCursor()
                invalidate()
                return
            }
        }

        // Auto-close pairs & typeover when typing single char
        if (selection.isEmpty && text.length == 1) {
            val char = text[0]
            val lineStr = buffer.getLine(cursorPosition.line)
            val nextChar = if (cursorPosition.col < lineStr.length) lineStr[cursorPosition.col] else null

            // Typeover closing char: if next char matches, just step over it
            if (nextChar != null && char == nextChar && (char == ')' || char == ']' || char == '}' || char == '"' || char == '\'' || char == '`')) {
                moveCursorRight(false)
                return
            }

            // Auto-closing open pairs
            val closing = getMatchingClosingPair(char)
            if (closing != null) {
                val pair = "$char$closing"
                cursorPosition = buffer.insert(cursorPosition.line, cursorPosition.col, pair)
                // Position cursor inside the pair
                cursorPosition = CursorPos(cursorPosition.line, cursorPosition.col - 1)
                selection = SelectionRange(cursorPosition, cursorPosition)
                scheduleContentNotification()
                notifySelectionAndCursor()
                resetCursorBlink()
                scrollToCursor()
                invalidate()
                return
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
            // Pair deletion: if deleting between (), [], {}, "", '', delete both!
            val lineStr = buffer.getLine(cursorPosition.line)
            if (count == 1 && cursorPosition.col > 0 && cursorPosition.col < lineStr.length) {
                val prevChar = lineStr[cursorPosition.col - 1]
                val nextChar = lineStr[cursorPosition.col]
                if ((prevChar == '(' && nextChar == ')') ||
                    (prevChar == '[' && nextChar == ']') ||
                    (prevChar == '{' && nextChar == '}') ||
                    (prevChar == '"' && nextChar == '"') ||
                    (prevChar == '\'' && nextChar == '\'') ||
                    (prevChar == '`' && nextChar == '`')) {
                    buffer.deleteAfter(cursorPosition, 1)
                    cursorPosition = buffer.deleteBefore(cursorPosition, 1)
                    selection = SelectionRange(cursorPosition, cursorPosition)
                    scheduleContentNotification()
                    notifySelectionAndCursor()
                    resetCursorBlink()
                    scrollToCursor()
                    invalidate()
                    return
                }
            }
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
            cursorPosition = buffer.deleteAfter(cursorPosition, count)
            selection = SelectionRange(cursorPosition, cursorPosition)
        }
        scheduleContentNotification()
        notifySelectionAndCursor()
        resetCursorBlink()
        scrollToCursor()
        invalidate()
    }

    fun insertNewlineWithAutoIndent() {
        val currentLine = buffer.getLine(cursorPosition.line)
        val prefixIndent = currentLine.substring(0, cursorPosition.col.coerceIn(0, currentLine.length)).takeWhile { it == ' ' || it == '\t' }
        val trimmedBefore = currentLine.substring(0, cursorPosition.col.coerceIn(0, currentLine.length)).trimEnd()
        val extraIndent = if (trimmedBefore.endsWith("{") || trimmedBefore.endsWith("(") || trimmedBefore.endsWith("[") ||
            (languageId.equals("python", ignoreCase = true) && trimmedBefore.endsWith(":"))) {
            "    "
        } else {
            ""
        }
        insertText("\n$prefixIndent$extraIndent")
    }

    fun moveCursorLeft(isShift: Boolean) {
        if (!isShift && !selection.isEmpty) {
            val normStart = selection.normalizedStart
            dismissSelectionActionMode()
            cursorPosition = normStart
            selection = SelectionRange(normStart, normStart)
            notifySelectionAndCursor()
            resetCursorBlink()
            scrollToCursor()
            invalidate()
            return
        }
        val newPos = if (cursorPosition.col > 0) {
            val step = buffer.getStepLeftOffset(cursorPosition.line, cursorPosition.col)
            CursorPos(cursorPosition.line, cursorPosition.col - step)
        } else if (cursorPosition.line > 0) {
            val prevLine = cursorPosition.line - 1
            CursorPos(prevLine, buffer.getLineLength(prevLine))
        } else {
            cursorPosition
        }
        updateCursorMove(newPos, isShift)
    }

    fun moveCursorRight(isShift: Boolean) {
        if (!isShift && !selection.isEmpty) {
            val normEnd = selection.normalizedEnd
            dismissSelectionActionMode()
            cursorPosition = normEnd
            selection = SelectionRange(normEnd, normEnd)
            notifySelectionAndCursor()
            resetCursorBlink()
            scrollToCursor()
            invalidate()
            return
        }
        val lineLen = buffer.getLineLength(cursorPosition.line)
        val newPos = if (cursorPosition.col < lineLen) {
            val step = buffer.getStepRightOffset(cursorPosition.line, cursorPosition.col)
            CursorPos(cursorPosition.line, cursorPosition.col + step)
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

    fun moveCursorWordLeft(isShift: Boolean) {
        val line = cursorPosition.line
        val lineStr = buffer.getLine(line)
        var col = cursorPosition.col
        if (col > 0) {
            while (col > 0 && !lineStr[col - 1].isLetterOrDigit() && lineStr[col - 1] != '_') {
                col--
            }
            while (col > 0 && (lineStr[col - 1].isLetterOrDigit() || lineStr[col - 1] == '_')) {
                col--
            }
            updateCursorMove(CursorPos(line, col), isShift)
        } else if (line > 0) {
            val prevLine = line - 1
            updateCursorMove(CursorPos(prevLine, buffer.getLineLength(prevLine)), isShift)
        }
    }

    fun moveCursorWordRight(isShift: Boolean) {
        val line = cursorPosition.line
        val lineStr = buffer.getLine(line)
        var col = cursorPosition.col
        if (col < lineStr.length) {
            while (col < lineStr.length && (lineStr[col].isLetterOrDigit() || lineStr[col] == '_')) {
                col++
            }
            while (col < lineStr.length && !lineStr[col].isLetterOrDigit() && lineStr[col] != '_') {
                col++
            }
            updateCursorMove(CursorPos(line, col), isShift)
        } else if (line < buffer.lineCount - 1) {
            updateCursorMove(CursorPos(line + 1, 0), isShift)
        }
    }

    fun deleteWordBefore() {
        val line = cursorPosition.line
        val lineStr = buffer.getLine(line)
        var col = cursorPosition.col
        if (col > 0) {
            val origCol = col
            while (col > 0 && !lineStr[col - 1].isLetterOrDigit() && lineStr[col - 1] != '_') {
                col--
            }
            while (col > 0 && (lineStr[col - 1].isLetterOrDigit() || lineStr[col - 1] == '_')) {
                col--
            }
            deleteBeforeCursor(origCol - col)
        } else if (line > 0) {
            deleteBeforeCursor(1)
        }
    }

    fun deleteWordAfter() {
        val line = cursorPosition.line
        val lineStr = buffer.getLine(line)
        var col = cursorPosition.col
        if (col < lineStr.length) {
            val origCol = col
            while (col < lineStr.length && (lineStr[col].isLetterOrDigit() || lineStr[col] == '_')) {
                col++
            }
            while (col < lineStr.length && !lineStr[col].isLetterOrDigit() && lineStr[col] != '_') {
                col++
            }
            deleteAfterCursor(col - origCol)
        } else if (line < buffer.lineCount - 1) {
            deleteAfterCursor(1)
        }
    }

    fun cutSelection() {
        if (!selection.isEmpty) {
            copySelectedText()
            deleteSelectedText()
        } else {
            val line = cursorPosition.line
            val lineText = buffer.getLine(line)
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("DroidCode", lineText + "\n"))
            deleteLines()
        }
    }

    fun selectCurrentLine() {
        val line = cursorPosition.line
        val lineLen = buffer.getLineLength(line)
        selection = SelectionRange(CursorPos(line, 0), CursorPos(line, lineLen))
        cursorPosition = selection.end
        notifySelectionAndCursor()
        showSelectionActionMode()
        invalidate()
    }

    fun duplicateLines() {
        if (!selection.isEmpty) {
            val normStart = selection.normalizedStart
            val normEnd = selection.normalizedEnd
            val selectedText = buffer.getSelectedText(selection)
            val newCursor = buffer.insert(normEnd.line, normEnd.col, selectedText)
            selection = SelectionRange(normEnd, newCursor)
            cursorPosition = newCursor
        } else {
            val line = cursorPosition.line
            val lineText = buffer.getLine(line)
            buffer.insert(line, buffer.getLineLength(line), "\n$lineText")
            cursorPosition = CursorPos(line + 1, cursorPosition.col.coerceIn(0, lineText.length))
            selection = SelectionRange(cursorPosition, cursorPosition)
        }
        scheduleContentNotification()
        notifySelectionAndCursor()
        scrollToCursor()
        invalidate()
    }

    fun deleteLines() {
        if (!selection.isEmpty) {
            val normStart = selection.normalizedStart
            val normEnd = selection.normalizedEnd
            val startPos = CursorPos(normStart.line, 0)
            val endPos = if (normEnd.line < buffer.lineCount - 1) {
                CursorPos(normEnd.line + 1, 0)
            } else {
                CursorPos(normEnd.line, buffer.getLineLength(normEnd.line))
            }
            cursorPosition = buffer.deleteRange(SelectionRange(startPos, endPos))
            selection = SelectionRange(cursorPosition, cursorPosition)
        } else {
            val line = cursorPosition.line
            val startPos = CursorPos(line, 0)
            val endPos = if (line < buffer.lineCount - 1) {
                CursorPos(line + 1, 0)
            } else {
                CursorPos(line, buffer.getLineLength(line))
            }
            cursorPosition = buffer.deleteRange(SelectionRange(startPos, endPos))
            selection = SelectionRange(cursorPosition, cursorPosition)
        }
        scheduleContentNotification()
        notifySelectionAndCursor()
        scrollToCursor()
        invalidate()
    }

    fun joinLines() {
        val line = cursorPosition.line
        if (line < buffer.lineCount - 1) {
            val nextLine = buffer.getLine(line + 1).trimStart()
            val currentLine = buffer.getLine(line)
            val col = currentLine.length
            buffer.deleteRange(SelectionRange(CursorPos(line, currentLine.length), CursorPos(line + 1, buffer.getLine(line + 1).length)))
            buffer.insert(line, col, " $nextLine")
            cursorPosition = CursorPos(line, col)
            selection = SelectionRange(cursorPosition, cursorPosition)
            scheduleContentNotification()
            notifySelectionAndCursor()
            scrollToCursor()
            invalidate()
        }
    }

    fun indentSelection() {
        val startLine = if (!selection.isEmpty) selection.normalizedStart.line else cursorPosition.line
        val endLine = if (!selection.isEmpty) selection.normalizedEnd.line else cursorPosition.line
        for (l in startLine..endLine) {
            buffer.insert(l, 0, "    ")
        }
        if (!selection.isEmpty) {
            selection = SelectionRange(
                CursorPos(startLine, selection.normalizedStart.col + 4),
                CursorPos(endLine, selection.normalizedEnd.col + 4)
            )
            cursorPosition = selection.end
        } else {
            cursorPosition = CursorPos(cursorPosition.line, cursorPosition.col + 4)
            selection = SelectionRange(cursorPosition, cursorPosition)
        }
        scheduleContentNotification()
        notifySelectionAndCursor()
        invalidate()
    }

    fun outdentSelection() {
        val startLine = if (!selection.isEmpty) selection.normalizedStart.line else cursorPosition.line
        val endLine = if (!selection.isEmpty) selection.normalizedEnd.line else cursorPosition.line
        for (l in startLine..endLine) {
            val lineStr = buffer.getLine(l)
            val spacesToRemove = minOf(4, lineStr.takeWhile { it == ' ' }.length)
            if (spacesToRemove > 0) {
                buffer.deleteRange(SelectionRange(CursorPos(l, 0), CursorPos(l, spacesToRemove)))
            } else if (lineStr.startsWith("\t")) {
                buffer.deleteRange(SelectionRange(CursorPos(l, 0), CursorPos(l, 1)))
            }
        }
        cursorPosition = buffer.clampPosition(cursorPosition)
        selection = SelectionRange(buffer.clampPosition(selection.start), buffer.clampPosition(selection.end))
        scheduleContentNotification()
        notifySelectionAndCursor()
        invalidate()
    }

    fun toggleComment() {
        val startLine = if (!selection.isEmpty) selection.normalizedStart.line else cursorPosition.line
        val endLine = if (!selection.isEmpty) selection.normalizedEnd.line else cursorPosition.line
        val prefix = when (languageId.lowercase()) {
            "python", "py", "shell", "bash", "sh" -> "# "
            "sql" -> "-- "
            else -> "// "
        }

        var allCommented = true
        for (l in startLine..endLine) {
            val trimmed = buffer.getLine(l).trimStart()
            if (trimmed.isNotEmpty() && !trimmed.startsWith(prefix.trimEnd())) {
                allCommented = false
                break
            }
        }

        for (l in startLine..endLine) {
            val lineStr = buffer.getLine(l)
            if (allCommented) {
                val idx = lineStr.indexOf(prefix.trimEnd())
                if (idx >= 0) {
                    val removeLen = if (lineStr.startsWith(prefix, idx)) prefix.length else prefix.trimEnd().length
                    buffer.deleteRange(SelectionRange(CursorPos(l, idx), CursorPos(l, idx + removeLen)))
                }
            } else {
                val indent = lineStr.takeWhile { it == ' ' || it == '\t' }.length
                buffer.insert(l, indent, prefix)
            }
        }
        cursorPosition = buffer.clampPosition(cursorPosition)
        selection = SelectionRange(buffer.clampPosition(selection.start), buffer.clampPosition(selection.end))
        scheduleContentNotification()
        notifySelectionAndCursor()
        invalidate()
    }

    fun moveLinesUp() {
        val startLine = if (!selection.isEmpty) selection.normalizedStart.line else cursorPosition.line
        val endLine = if (!selection.isEmpty) selection.normalizedEnd.line else cursorPosition.line
        if (startLine > 0) {
            val prevLineText = buffer.getLine(startLine - 1)
            buffer.deleteRange(SelectionRange(CursorPos(startLine - 1, 0), CursorPos(startLine, 0)))
            val insertLine = endLine
            buffer.insert(insertLine, buffer.getLineLength(insertLine), "\n$prevLineText")
            cursorPosition = CursorPos(cursorPosition.line - 1, cursorPosition.col)
            selection = SelectionRange(CursorPos(startLine - 1, selection.start.col), CursorPos(endLine - 1, selection.end.col))
            scheduleContentNotification()
            notifySelectionAndCursor()
            scrollToCursor()
            invalidate()
        }
    }

    fun moveLinesDown() {
        val startLine = if (!selection.isEmpty) selection.normalizedStart.line else cursorPosition.line
        val endLine = if (!selection.isEmpty) selection.normalizedEnd.line else cursorPosition.line
        if (endLine < buffer.lineCount - 1) {
            val nextLineText = buffer.getLine(endLine + 1)
            val deleteEnd = if (endLine + 1 < buffer.lineCount - 1) CursorPos(endLine + 2, 0) else CursorPos(endLine + 1, buffer.getLineLength(endLine + 1))
            buffer.deleteRange(SelectionRange(CursorPos(endLine + 1, 0), deleteEnd))
            buffer.insert(startLine, 0, "$nextLineText\n")
            cursorPosition = CursorPos(cursorPosition.line + 1, cursorPosition.col)
            selection = SelectionRange(CursorPos(startLine + 1, selection.start.col), CursorPos(endLine + 1, selection.end.col))
            scheduleContentNotification()
            notifySelectionAndCursor()
            scrollToCursor()
            invalidate()
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
            onEditorFocus?.invoke()
            resetCursorBlink()
        }
    }

    override fun onCheckIsTextEditor(): Boolean = true

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (!hasWindowFocus) {
            flushContent()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        dismissSelectionActionMode()
        flushContent()
        removeCallbacks(cursorBlinkRunnable)
        removeCallbacks(notifyContentRunnable)
    }

    fun handleKeyEvent(keyCode: Int, event: KeyEvent?): Boolean {
        if (event == null) return false
        val isCtrl = event.isCtrlPressed || ctrlActive
        val isShift = event.isShiftPressed || shiftActive
        val isAlt = event.isAltPressed || altActive

        if (event.action == KeyEvent.ACTION_DOWN) {
            if (isCtrl) {
                if (isShift && keyCode == KeyEvent.KEYCODE_F) {
                    onWorkspaceSearchShortcut?.invoke()
                    onResetModifiers?.invoke()
                    return true
                }
                if (isShift && (keyCode == KeyEvent.KEYCODE_Z || keyCode == KeyEvent.KEYCODE_Y)) {
                    onRedoShortcut?.invoke()
                    onResetModifiers?.invoke()
                    return true
                }
                when (keyCode) {
                    KeyEvent.KEYCODE_A -> { selectAll(); onResetModifiers?.invoke(); return true }
                    KeyEvent.KEYCODE_C -> { copySelectedText(); onResetModifiers?.invoke(); return true }
                    KeyEvent.KEYCODE_X -> { cutSelection(); onResetModifiers?.invoke(); return true }
                    KeyEvent.KEYCODE_V -> { pasteFromClipboard(); onResetModifiers?.invoke(); return true }
                    KeyEvent.KEYCODE_S -> { save(); onResetModifiers?.invoke(); return true }
                    KeyEvent.KEYCODE_Z -> { onUndoShortcut?.invoke(); onResetModifiers?.invoke(); return true }
                    KeyEvent.KEYCODE_Y -> { onRedoShortcut?.invoke(); onResetModifiers?.invoke(); return true }
                    KeyEvent.KEYCODE_P -> { onCommandPaletteShortcut?.invoke(); onResetModifiers?.invoke(); return true }
                    KeyEvent.KEYCODE_F -> { onFindShortcut?.invoke(); onResetModifiers?.invoke(); return true }
                    KeyEvent.KEYCODE_D -> { duplicateLines(); onResetModifiers?.invoke(); return true }
                    KeyEvent.KEYCODE_L -> { selectCurrentLine(); onResetModifiers?.invoke(); return true }
                    KeyEvent.KEYCODE_SLASH -> { toggleComment(); onResetModifiers?.invoke(); return true }
                    KeyEvent.KEYCODE_DPAD_LEFT -> { moveCursorWordLeft(isShift); return true }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> { moveCursorWordRight(isShift); return true }
                    KeyEvent.KEYCODE_DEL -> { deleteWordBefore(); return true }
                    KeyEvent.KEYCODE_FORWARD_DEL -> { deleteWordAfter(); return true }
                    KeyEvent.KEYCODE_MOVE_HOME -> { updateCursorMove(CursorPos(0, 0), isShift); return true }
                    KeyEvent.KEYCODE_MOVE_END -> {
                        val lastL = (buffer.lineCount - 1).coerceAtLeast(0)
                        updateCursorMove(CursorPos(lastL, buffer.getLineLength(lastL)), isShift)
                        return true
                    }
                }
            }

            if (isAlt) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    moveLinesUp()
                    return true
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    moveLinesDown()
                    return true
                }
            }

            when (keyCode) {
                KeyEvent.KEYCODE_MOVE_HOME -> {
                    updateCursorMove(CursorPos(cursorPosition.line, 0), isShift)
                    return true
                }
                KeyEvent.KEYCODE_MOVE_END -> {
                    updateCursorMove(CursorPos(cursorPosition.line, buffer.getLineLength(cursorPosition.line)), isShift)
                    return true
                }
                KeyEvent.KEYCODE_PAGE_UP -> {
                    val targetLine = (cursorPosition.line - 20).coerceAtLeast(0)
                    updateCursorMove(CursorPos(targetLine, cursorPosition.col.coerceIn(0, buffer.getLineLength(targetLine))), isShift)
                    return true
                }
                KeyEvent.KEYCODE_PAGE_DOWN -> {
                    val lastL = (buffer.lineCount - 1).coerceAtLeast(0)
                    val targetLine = (cursorPosition.line + 20).coerceAtMost(lastL)
                    updateCursorMove(CursorPos(targetLine, cursorPosition.col.coerceIn(0, buffer.getLineLength(targetLine))), isShift)
                    return true
                }
                KeyEvent.KEYCODE_ESCAPE -> {
                    dismissSelectionActionMode()
                    selection = SelectionRange(cursorPosition, cursorPosition)
                    notifySelectionAndCursor()
                    invalidate()
                    return true
                }
                KeyEvent.KEYCODE_F3 -> {
                    // Find Next / Previous
                    onFindShortcut?.invoke()
                    return true
                }
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
                    if (isShift) {
                        outdentSelection()
                    } else if (!selection.isEmpty) {
                        indentSelection()
                    } else {
                        insertText("    ")
                    }
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

            // Printable character fallback: hardware keyboard, adb input, IME raw characters
            if (!isCtrl && !isAlt) {
                val unicode = event.unicodeChar
                if (unicode > 31 && unicode != 127) {
                    insertText(unicode.toChar().toString())
                    return true
                }
            }
        }
        return false
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (handleKeyEvent(keyCode, event)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private var currentInputConnection: EditorInputConnection? = null

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        outAttrs.initialSelStart = buffer.positionToOffset(selection.normalizedStart)
        outAttrs.initialSelEnd = buffer.positionToOffset(selection.normalizedEnd)
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN or EditorInfo.IME_ACTION_NONE
        val ic = EditorInputConnection(this)
        currentInputConnection = ic
        return ic
    }

    fun notifyImeSelection() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val selStart = buffer.positionToOffset(selection.normalizedStart)
        val selEnd = buffer.positionToOffset(selection.normalizedEnd)
        val candStart = currentInputConnection?.composingStartOffset ?: -1
        val candEnd = currentInputConnection?.composingEndOffset ?: -1
        imm?.updateSelection(this, selStart, selEnd, candStart, candEnd)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val saveCount = canvas.save()
        val viewLeft = scrollX.toFloat()
        val viewTop = scrollY.toFloat()
        val viewRight = (scrollX + width).toFloat()
        val viewBottom = (scrollY + height).toFloat()
        canvas.clipRect(viewLeft, viewTop, viewRight, viewBottom)

        try {
            // 1. Fill editor background strictly within viewport bounds
            uiPaint.color = theme.backgroundColor
            canvas.drawRect(viewLeft, viewTop, viewRight, viewBottom, uiPaint)

            val gutterW = calculateGutterWidth()
            val padStart = textPaddingStart
            val lineContentStartX = gutterW + padStart
            val firstVisibleLine = (scrollY / lineHeight).toInt().coerceIn(0, (buffer.lineCount - 1).coerceAtLeast(0))
            val lastVisibleLine = ((scrollY + height) / lineHeight + 1).toInt().coerceIn(firstVisibleLine, (buffer.lineCount - 1).coerceAtLeast(0))

            // 2. Draw current line background highlight only if within visible lines
            if (cursorPosition.line in firstVisibleLine..lastVisibleLine) {
                val currentLineTop = cursorPosition.line * lineHeight
                uiPaint.color = theme.currentLineBackgroundColor
                canvas.drawRect(
                    gutterW + scrollX,
                    currentLineTop,
                    viewRight,
                    currentLineTop + lineHeight,
                    uiPaint
                )
            }

            // 3. Draw text selection background
            if (!selection.isEmpty) {
                uiPaint.color = theme.selectionColor
                val normStart = selection.normalizedStart
                val normEnd = selection.normalizedEnd

                for (l in normStart.line..normEnd.line) {
                    if (l in firstVisibleLine..lastVisibleLine) {
                        val lineTop = l * lineHeight
                        val lineBottom = lineTop + lineHeight
                        val lineStr = buffer.getLine(l)

                        val startX = if (l == normStart.line) {
                            lineContentStartX + VisualColumnHelper.charIndexToVisualColumn(lineStr, normStart.col) * charWidth
                        } else {
                            lineContentStartX
                        }
                        val endX = if (l == normEnd.line) {
                            lineContentStartX + VisualColumnHelper.charIndexToVisualColumn(lineStr, normEnd.col) * charWidth
                        } else {
                            lineContentStartX + (VisualColumnHelper.charIndexToVisualColumn(lineStr, lineStr.length) + 1) * charWidth
                        }

                        canvas.drawRect(startX, lineTop, endX, lineBottom, uiPaint)
                    }
                }
            }

            // 3b. Draw search query matches
            val query = searchQuery
            if (!query.isNullOrEmpty()) {
                val matchColor = 0x66FFEB3B.toInt()
                val activeMatchColor = 0xCCFF9800.toInt()
                val qLen = query.length
                val normStart = selection.normalizedStart
                val normEnd = selection.normalizedEnd

                for (l in firstVisibleLine..lastVisibleLine) {
                    val lineStr = buffer.getLine(l)
                    if (lineStr.isEmpty()) continue
                    val lineTop = l * lineHeight
                    val lineBottom = lineTop + lineHeight

                    var matchIdx = lineStr.indexOf(query, 0, ignoreCase = true)
                    while (matchIdx >= 0) {
                        val matchEnd = matchIdx + qLen
                        val startX = lineContentStartX + VisualColumnHelper.charIndexToVisualColumn(lineStr, matchIdx) * charWidth
                        val endX = lineContentStartX + VisualColumnHelper.charIndexToVisualColumn(lineStr, matchEnd) * charWidth

                        val isActive = (!selection.isEmpty && normStart.line == l && normStart.col == matchIdx &&
                                normEnd.line == l && normEnd.col == matchEnd)

                        uiPaint.color = if (isActive) activeMatchColor else matchColor
                        canvas.drawRect(startX, lineTop, endX, lineBottom, uiPaint)

                        matchIdx = lineStr.indexOf(query, matchIdx + 1, ignoreCase = true)
                    }
                }
            }

            // 4. Render visible lines
            val density = resources.displayMetrics.density
            val gutterPaddingRight = 8f * density
            val diffBarWidth = 3f * density

            for (lineIndex in firstVisibleLine..lastVisibleLine) {
                val lineTop = lineIndex * lineHeight
                val baseline = lineTop + baselineOffset
                val lineText = buffer.getLine(lineIndex)

                // Draw line text with syntax highlighting and multi-line state
                val startState = getLineStartState(lineIndex)
                val tokenizeResult = tokenizer.tokenizeLine(lineText, startState)
                setLineEndState(lineIndex, tokenizeResult.endState)
                val tokens = tokenizeResult.tokens
                var currentCol = 0
                val textStartX = gutterW + padStart

                for (token in tokens) {
                    val tokenStartCol = token.startCol.coerceIn(0, lineText.length)
                    val tokenEndCol = token.endCol.coerceIn(tokenStartCol, lineText.length)

                    // Unstyled prefix before token
                    if (tokenStartCol > currentCol) {
                        val startX = textStartX + VisualColumnHelper.charIndexToVisualColumn(lineText, currentCol) * charWidth
                        textPaint.color = theme.textColor
                        canvas.drawText(lineText, currentCol, tokenStartCol, startX, baseline, textPaint)
                    }

                    // Styled token
                    if (tokenEndCol > tokenStartCol) {
                        val startX = textStartX + VisualColumnHelper.charIndexToVisualColumn(lineText, tokenStartCol) * charWidth
                        val paintToUse = if (token.isBold) boldTextPaint else textPaint
                        paintToUse.color = token.color
                        canvas.drawText(lineText, tokenStartCol, tokenEndCol, startX, baseline, paintToUse)
                    }
                    currentCol = maxOf(currentCol, tokenEndCol)
                }

                // Remainder of line after last token
                if (currentCol < lineText.length) {
                    val startX = textStartX + VisualColumnHelper.charIndexToVisualColumn(lineText, currentCol) * charWidth
                    textPaint.color = theme.textColor
                    canvas.drawText(lineText, currentCol, lineText.length, startX, baseline, textPaint)
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
            if (!selection.isEmpty) {
                val normStart = selection.normalizedStart
                val normEnd = selection.normalizedEnd
                val handleRadius = 9f * density
                uiPaint.color = theme.cursorColor

                // Start handle
                if (normStart.line in firstVisibleLine..lastVisibleLine) {
                    val lineStr = buffer.getLine(normStart.line)
                    val startX = gutterW + padStart + VisualColumnHelper.charIndexToVisualColumn(lineStr, normStart.col) * charWidth
                    val startY = (normStart.line + 1) * lineHeight
                    canvas.drawRect(startX - 1f * density, startY - lineHeight, startX + 1f * density, startY, uiPaint)
                    canvas.drawCircle(startX - handleRadius / 2f, startY + handleRadius, handleRadius, uiPaint)
                }

                // End handle
                if (normEnd.line in firstVisibleLine..lastVisibleLine) {
                    val lineStr = buffer.getLine(normEnd.line)
                    val endX = gutterW + padStart + VisualColumnHelper.charIndexToVisualColumn(lineStr, normEnd.col) * charWidth
                    val endY = (normEnd.line + 1) * lineHeight
                    canvas.drawRect(endX - 1f * density, endY - lineHeight, endX + 1f * density, endY, uiPaint)
                    canvas.drawCircle(endX + handleRadius / 2f, endY + handleRadius, handleRadius, uiPaint)
                }
            } else if (isFocused && cursorVisible && cursorPosition.line in firstVisibleLine..lastVisibleLine) {
                val lineStr = buffer.getLine(cursorPosition.line)
                val cursorX = gutterW + padStart + VisualColumnHelper.charIndexToVisualColumn(lineStr, cursorPosition.col) * charWidth
                val cursorY = cursorPosition.line * lineHeight
                val cursorW = 2f * density

                uiPaint.color = theme.cursorColor
                canvas.drawRoundRect(
                    RectF(cursorX, cursorY + 2f * density, cursorX + cursorW, cursorY + lineHeight - 2f * density),
                    1f, 1f,
                    uiPaint
                )
            }
        } finally {
            canvas.restoreToCount(saveCount)
        }
    }
}

