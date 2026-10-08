package com.droidcode

import com.droidcode.editor.engine.CursorPos
import com.droidcode.editor.engine.LineWrapHelper
import com.droidcode.editor.engine.TextBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LineWrapHelperTest {

    @Test
    fun testWrapEmptyAndShortLine() {
        val emptySlices = LineWrapHelper.wrapLine("", 40)
        assertEquals(1, emptySlices.size)
        assertEquals(0 to 0, emptySlices[0])

        val shortSlices = LineWrapHelper.wrapLine("val a = 1", 40)
        assertEquals(1, shortSlices.size)
        assertEquals(0 to 9, shortSlices[0])
    }

    @Test
    fun testWrapLongLineAtWordBoundary() {
        val line = "val message = \"hello world this is a test for soft wrap\""
        val slices = LineWrapHelper.wrapLine(line, 20)
        assertTrue(slices.size >= 3)
        // Verify contiguous coverage of all characters without gaps or overlapping
        assertEquals(0, slices.first().first)
        assertEquals(line.length, slices.last().second)
        for (i in 0 until slices.size - 1) {
            assertEquals(slices[i].second, slices[i + 1].first)
        }
    }

    @Test
    fun testWrapLongUnbrokenString() {
        val line = "abcdefghijklmnopqrstuvwxyz0123456789"
        val slices = LineWrapHelper.wrapLine(line, 10)
        assertEquals(4, slices.size)
        assertEquals(0 to 10, slices[0])
        assertEquals(10 to 20, slices[1])
        assertEquals(20 to 30, slices[2])
        assertEquals(30 to 36, slices[3])
    }

    @Test
    fun testWrapWithTabs() {
        val line = "\t\tval x = 1234567890"
        // 2 tabs = 8 cols, + 18 chars = 26 cols
        val slices = LineWrapHelper.wrapLine(line, 12)
        assertTrue(slices.size >= 2)
        assertEquals(0, slices.first().first)
        assertEquals(line.length, slices.last().second)
    }

    @Test
    fun testComputeWrapLayoutAndCursorRowLookup() {
        val buffer = TextBuffer()
        buffer.setText("first short line\nval veryLongLine = \"a very long line that will wrap into multiple visual rows when maxCols is small\"\nthird line")
        val layout = LineWrapHelper.computeWrapLayout(buffer, 30)

        // Buffer has 3 lines, but second line should wrap
        assertTrue(layout.totalRows > 3)
        assertEquals(0, layout.getRow(0).lineIndex)
        assertTrue(layout.getRow(0).isFirstRowOfLine)

        // Cursor on line 0
        assertEquals(0, layout.findRowIndexForCursor(CursorPos(0, 5)))

        // Cursor at start of line 1
        val line1StartRow = layout.lineToFirstRow[1]
        assertEquals(line1StartRow, layout.findRowIndexForCursor(CursorPos(1, 0)))

        // Cursor near end of line 1
        val line1Len = buffer.getLineLength(1)
        val line1EndRow = layout.findRowIndexForCursor(CursorPos(1, line1Len))
        assertTrue(line1EndRow > line1StartRow)

        // Cursor on line 2
        val line2Row = layout.lineToFirstRow[2]
        assertEquals(line2Row, layout.findRowIndexForCursor(CursorPos(2, 2)))
    }
}
