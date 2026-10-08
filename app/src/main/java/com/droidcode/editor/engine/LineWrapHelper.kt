package com.droidcode.editor.engine

/**
 * Visual row representation for wrapped and unwrapped code lines.
 *
 * @param lineIndex 0-based buffer line index.
 * @param startCol Starting character index on the buffer line.
 * @param endCol Ending character index on the buffer line (exclusive).
 * @param isFirstRowOfLine True if this row is the first visual slice of the buffer line (shows line number).
 */
data class VisualRow(
    val lineIndex: Int,
    val startCol: Int,
    val endCol: Int,
    val isFirstRowOfLine: Boolean
)

/**
 * Precalculated visual row layout for soft-wrapping in [CodeEditorView].
 */
data class WrapLayout(
    val rows: List<VisualRow>,
    val lineToFirstRow: IntArray
) {
    val totalRows: Int get() = rows.size

    fun getRow(visualRowIdx: Int): VisualRow {
        if (rows.isEmpty()) return VisualRow(0, 0, 0, true)
        val idx = visualRowIdx.coerceIn(0, rows.size - 1)
        return rows[idx]
    }

    fun findRowIndexForCursor(pos: CursorPos): Int {
        if (rows.isEmpty() || lineToFirstRow.isEmpty()) return pos.line
        val lineIdx = pos.line.coerceIn(0, lineToFirstRow.size - 1)
        val startRowIdx = lineToFirstRow[lineIdx]
        var current = startRowIdx
        while (current < rows.size && rows[current].lineIndex == lineIdx) {
            val row = rows[current]
            val isLastRowOfLine = (current == rows.size - 1 || rows[current + 1].lineIndex != lineIdx)
            if (isLastRowOfLine) {
                if (pos.col >= row.startCol) return current
            } else {
                if (pos.col >= row.startCol && pos.col < row.endCol) return current
            }
            current++
        }
        return startRowIdx
    }
}

/**
 * High-performance line-wrapping calculator for [CodeEditorView].
 *
 * Breaks long buffer lines into visual rows based on viewport column width,
 * prioritizing break boundaries (whitespace, operators, brackets, and punctuation).
 */
object LineWrapHelper {

    fun computeWrapLayout(
        buffer: TextBuffer,
        maxCols: Int
    ): WrapLayout {
        val count = buffer.lineCount
        if (count == 0) {
            val emptyRows = listOf(VisualRow(0, 0, 0, true))
            return WrapLayout(emptyRows, intArrayOf(0))
        }

        val effectiveMaxCols = maxOf(10, maxCols)
        val rows = ArrayList<VisualRow>(count + count / 4)
        val lineToFirstRow = IntArray(count)

        for (lineIdx in 0 until count) {
            lineToFirstRow[lineIdx] = rows.size
            val len = buffer.getLineLength(lineIdx)
            if (len == 0) {
                rows.add(VisualRow(lineIdx, 0, 0, true))
                continue
            }

            val lineStr = buffer.getLine(lineIdx)
            // Fast-path: if character count fits in maxCols and line has no tabs, it fits on 1 row guaranteed
            if (len <= effectiveMaxCols && !lineStr.contains('\t')) {
                rows.add(VisualRow(lineIdx, 0, len, true))
                continue
            }

            val wrappedSlices = wrapLine(lineStr, effectiveMaxCols)
            var isFirst = true
            for (slice in wrappedSlices) {
                rows.add(VisualRow(lineIdx, slice.first, slice.second, isFirst))
                isFirst = false
            }
        }

        return WrapLayout(rows, lineToFirstRow)
    }

    /**
     * Splits a single line of text into character offset slices [start, end) that fit within [maxCols].
     */
    fun wrapLine(lineText: CharSequence, maxCols: Int): List<Pair<Int, Int>> {
        val len = lineText.length
        if (len == 0) return listOf(0 to 0)

        val result = ArrayList<Pair<Int, Int>>()
        var start = 0
        while (start < len) {
            var end = start
            var currentCols = 0
            while (end < len) {
                val c = lineText[end]
                val step = if (c == '\t') VisualColumnHelper.DEFAULT_TAB_WIDTH else 1
                if (currentCols + step > maxCols) {
                    break
                }
                currentCols += step
                end++
            }

            if (end >= len) {
                result.add(start to len)
                break
            }

            // Search backward from end for a word / punctuation break point
            var breakPoint = -1
            for (i in end downTo start + 1) {
                val ch = lineText[i - 1]
                if (isWrapBreakChar(ch)) {
                    breakPoint = i
                    break
                }
            }

            // Accept natural break if it utilizes at least 25% of the row width
            if (breakPoint > start && (breakPoint - start) >= (maxCols * 0.25f)) {
                result.add(start to breakPoint)
                start = breakPoint
            } else {
                // Otherwise force break at end (or advance by at least 1 character to avoid infinite loops)
                val actualEnd = maxOf(start + 1, end)
                result.add(start to actualEnd)
                start = actualEnd
            }
        }
        return result
    }

    private fun isWrapBreakChar(c: Char): Boolean {
        return c == ' ' || c == '\t' || c == ',' || c == ';' || c == ')' ||
               c == '}' || c == ']' || c == '>' || c == ':' || c == '.'
    }
}
