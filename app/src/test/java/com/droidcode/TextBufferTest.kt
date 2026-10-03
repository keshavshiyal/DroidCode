package com.droidcode

import com.droidcode.editor.engine.CursorPos
import com.droidcode.editor.engine.SelectionRange
import com.droidcode.editor.engine.TextBuffer
import org.junit.Assert.assertEquals
import org.junit.Test

class TextBufferTest {

    @Test
    fun testInitializationAndLineCount() {
        val buffer = TextBuffer("line 1\nline 2\nline 3")
        assertEquals(3, buffer.lineCount)
        assertEquals("line 1", buffer.getLine(0))
        assertEquals("line 2", buffer.getLine(1))
        assertEquals("line 3", buffer.getLine(2))
        assertEquals("line 1\nline 2\nline 3", buffer.getText())
    }

    @Test
    fun testSingleLineInsert() {
        val buffer = TextBuffer("hello world")
        val newPos = buffer.insert(0, 5, ", beautiful")
        assertEquals("hello, beautiful world", buffer.getLine(0))
        assertEquals(CursorPos(0, 16), newPos)
        assertEquals(1, buffer.lineCount)
    }

    @Test
    fun testMultiLineInsert() {
        val buffer = TextBuffer("first\nsecond")
        val newPos = buffer.insert(0, 5, "\nmiddle 1\nmiddle 2")
        assertEquals(4, buffer.lineCount)
        assertEquals("first", buffer.getLine(0))
        assertEquals("middle 1", buffer.getLine(1))
        assertEquals("middle 2", buffer.getLine(2))
        assertEquals("second", buffer.getLine(3))
        assertEquals(CursorPos(2, 8), newPos)
    }

    @Test
    fun testDeleteBeforeWithinLine() {
        val buffer = TextBuffer("hello world")
        val newPos = buffer.deleteBefore(CursorPos(0, 5), 2) // deletes "lo"
        assertEquals("hel world", buffer.getLine(0))
        assertEquals(CursorPos(0, 3), newPos)
    }

    @Test
    fun testDeleteBeforeMergeLines() {
        val buffer = TextBuffer("first\nsecond")
        val newPos = buffer.deleteBefore(CursorPos(1, 0), 1) // deletes '\n'
        assertEquals(1, buffer.lineCount)
        assertEquals("firstsecond", buffer.getLine(0))
        assertEquals(CursorPos(0, 5), newPos)
    }

    @Test
    fun testDeleteRangeMultiLine() {
        val buffer = TextBuffer("line 1: start here\nline 2: to delete\nline 3: keep end")
        val range = SelectionRange(CursorPos(0, 8), CursorPos(2, 8))
        val newPos = buffer.deleteRange(range)
        assertEquals(1, buffer.lineCount)
        assertEquals("line 1: keep end", buffer.getLine(0))
        assertEquals(CursorPos(0, 8), newPos)
    }

    @Test
    fun testPositionAndOffsetConversion() {
        val text = "abc\ndefgh\nijk"
        val buffer = TextBuffer(text)
        // Offset 0 -> (0, 0)
        assertEquals(CursorPos(0, 0), buffer.offsetToPosition(0))
        assertEquals(0, buffer.positionToOffset(CursorPos(0, 0)))

        // Offset 4 -> (1, 0) (start of "defgh")
        assertEquals(CursorPos(1, 0), buffer.offsetToPosition(4))
        assertEquals(4, buffer.positionToOffset(CursorPos(1, 0)))

        // Offset 6 -> (1, 2)
        assertEquals(CursorPos(1, 2), buffer.offsetToPosition(6))
        assertEquals(6, buffer.positionToOffset(CursorPos(1, 2)))

        // Offset 10 -> (2, 0)
        assertEquals(CursorPos(2, 0), buffer.offsetToPosition(10))
        assertEquals(10, buffer.positionToOffset(CursorPos(2, 0)))
    }

    @Test
    fun testTenThousandLinesPerformanceAndAccess() {
        val lineCount = 10_000
        val sb = StringBuilder()
        for (i in 0 until lineCount) {
            sb.append("val variable_").append(i).append(" = ").append(i).append(" * 42;\n")
        }
        val buffer = TextBuffer(sb.toString())
        // sb ends with '\n', so lineCount + 1 lines (last empty line)
        assertEquals(lineCount + 1, buffer.lineCount)

        // Instant O(1) line retrieval
        val line5000 = buffer.getLine(5000)
        assertEquals("val variable_5000 = 5000 * 42;", line5000)

        // Instant single-line mutation in massive file
        val updatedPos = buffer.insert(5000, line5000.length, " // mutated")
        assertEquals("val variable_5000 = 5000 * 42; // mutated", buffer.getLine(5000))
        assertEquals(CursorPos(5000, line5000.length + 11), updatedPos)

        // Viewport tokenization test
        val tokenizer = com.droidcode.editor.engine.LineTokenizer(
            languageId = "kotlin",
            theme = com.droidcode.editor.engine.EditorTheme.darkTheme()
        )
        // Tokenizing 40 visible viewport lines
        for (lineIdx in 5000..5040) {
            val tokens = tokenizer.tokenizeLine(lineIdx, buffer.getLine(lineIdx))
            org.junit.Assert.assertTrue(tokens.isNotEmpty())
        }
    }
}
