package com.droidcode.editor.engine

/**
 * Universal visual column calculator for DroidCodeEngine.
 *
 * Maps between 0-based character offsets in a line and monospace visual column coordinates,
 * taking into account tab stop expansion (e.g. 4 spaces) and double-width glyphs (emojis, CJK).
 */
object VisualColumnHelper {

    const val DEFAULT_TAB_WIDTH = 4

    fun charIndexToVisualColumn(line: CharSequence, charIndex: Int, tabWidth: Int = DEFAULT_TAB_WIDTH): Int {
        var visualCol = 0
        val target = charIndex.coerceIn(0, line.length)
        var i = 0
        while (i < target) {
            val c = line[i]
            if (c == '\t') {
                visualCol = (visualCol / tabWidth + 1) * tabWidth
                i++
            } else if (i + 1 < target && Character.isSurrogatePair(c, line[i + 1])) {
                visualCol += 2
                i += 2
            } else {
                visualCol += 1
                i++
            }
        }
        return visualCol
    }

    fun visualColumnToCharIndex(line: CharSequence, visualColumn: Int, tabWidth: Int = DEFAULT_TAB_WIDTH): Int {
        if (visualColumn <= 0) return 0
        var currentVisualCol = 0
        var i = 0
        val len = line.length
        while (i < len) {
            val c = line[i]
            val stepVisual = if (c == '\t') {
                val nextStop = (currentVisualCol / tabWidth + 1) * tabWidth
                nextStop - currentVisualCol
            } else if (i + 1 < len && Character.isSurrogatePair(c, line[i + 1])) {
                2
            } else {
                1
            }

            if (currentVisualCol + stepVisual / 2 >= visualColumn) {
                return i
            }
            if (currentVisualCol + stepVisual > visualColumn) {
                return i
            }

            currentVisualCol += stepVisual
            if (i + 1 < len && Character.isSurrogatePair(c, line[i + 1])) {
                i += 2
            } else {
                i++
            }
        }
        return len
    }
}
