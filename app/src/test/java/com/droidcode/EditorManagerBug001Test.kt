package com.droidcode

import com.droidcode.editor.EditorManager
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class EditorManagerBug001Test {

    private lateinit var mgr: EditorManager

    @Before
    fun setUp() {
        mgr = EditorManager()
        mgr.closeAllTabs()
    }

    @Test
    fun `editing tab A after switching to tab B must not modify tab B`() {
        val a = mgr.openVirtualFile("a.kt", "alpha\nbeta\ngamma")
        val b = mgr.openVirtualFile("b.kt", "one\ntwo\nthree")
        mgr.selectTab(b.id)
        mgr.updateTabContent(a, "alpha\ngamma")
        assertEquals("alpha\ngamma", a.content)
        assertEquals("one\ntwo\nthree", b.content)
    }

    @Test
    fun `switching tabs then editing only affects the target tab`() {
        val a = mgr.openVirtualFile("a.kt", "AAA")
        val b = mgr.openVirtualFile("b.kt", "BBB")
        mgr.selectTab(b.id)
        mgr.updateTabContent(b, "BBB!")
        mgr.selectTab(a.id)
        assertEquals("AAA", a.content)
        assertEquals("BBB!", b.content)
    }

    @Test
    fun `undo in tab A does not resurrect content in tab B`() {
        val a = mgr.openVirtualFile("a.kt", "aaa\nbbb\nccc")
        val b = mgr.openVirtualFile("b.kt", "xxx\nyyy\nzzz")
        mgr.selectTab(a.id)
        mgr.updateTabContent(a, "aaa\nccc")
        mgr.undoActiveTab()
        assertEquals("aaa\nbbb\nccc", a.content)
        assertEquals("xxx\nyyy\nzzz", b.content)
    }
}
