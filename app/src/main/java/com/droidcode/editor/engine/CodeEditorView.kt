package com.droidcode.editor.engine

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.SystemClock
import android.text.InputType
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.OverScroller
import com.droidcode.editor.LineDiffStatus
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

    var fontSizeSp: Float = 14f
        set(value) {
            field = value
            updateMetrics()
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

    private val cursorBlinkRunnable = object : Runnable {
        override fun run() {
            if (isFocused) {
                cursorVisible = !cursorVisible
                invalidate()
                postDelayed(this, cursorBlinkInterval)
            }
        }
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
            resetCursorBlink()
            invalidate()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            val pos = screenToCursor(e.x, e.y)
            selectWordAt(pos)
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            scrollBy(distanceX.toInt(), distanceY.toInt())
            clampScroll()
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
            invalidate()
            return true
        }
    })

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        updateMetrics()

        buffer.onContentChanged = {
            invalidate()
            onContentChanged?.invoke(buffer.getText())
        }
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
        return max(0, (maxLen * charWidth + width * 0.5f).toInt())
    }

    private fun getMaxScrollY(): Int {
        return max(0, (buffer.lineCount * lineHeight + height * 0.4f).toInt())
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
            invalidate()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gestureDetector.onTouchEvent(event)) {
            return true
        }

        if (event.action == MotionEvent.ACTION_MOVE && event.historySize > 0) {
            // Drag-to-select
            val pos = screenToCursor(event.x, event.y)
            selection = SelectionRange(selection.start, pos)
            cursorPosition = pos
            scrollToCursor()
            invalidate()
            return true
        }

        return super.onTouchEvent(event)
    }

    private fun screenToCursor(screenX: Float, screenY: Float): CursorPos {
        val gutterW = calculateGutterWidth()
        val contentX = screenX + scrollX - gutterW
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
        cursorPosition = CursorPos(pos.line, end)
        invalidate()
    }

    fun setCursorPosition(pos: CursorPos) {
        setCursorPositionInternal(buffer.clampPosition(pos))
        selection = SelectionRange(cursorPosition, cursorPosition)
        scrollToCursor()
        invalidate()
    }

    private fun setCursorPositionInternal(pos: CursorPos) {
        cursorPosition = pos
        onCursorChanged?.invoke(pos.line + 1, pos.col + 1)
    }

    fun setSelection(start: CursorPos, end: CursorPos) {
        selection = SelectionRange(buffer.clampPosition(start), buffer.clampPosition(end))
        cursorPosition = selection.end
        scrollToCursor()
        invalidate()
    }

    fun scrollToCursor() {
        val gutterW = calculateGutterWidth()
        val cursorX = gutterW + cursorPosition.col * charWidth
        val cursorY = cursorPosition.line * lineHeight

        var targetScrollX = scrollX
        var targetScrollY = scrollY

        val padding = 40f
        if (cursorX < scrollX + gutterW + padding) {
            targetScrollX = max(0, (cursorX - gutterW - padding).toInt())
        } else if (cursorX > scrollX + width - padding) {
            targetScrollX = (cursorX - width + padding).toInt()
        }

        if (cursorY < scrollY + padding) {
            targetScrollY = max(0, (cursorY - padding).toInt())
        } else if (cursorY > scrollY + height - lineHeight - padding) {
            targetScrollY = (cursorY - height + lineHeight + padding).toInt()
        }

        if (targetScrollX != scrollX || targetScrollY != scrollY) {
            scrollTo(targetScrollX, targetScrollY)
            clampScroll()
        }
    }

    fun insertText(text: String) {
        if (ctrlActive) {
            when (text.lowercase()) {
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
        }
        cursorPosition = buffer.insert(cursorPosition.line, cursorPosition.col, text)
        selection = SelectionRange(cursorPosition, cursorPosition)
        scheduleContentNotification()
        onCursorChanged?.invoke(cursorPosition.line + 1, cursorPosition.col + 1)
        resetCursorBlink()
        scrollToCursor()
        invalidate()
    }

    fun deleteBeforeCursor(count: Int = 1) {
        if (!selection.isEmpty) {
            cursorPosition = buffer.deleteRange(selection)
            selection = SelectionRange(cursorPosition, cursorPosition)
        } else {
            cursorPosition = buffer.deleteBefore(cursorPosition, count)
            selection = SelectionRange(cursorPosition, cursorPosition)
        }
        scheduleContentNotification()
        onCursorChanged?.invoke(cursorPosition.line + 1, cursorPosition.col + 1)
        resetCursorBlink()
        scrollToCursor()
        invalidate()
    }

    fun deleteAfterCursor(count: Int = 1) {
        if (!selection.isEmpty) {
            cursorPosition = buffer.deleteRange(selection)
            selection = SelectionRange(cursorPosition, cursorPosition)
        } else {
            val offset = buffer.positionToOffset(cursorPosition)
            val nextPos = buffer.offsetToPosition(offset + count)
            buffer.deleteRange(SelectionRange(cursorPosition, nextPos))
        }
        scheduleContentNotification()
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
            SelectionRange(newPos, newPos)
        }
        onCursorChanged?.invoke(cursorPosition.line + 1, cursorPosition.col + 1)
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

            for (l in normStart.line..normEnd.line) {
                if (l in firstVisibleLine..lastVisibleLine) {
                    val lineTop = l * lineHeight
                    val lineBottom = lineTop + lineHeight
                    val lineStr = buffer.getLine(l)

                    val startX = if (l == normStart.line) gutterW + normStart.col * charWidth else gutterW
                    val endX = if (l == normEnd.line) {
                        gutterW + normEnd.col * charWidth
                    } else {
                        gutterW + (lineStr.length + 1) * charWidth
                    }

                    canvas.drawRect(startX, lineTop, endX, lineBottom, uiPaint)
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

            // Draw line text with syntax highlighting
            val tokens = tokenizer.tokenizeLine(lineText)
            var currentCol = 0
            val textStartX = gutterW

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

        // 6. Draw Blinking Cursor
        if (isFocused && cursorVisible) {
            val cursorX = gutterW + cursorPosition.col * charWidth
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
