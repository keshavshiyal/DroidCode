package com.droidcode

import com.droidcode.editor.UndoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UndoManagerTest {

    private lateinit var undoManager: UndoManager

    @Before
    fun setUp() {
        undoManager = UndoManager()
    }

    @Test
    fun testBasicUndoAndRedo() {
        assertFalse(undoManager.canUndo())
        assertFalse(undoManager.canRedo())

        undoManager.pushState("hello")
        undoManager.pushState("hello world")

        assertTrue(undoManager.canUndo())
        assertFalse(undoManager.canRedo())

        val undone = undoManager.undo("hello world")
        assertEquals("hello", undone)
        assertTrue(undoManager.canRedo())

        val redone = undoManager.redo(undone)
        assertEquals("hello world", redone)
    }

    @Test
    fun testCursorOffsetRestorationOnUndoAndRedo() {
        // Initial state at cursor 0
        undoManager.pushState("fun main() {}", 0, 0)

        // User typed inside braces at cursor 11
        val state2 = "fun main() {\n    println(1)\n}"
        undoManager.pushState(state2, 11, 26)

        assertTrue(undoManager.canUndo())

        // Undo should revert text to state 1 and restore prior cursor at 11
        val undoResult = undoManager.undoWithCursor(state2)
        assertEquals("fun main() {}", undoResult.text)
        assertEquals(11, undoResult.cursorOffset)

        // Redo should revert text to state 2 and restore new cursor at 26
        val redoResult = undoManager.redoWithCursor(undoResult.text)
        assertEquals(state2, redoResult.text)
        assertEquals(26, redoResult.cursorOffset)
    }

    @Test
    fun testComputeDeltaCharacters() {
        val delta = UndoManager.computeDelta("const x = 10;", "const x = 20;", 10, 11)
        assertEquals(10, delta.offset)
        assertEquals("1", delta.deletedText)
        assertEquals("2", delta.insertedText)
        assertEquals(10, delta.priorCursorOffset)
        assertEquals(11, delta.newCursorOffset)
    }

    @Test
    fun testIdenticalStateIgnored() {
        undoManager.pushState("hello")
        undoManager.pushState("hello")

        assertFalse(undoManager.canUndo())
    }

    @Test
    fun testMultiStepUndoHistory() {
        undoManager.pushState("step 0")
        undoManager.pushState("step 1")
        undoManager.pushState("step 2")
        undoManager.pushState("step 3")

        var text = "step 3"
        text = undoManager.undo(text)
        assertEquals("step 2", text)

        text = undoManager.undo(text)
        assertEquals("step 1", text)

        text = undoManager.undo(text)
        assertEquals("step 0", text)

        assertFalse(undoManager.canUndo())
        assertTrue(undoManager.canRedo())

        text = undoManager.redo(text)
        assertEquals("step 1", text)

        text = undoManager.redo(text)
        assertEquals("step 2", text)

        text = undoManager.redo(text)
        assertEquals("step 3", text)

        assertFalse(undoManager.canRedo())
    }

    @Test
    fun testClearResetsEverything() {
        undoManager.pushState("a")
        undoManager.pushState("b")
        assertTrue(undoManager.canUndo())

        undoManager.clear()
        assertFalse(undoManager.canUndo())
        assertFalse(undoManager.canRedo())
    }
}
