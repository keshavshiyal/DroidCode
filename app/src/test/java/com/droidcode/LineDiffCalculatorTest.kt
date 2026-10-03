package com.droidcode

import com.droidcode.editor.LineDiffCalculator
import com.droidcode.editor.LineDiffStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LineDiffCalculatorTest {

    @Test
    fun testIdenticalContentHasNoDiff() {
        val content = "fun main() {\n    println(\"Hello\")\n}"
        val diff = LineDiffCalculator.computeDiff(content, content)
        assertTrue(diff.isEmpty())
    }

    @Test
    fun testAddedLines() {
        val original = "line 1\nline 3"
        val current = "line 1\nline 2\nline 3"
        val diff = LineDiffCalculator.computeDiff(original, current)
        assertEquals(LineDiffStatus.ADDED, diff[1])
    }

    @Test
    fun testModifiedLines() {
        val original = "line 1\nold line 2\nline 3"
        val current = "line 1\nnew line 2\nline 3"
        val diff = LineDiffCalculator.computeDiff(original, current)
        assertEquals(LineDiffStatus.MODIFIED, diff[1])
    }

    @Test
    fun testEmptyOriginalAllAdded() {
        val original = ""
        val current = "line 1\nline 2"
        val diff = LineDiffCalculator.computeDiff(original, current)
        assertEquals(2, diff.size)
        assertEquals(LineDiffStatus.ADDED, diff[0])
        assertEquals(LineDiffStatus.ADDED, diff[1])
    }
}
