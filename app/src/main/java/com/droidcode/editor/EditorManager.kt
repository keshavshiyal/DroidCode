package com.droidcode.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import com.droidcode.filesystem.LocalFileSystem
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EditorManager @Inject constructor() {

    val tabs = mutableStateListOf<EditorTab>()
    private val undoManagers = mutableMapOf<String, UndoManager>()

    var activeTabIndex: Int by mutableIntStateOf(-1)

    private val fileSystem = LocalFileSystem.getInstance()

    val activeTab: EditorTab?
        get() = if (activeTabIndex in 0 until tabs.size) tabs[activeTabIndex] else null

    fun openFile(file: File): EditorTab {
        require(file.exists() && file.isFile) { "File does not exist or is not a valid file: ${file.path}" }

        val path = file.absolutePath
        // Check if already open
        tabs.forEachIndexed { index, tab ->
            if (tab.filePath == path) {
                activeTabIndex = index
                return tab
            }
        }

        val viewerType = EditorTab.detectViewerType(file)
        val content = if (viewerType == FileViewerType.TEXT) {
            try {
                fileSystem.readFileToString(file) ?: ""
            } catch (e: Exception) {
                ""
            }
        } else {
            ""
        }

        val tab = EditorTab(
            filePath = path,
            fileName = file.name,
            initialContent = content,
            initialViewerType = viewerType
        )
        tabs.add(tab)
        activeTabIndex = tabs.size - 1

        val undoMgr = UndoManager()
        undoMgr.pushState(content)
        undoManagers[path] = undoMgr

        return tab
    }

    fun openVirtualFile(fileName: String, content: String): EditorTab {
        val virtualFile = File("/virtual/$fileName")
        val path = virtualFile.absolutePath
        tabs.forEachIndexed { index, tab ->
            if (tab.filePath == path) {
                activeTabIndex = index
                return tab
            }
        }
        val tab = EditorTab(
            filePath = path,
            fileName = fileName,
            initialContent = content,
            initialViewerType = FileViewerType.TEXT
        )
        tabs.add(tab)
        activeTabIndex = tabs.size - 1

        val undoMgr = UndoManager()
        undoMgr.pushState(content)
        undoManagers[path] = undoMgr

        return tab
    }

    fun selectTab(tabId: String) {
        val index = tabs.indexOfFirst { it.id == tabId }
        if (index >= 0) {
            activeTabIndex = index
        }
    }

    fun updateActiveTabContent(newContent: String) {
        val tab = activeTab ?: return
        updateTabContent(tab, newContent)
    }

    fun updateTabContent(tab: EditorTab, newContent: String) {
        tab.updateContent(newContent)
        undoManagers[tab.filePath]?.pushState(newContent)
    }

    fun saveActiveTab() {
        val tab = activeTab ?: return
        saveTab(tab)
    }

    fun saveTab(tab: EditorTab) {
        if (tab.isModified) {
            fileSystem.writeStringToFile(tab.file, tab.content)
            tab.markSaved()
        }
    }

    fun saveAllTabs() {
        tabs.forEach { tab ->
            if (tab.isModified) {
                fileSystem.writeStringToFile(tab.file, tab.content)
                tab.markSaved()
            }
        }
    }

    fun hasUnsavedChanges(): Boolean = tabs.any { it.isModified }

    fun getUnsavedTabs(): List<EditorTab> = tabs.filter { it.isModified }

    fun canUndoActiveTab(): Boolean {
        val tab = activeTab ?: return false
        val undoMgr = undoManagers[tab.filePath] ?: return false
        return undoMgr.canUndo()
    }

    fun canRedoActiveTab(): Boolean {
        val tab = activeTab ?: return false
        val undoMgr = undoManagers[tab.filePath] ?: return false
        return undoMgr.canRedo()
    }

    fun undoActiveTab() {
        val tab = activeTab ?: return
        val undoMgr = undoManagers[tab.filePath] ?: return
        if (undoMgr.canUndo()) {
            val previous = undoMgr.undo(tab.content)
            tab.updateContent(previous)
        }
    }

    fun redoActiveTab() {
        val tab = activeTab ?: return
        val undoMgr = undoManagers[tab.filePath] ?: return
        if (undoMgr.canRedo()) {
            val next = undoMgr.redo(tab.content)
            tab.updateContent(next)
        }
    }

    fun closeTab(index: Int) {
        if (index !in tabs.indices) return
        val removed = tabs.removeAt(index)
        undoManagers.remove(removed.filePath)
        activeTabIndex = when {
            tabs.isEmpty() -> -1
            index < activeTabIndex -> activeTabIndex - 1
            activeTabIndex >= tabs.size -> tabs.size - 1
            else -> activeTabIndex
        }
    }

    fun closeAllTabs() {
        tabs.clear()
        undoManagers.clear()
        activeTabIndex = -1
    }

    fun reloadActiveTab() {
        val tab = activeTab ?: return
        if (tab.file.exists()) {
            val fresh = fileSystem.readFileToString(tab.file) ?: ""
            tab.forceOpenAsText(fresh)
            tab.markSaved()
        }
    }

    fun closeTabsToRight(fromIndex: Int) {
        if (fromIndex in 0 until tabs.size) {
            val lastIdx = tabs.size - 1
            for (i in lastIdx downTo (fromIndex + 1)) {
                closeTab(i)
            }
        }
    }

    fun closeOtherTabs(keepIndex: Int) {
        if (keepIndex !in tabs.indices) return
        val keepTab = tabs[keepIndex]
        val closedPaths = tabs.filter { it !== keepTab }.map { it.filePath }
        closedPaths.forEach { undoManagers.remove(it) }
        tabs.clear()
        tabs.add(keepTab)
        activeTabIndex = 0
    }

    companion object {
        @Volatile
        private var instance: EditorManager? = null

        @JvmStatic
        fun getInstance(): EditorManager {
            return instance ?: synchronized(this) {
                instance ?: EditorManager().also { instance = it }
            }
        }
    }
}
