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

    var onFlushRequested: (() -> Unit)? = null

    fun flushActiveEditor() {
        onFlushRequested?.invoke()
    }

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
        var readError: String? = null
        var detectedEncoding = "UTF-8"
        var hasBom = false
        val content = if (viewerType == FileViewerType.TEXT) {
            try {
                val result = fileSystem.readFileWithMetadata(file)
                detectedEncoding = result.encoding
                hasBom = result.hasBom
                result.content
            } catch (e: Exception) {
                readError = e.message ?: "Failed to read file from disk"
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
        ).apply {
            this.encoding = detectedEncoding
            this.hasBom = hasBom
            if (readError != null) {
                this.isReadError = true
                this.isReadOnly = true
                this.readErrorMessage = readError
            }
        }
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
        flushActiveEditor()
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
        val priorCursor = tab.cursorPosition
        tab.updateContent(newContent)
        undoManagers[tab.filePath]?.pushState(newContent, priorCursor, tab.cursorPosition)
    }

    fun updateTabContentByPath(filePath: String, newContent: String) {
        val tab = tabs.firstOrNull { it.filePath == filePath } ?: return
        updateTabContent(tab, newContent)
    }

    fun saveActiveTab(): Boolean {
        flushActiveEditor()
        val tab = activeTab ?: return false
        return saveTab(tab)
    }

    fun saveTab(tab: EditorTab): Boolean {
        flushActiveEditor()
        if (tab.isReadError || tab.isReadOnly) {
            android.util.Log.e("EditorManager", "Cannot save file with read error or read-only status: ${tab.filePath}")
            return false
        }
        if (tab.isModified) {
            return try {
                val contentToWrite = tab.getContentWithLineEndings()
                fileSystem.writeStringToFile(tab.file, contentToWrite, tab.encoding, tab.hasBom)
                tab.markSaved()
                true
            } catch (e: Exception) {
                android.util.Log.e("EditorManager", "Failed to save file: ${tab.filePath}", e)
                // Retain tab.isModified = true so user doesn't lose data
                false
            }
        }
        return true
    }

    fun saveAllTabs(): Boolean {
        flushActiveEditor()
        var allSuccess = true
        tabs.forEach { tab ->
            if (tab.isModified) {
                val ok = saveTab(tab)
                if (!ok) allSuccess = false
            }
        }
        return allSuccess
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

    fun undoTab(tab: EditorTab) {
        flushActiveEditor()
        val undoMgr = undoManagers[tab.filePath] ?: return
        if (undoMgr.canUndo()) {
            val result = undoMgr.undoWithCursor(tab.content)
            tab.updateContent(result.text)
            if (result.cursorOffset >= 0) {
                tab.requestSelection(result.cursorOffset, result.cursorOffset)
            }
        }
    }

    fun redoTab(tab: EditorTab) {
        flushActiveEditor()
        val undoMgr = undoManagers[tab.filePath] ?: return
        if (undoMgr.canRedo()) {
            val result = undoMgr.redoWithCursor(tab.content)
            tab.updateContent(result.text)
            if (result.cursorOffset >= 0) {
                tab.requestSelection(result.cursorOffset, result.cursorOffset)
            }
        }
    }

    fun undoActiveTab() {
        val tab = activeTab ?: return
        undoTab(tab)
    }

    fun redoActiveTab() {
        val tab = activeTab ?: return
        redoTab(tab)
    }

    fun closeTab(index: Int) {
        flushActiveEditor()
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
        flushActiveEditor()
        tabs.clear()
        undoManagers.clear()
        activeTabIndex = -1
    }

    fun reloadActiveTab() {
        val tab = activeTab ?: return
        if (tab.file.exists()) {
            try {
                val result = fileSystem.readFileWithMetadata(tab.file)
                tab.encoding = result.encoding
                tab.hasBom = result.hasBom
                tab.forceOpenAsText(result.content)
                tab.isReadError = false
                tab.readErrorMessage = null
                tab.markSaved()
            } catch (e: Exception) {
                android.util.Log.e("EditorManager", "Failed to reload tab: ${tab.filePath}", e)
            }
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
