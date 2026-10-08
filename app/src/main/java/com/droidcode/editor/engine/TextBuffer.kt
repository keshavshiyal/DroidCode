package com.droidcode.editor.engine

/**
 * Cursor position represented by 0-based line and column coordinates.
 */
data class CursorPos(
    val line: Int,
    val col: Int
) : Comparable<CursorPos> {
    override fun compareTo(other: CursorPos): Int {
        val lineDiff = this.line.compareTo(other.line)
        return if (lineDiff != 0) lineDiff else this.col.compareTo(other.col)
    }
}

/**
 * Text selection range defined by start and end positions.
 */
data class SelectionRange(
    val start: CursorPos,
    val end: CursorPos
) {
    val isEmpty: Boolean get() = start == end
    val normalizedStart: CursorPos get() = if (start <= end) start else end
    val normalizedEnd: CursorPos get() = if (start <= end) end else start
}

/**
 * High-performance line-indexed text buffer for DroidCodeEngine.
 *
 * Unlike naive monolithic strings (which copy megabytes on every keystroke),
 * this buffer maintains individual line segments in an ArrayList. Single-line
 * typing operations only touch the active line StringBuilder, delivering O(1)
 * keystroke latency for 100,000+ line files.
 */
class TextBuffer(initialText: String = "") {

    private val lines = ArrayList<StringBuilder>()

    var onContentChanged: ((isStructural: Boolean) -> Unit)? = null

    init {
        setText(initialText)
    }

    val lineCount: Int
        get() = lines.size

    fun getLine(index: Int): String {
        if (index in 0 until lines.size) {
            return lines[index].toString()
        }
        return ""
    }

    fun getLineLength(index: Int): Int {
        if (index in 0 until lines.size) {
            return lines[index].length
        }
        return 0
    }

    fun setText(newText: String) {
        lines.clear()
        val split = splitLines(newText)
        for (l in split) {
            lines.add(StringBuilder(l))
        }
        onContentChanged?.invoke(true)
    }

    fun getText(lineEnding: String = "\n"): String {
        val totalLength = lines.sumOf { it.length } + (lines.size - 1).coerceAtLeast(0) * lineEnding.length
        val sb = java.lang.StringBuilder(totalLength)
        for (i in 0 until lines.size) {
            sb.append(lines[i])
            if (i < lines.size - 1) {
                sb.append(lineEnding)
            }
        }
        return sb.toString()
    }

    fun clampPosition(pos: CursorPos): CursorPos {
        val line = pos.line.coerceIn(0, (lines.size - 1).coerceAtLeast(0))
        val maxCol = if (line in 0 until lines.size) lines[line].length else 0
        var col = pos.col.coerceIn(0, maxCol)
        if (line in 0 until lines.size) {
            val lineSb = lines[line]
            if (col > 0 && col < lineSb.length && Character.isLowSurrogate(lineSb[col]) && Character.isHighSurrogate(lineSb[col - 1])) {
                col = minOf(col + 1, lineSb.length)
            }
        }
        return CursorPos(line, col)
    }

    /**
     * Inserts text at the specified line and column.
     * Returns the new cursor position after the inserted text.
     */
    fun insert(line: Int, col: Int, text: String): CursorPos {
        if (text.isEmpty()) return CursorPos(line, col)

        val targetLine = line.coerceIn(0, (lines.size - 1).coerceAtLeast(0))
        val currentLine = lines[targetLine]
        val targetCol = col.coerceIn(0, currentLine.length)

        if (!text.contains('\n') && !text.contains('\r')) {
            // Fast path: single-line insertion (99.9% of user typing)
            currentLine.insert(targetCol, text)
            onContentChanged?.invoke(false)
            return CursorPos(targetLine, targetCol + text.length)
        }

        // Multi-line insertion (e.g. newline pressed or multi-line paste)
        val remainingTail = currentLine.substring(targetCol)
        currentLine.delete(targetCol, currentLine.length)

        val insertedLines = splitLines(text)

        currentLine.append(insertedLines[0])

        var insertIndex = targetLine + 1
        for (i in 1 until insertedLines.size - 1) {
            lines.add(insertIndex, StringBuilder(insertedLines[i]))
            insertIndex++
        }

        val lastInsertedLine = StringBuilder(insertedLines.last())
        val endCol = lastInsertedLine.length
        lastInsertedLine.append(remainingTail)
        lines.add(insertIndex, lastInsertedLine)

        val finalLine = targetLine + insertedLines.size - 1
        onContentChanged?.invoke(true)
        return CursorPos(finalLine, endCol)
    }

    /**
     * Deletes [count] code points / characters immediately before [pos] (Backspace).
     * Surrogate pairs are treated as a single unit so emojis / supplementary characters are never split.
     * Returns the new cursor position.
     */
    fun deleteBefore(pos: CursorPos, count: Int = 1): CursorPos {
        val clamped = clampPosition(pos)
        var remaining = count
        var currentLine = clamped.line
        var currentCol = clamped.col

        while (remaining > 0) {
            if (currentCol > 0) {
                val lineSb = lines[currentLine]
                val deleteInLine = if (currentCol >= 2 && Character.isSurrogatePair(lineSb[currentCol - 2], lineSb[currentCol - 1])) {
                    2
                } else {
                    1
                }
                lineSb.delete(currentCol - deleteInLine, currentCol)
                currentCol -= deleteInLine
                remaining--
            } else if (currentLine > 0) {
                // Merge current line with previous line
                val prevLine = currentLine - 1
                val prevLineLength = lines[prevLine].length
                lines[prevLine].append(lines[currentLine])
                lines.removeAt(currentLine)
                currentLine = prevLine
                currentCol = prevLineLength
                remaining--
            } else {
                break
            }
        }

        onContentChanged?.invoke(count > 1 || clamped.col == 0)
        return CursorPos(currentLine, currentCol)
    }

    /**
     * Deletes [count] code points / characters immediately after [pos] (Forward Delete).
     * Surrogate pairs are treated as a single unit so emojis / supplementary characters are never split.
     * Returns the new cursor position.
     */
    fun deleteAfter(pos: CursorPos, count: Int = 1): CursorPos {
        val clamped = clampPosition(pos)
        var remaining = count
        var currentLine = clamped.line
        var currentCol = clamped.col

        while (remaining > 0) {
            val lineLen = lines[currentLine].length
            if (currentCol < lineLen) {
                val lineSb = lines[currentLine]
                val deleteInLine = if (currentCol + 1 < lineLen && Character.isSurrogatePair(lineSb[currentCol], lineSb[currentCol + 1])) {
                    2
                } else {
                    1
                }
                lineSb.delete(currentCol, currentCol + deleteInLine)
                remaining--
            } else if (currentLine < lines.size - 1) {
                // Merge next line into current line
                val nextLine = currentLine + 1
                lines[currentLine].append(lines[nextLine])
                lines.removeAt(nextLine)
                remaining--
            } else {
                break
            }
        }

        onContentChanged?.invoke(count > 1 || clamped.col >= lines[currentLine].length)
        return CursorPos(currentLine, currentCol)
    }

    fun getStepLeftOffset(line: Int, col: Int): Int {
        if (line !in 0 until lines.size) return 1
        val lineSb = lines[line]
        return if (col >= 2 && Character.isSurrogatePair(lineSb[col - 2], lineSb[col - 1])) 2 else 1
    }

    fun getStepRightOffset(line: Int, col: Int): Int {
        if (line !in 0 until lines.size) return 1
        val lineSb = lines[line]
        return if (col + 1 < lineSb.length && Character.isSurrogatePair(lineSb[col], lineSb[col + 1])) 2 else 1
    }

    private fun splitLines(text: String): List<String> {
        val result = ArrayList<String>()
        if (text.isEmpty()) {
            result.add("")
            return result
        }
        var i = 0
        val len = text.length
        var lineStart = 0
        while (i < len) {
            val c = text[i]
            if (c == '\r') {
                result.add(text.substring(lineStart, i))
                if (i + 1 < len && text[i + 1] == '\n') {
                    i++
                }
                lineStart = i + 1
            } else if (c == '\n') {
                result.add(text.substring(lineStart, i))
                lineStart = i + 1
            }
            i++
        }
        result.add(text.substring(lineStart, len))
        return result
    }

    /**
     * Deletes a text selection range.
     * Returns the cursor position at the merged start point.
     */
    fun deleteRange(range: SelectionRange): CursorPos {
        if (range.isEmpty) return range.start

        val start = clampPosition(range.normalizedStart)
        val end = clampPosition(range.normalizedEnd)

        if (start.line == end.line) {
            lines[start.line].delete(start.col, end.col)
            onContentChanged?.invoke(false)
            return start
        }

        val firstLine = lines[start.line]
        firstLine.delete(start.col, firstLine.length)

        val lastLine = lines[end.line]
        val remainingTail = lastLine.substring(end.col)

        // Remove intermediate lines
        for (i in end.line downTo start.line + 1) {
            lines.removeAt(i)
        }

        firstLine.append(remainingTail)
        onContentChanged?.invoke(true)
        return start
    }

    fun getSelectedText(range: SelectionRange): String {
        if (range.isEmpty) return ""
        val start = clampPosition(range.normalizedStart)
        val end = clampPosition(range.normalizedEnd)

        if (start.line == end.line) {
            return lines[start.line].substring(start.col, end.col)
        }

        val sb = java.lang.StringBuilder()
        sb.append(lines[start.line].substring(start.col)).append('\n')
        for (i in start.line + 1 until end.line) {
            sb.append(lines[i]).append('\n')
        }
        sb.append(lines[end.line].substring(0, end.col))
        return sb.toString()
    }

    val totalLength: Int
        get() = lines.sumOf { it.length } + (lines.size - 1).coerceAtLeast(0)

    fun getTextInRange(startOffset: Int, endOffset: Int): String {
        val total = totalLength
        val start = startOffset.coerceIn(0, total)
        val end = endOffset.coerceIn(start, total)
        if (start == end) return ""
        val startPos = offsetToPosition(start)
        val endPos = offsetToPosition(end)
        return getSelectedText(SelectionRange(startPos, endPos))
    }

    fun positionToOffset(pos: CursorPos): Int {
        val clamped = clampPosition(pos)
        var offset = 0
        for (i in 0 until clamped.line) {
            offset += lines[i].length + 1
        }
        offset += clamped.col
        return offset
    }

    fun offsetToPosition(offset: Int): CursorPos {
        var currentOffset = 0
        for (i in 0 until lines.size) {
            val lineLen = lines[i].length
            if (offset <= currentOffset + lineLen) {
                val col = (offset - currentOffset).coerceIn(0, lineLen)
                return CursorPos(i, col)
            }
            currentOffset += lineLen + 1
        }
        val lastLine = (lines.size - 1).coerceAtLeast(0)
        return CursorPos(lastLine, lines[lastLine].length)
    }
}
