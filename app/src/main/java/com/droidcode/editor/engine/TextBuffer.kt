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
        if (newText.isEmpty()) {
            lines.add(StringBuilder())
        } else {
            val rawLines = newText.split("\n")
            for (line in rawLines) {
                // Strip CR if present
                val sanitized = if (line.endsWith('\r')) line.substring(0, line.length - 1) else line
                lines.add(StringBuilder(sanitized))
            }
        }
        onContentChanged?.invoke(true)
    }

    fun getText(): String {
        val totalLength = lines.sumOf { it.length } + (lines.size - 1).coerceAtLeast(0)
        val sb = java.lang.StringBuilder(totalLength)
        for (i in 0 until lines.size) {
            sb.append(lines[i])
            if (i < lines.size - 1) {
                sb.append('\n')
            }
        }
        return sb.toString()
    }

    fun clampPosition(pos: CursorPos): CursorPos {
        val line = pos.line.coerceIn(0, (lines.size - 1).coerceAtLeast(0))
        val maxCol = if (line in 0 until lines.size) lines[line].length else 0
        val col = pos.col.coerceIn(0, maxCol)
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

        if (!text.contains('\n')) {
            // Fast path: single-line insertion (99.9% of user typing)
            currentLine.insert(targetCol, text)
            onContentChanged?.invoke(false)
            return CursorPos(targetLine, targetCol + text.length)
        }

        // Multi-line insertion (e.g. newline pressed or multi-line paste)
        val remainingTail = currentLine.substring(targetCol)
        currentLine.delete(targetCol, currentLine.length)

        val insertedLines = text.split("\n").map {
            if (it.endsWith('\r')) it.substring(0, it.length - 1) else it
        }

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
     * Deletes [count] characters immediately before [pos] (Backspace).
     * Returns the new cursor position.
     */
    fun deleteBefore(pos: CursorPos, count: Int = 1): CursorPos {
        val clamped = clampPosition(pos)
        var remaining = count
        var currentLine = clamped.line
        var currentCol = clamped.col

        while (remaining > 0) {
            if (currentCol > 0) {
                val deleteInLine = minOf(remaining, currentCol)
                lines[currentLine].delete(currentCol - deleteInLine, currentCol)
                currentCol -= deleteInLine
                remaining -= deleteInLine
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
