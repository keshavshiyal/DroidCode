package com.droidcode

import com.droidcode.editor.EditorManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class EditorManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var editorMgr: EditorManager

    @Before
    fun setUp() {
        editorMgr = EditorManager.getInstance()
        editorMgr.closeAllTabs()
    }

    @Test
    fun testOpenTabAndSwitching() {
        val file1 = tempFolder.newFile("main.kt")
        val file2 = tempFolder.newFile("app.js")
        file1.writeText("fun main() {}")
        file2.writeText("console.log('hi');")

        assertEquals(0, editorMgr.tabs.size)
        assertEquals(-1, editorMgr.activeTabIndex)

        val tab1 = editorMgr.openFile(file1)
        assertEquals(1, editorMgr.tabs.size)
        assertEquals(0, editorMgr.activeTabIndex)
        assertEquals("main.kt", editorMgr.activeTab?.fileName)
        assertEquals("fun main() {}", editorMgr.activeTab?.content)

        val tab2 = editorMgr.openFile(file2)
        assertEquals(2, editorMgr.tabs.size)
        assertEquals(1, editorMgr.activeTabIndex)
        assertEquals("app.js", editorMgr.activeTab?.fileName)

        // Opening existing file should switch to it rather than create duplicate
        editorMgr.openFile(file1)
        assertEquals(2, editorMgr.tabs.size)
        assertEquals(0, editorMgr.activeTabIndex)
    }

    @Test
    fun testContentUpdatesAndDirtyState() {
        val file = tempFolder.newFile("test.txt")
        file.writeText("initial content")

        val tab = editorMgr.openFile(file)
        assertFalse(tab.isModified)

        editorMgr.updateActiveTabContent("updated content")
        assertTrue(tab.isModified)
        assertEquals("updated content", tab.content)

        // Save active tab
        editorMgr.saveActiveTab()
        assertFalse(tab.isModified)
        assertEquals("updated content", file.readText())
    }

    @Test
    fun testUndoAndRedo() {
        val file = tempFolder.newFile("notes.txt")
        file.writeText("A")

        editorMgr.openFile(file)
        editorMgr.updateActiveTabContent("B")
        editorMgr.updateActiveTabContent("C")

        assertTrue(editorMgr.canUndoActiveTab())
        editorMgr.undoActiveTab()
        assertEquals("B", editorMgr.activeTab?.content)

        editorMgr.undoActiveTab()
        assertEquals("A", editorMgr.activeTab?.content)

        assertTrue(editorMgr.canRedoActiveTab())
        editorMgr.redoActiveTab()
        assertEquals("B", editorMgr.activeTab?.content)
    }

    @Test
    fun testCloseTab() {
        val file1 = tempFolder.newFile("file1.txt")
        val file2 = tempFolder.newFile("file2.txt")
        editorMgr.openFile(file1)
        editorMgr.openFile(file2)

        assertEquals(2, editorMgr.tabs.size)
        editorMgr.closeTab(0)
        assertEquals(1, editorMgr.tabs.size)
        assertEquals("file2.txt", editorMgr.activeTab?.fileName)
    }
}
