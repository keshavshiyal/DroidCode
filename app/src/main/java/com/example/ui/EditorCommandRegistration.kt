package com.example.ui

import android.content.ClipboardManager
import android.content.Context
import com.example.core.Command
import com.example.core.CommandRegistry
import com.example.editor.EditorManager
import com.example.editor.FileViewerType
import com.example.git.GitService
import com.example.project.WorkspaceManager
import com.example.settings.AppSettings

object EditorCommandRegistration {

    fun registerAllCommands(
        context: Context,
        editorMgr: EditorManager,
        workspaceMgr: WorkspaceManager,
        settings: AppSettings,
        onOpenCommandPalette: () -> Unit,
        onShowGoToLineDialog: () -> Unit,
        onShowChangeLanguageDialog: () -> Unit,
        onShowChangeEncodingDialog: () -> Unit,
        onShowChangeLineEndingDialog: () -> Unit,
        onShowSaveAsDialog: () -> Unit,
        onShowInfoDialog: (title: String, text: String) -> Unit,
        onOpenTerminalPanel: () -> Unit,
        onExecuteAction: (actionType: String) -> Unit
    ) {
        val registry = CommandRegistry.getInstance()
        registry.clear()

        val isGitRepo = {
            val proj = workspaceMgr.currentProject
            proj != null && GitService.getInstance().inspectWorkspace(proj).isGitRepo
        }

        val hasClipboard = {
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = clipboard.primaryClip
                clip != null && clip.itemCount > 0 && !clip.getItemAt(0).text.isNullOrEmpty()
            } catch (e: Exception) {
                false
            }
        }

        // --- Editing ---
        registry.registerCommand(Command(
            "edit.cut", "Cut", "Editing", "Ctrl+X",
            { editorMgr.activeTab?.viewerType == FileViewerType.TEXT }
        ) { onExecuteAction("CUT") })

        registry.registerCommand(Command(
            "edit.copy", "Copy", "Editing", "Ctrl+C",
            { editorMgr.activeTab != null }
        ) { onExecuteAction("COPY") })

        registry.registerCommand(Command(
            "edit.paste", "Paste", "Editing", "Ctrl+V",
            { editorMgr.activeTab?.viewerType == FileViewerType.TEXT && hasClipboard() }
        ) { onExecuteAction("PASTE") })

        registry.registerCommand(Command(
            "edit.paste_plain", "Paste as Plain Text", "Editing", "Ctrl+Shift+V",
            { editorMgr.activeTab?.viewerType == FileViewerType.TEXT && hasClipboard() }
        ) { onExecuteAction("PASTE_PLAIN") })

        registry.registerCommand(Command(
            "edit.select_all", "Select All", "Editing", "Ctrl+A",
            { editorMgr.activeTab?.content?.isNotEmpty() == true }
        ) { onExecuteAction("SELECT_ALL") })

        registry.registerCommand(Command(
            "edit.select_line", "Select Line", "Editing", "Ctrl+L",
            { editorMgr.activeTab?.viewerType == FileViewerType.TEXT }
        ) { onExecuteAction("SELECT_LINE") })

        registry.registerCommand(Command(
            "edit.duplicate_line", "Duplicate Line/Selection", "Editing", "Alt+Shift+Down",
            { editorMgr.activeTab?.viewerType == FileViewerType.TEXT }
        ) { onExecuteAction("DUPLICATE_LINE") })

        registry.registerCommand(Command(
            "edit.delete_line", "Delete Line", "Editing", "Ctrl+Shift+K",
            { editorMgr.activeTab?.viewerType == FileViewerType.TEXT }
        ) { onExecuteAction("DELETE_LINE") })

        registry.registerCommand(Command(
            "edit.join_lines", "Join Lines", "Editing", "Ctrl+J",
            { editorMgr.activeTab?.viewerType == FileViewerType.TEXT }
        ) { onExecuteAction("JOIN_LINES") })

        registry.registerCommand(Command(
            "editor.undo", "Undo", "Editing", "Ctrl+Z",
            { editorMgr.canUndoActiveTab() }
        ) { editorMgr.undoActiveTab() })

        registry.registerCommand(Command(
            "editor.redo", "Redo", "Editing", "Ctrl+Y",
            { editorMgr.canRedoActiveTab() }
        ) { editorMgr.redoActiveTab() })

        // --- Navigation ---
        registry.registerCommand(Command(
            "nav.go_to_line", "Go to Line...", "Navigation", "Ctrl+G",
            { editorMgr.activeTab?.viewerType == FileViewerType.TEXT }
        ) { onShowGoToLineDialog() })

        registry.registerCommand(Command(
            "nav.go_to_definition", "Go to Definition", "Navigation", "F12",
            { editorMgr.activeTab?.viewerType == FileViewerType.TEXT }
        ) { onExecuteAction("GO_DEFINITION") })

        registry.registerCommand(Command(
            "nav.go_to_declaration", "Go to Declaration", "Navigation", "Ctrl+F12",
            { editorMgr.activeTab?.viewerType == FileViewerType.TEXT }
        ) { onExecuteAction("GO_DEFINITION") })

        registry.registerCommand(Command(
            "nav.go_to_reference", "Go to Reference", "Navigation", "Shift+F12",
            { editorMgr.activeTab?.viewerType == FileViewerType.TEXT }
        ) { onExecuteAction("GO_REFERENCE") })

        registry.registerCommand(Command(
            "nav.find", "Find", "Navigation", "Ctrl+F",
            { editorMgr.activeTab?.viewerType == FileViewerType.TEXT }
        ) { onExecuteAction("SHOW_FIND") })

        registry.registerCommand(Command(
            "nav.find_replace", "Find and Replace", "Navigation", "Ctrl+H",
            { editorMgr.activeTab?.viewerType == FileViewerType.TEXT }
        ) { onExecuteAction("SHOW_REPLACE") })

        registry.registerCommand(Command(
            "nav.find_next", "Find Next", "Navigation", "F3",
            { editorMgr.activeTab?.findQuery?.isNotEmpty() == true }
        ) { onExecuteAction("FIND_NEXT") })

        registry.registerCommand(Command(
            "nav.find_previous", "Find Previous", "Navigation", "Shift+F3",
            { editorMgr.activeTab?.findQuery?.isNotEmpty() == true }
        ) { onExecuteAction("FIND_PREVIOUS") })

        // --- Code ---
        registry.registerCommand(Command(
            "code.format_document", "Format Document", "Code", "Ctrl+Shift+F",
            { editorMgr.activeTab?.viewerType == FileViewerType.TEXT }
        ) { onExecuteAction("FORMAT_DOC") })

        registry.registerCommand(Command(
            "code.format_selection", "Format Selection", "Code", "Ctrl+K Ctrl+F",
            {
                val tab = editorMgr.activeTab
                tab?.viewerType == FileViewerType.TEXT && tab.selectionStart != tab.selectionEnd
            }
        ) { onExecuteAction("FORMAT_DOC") })

        registry.registerCommand(Command(
            "code.toggle_comment", "Comment/Uncomment Line", "Code", "Ctrl+/",
            { editorMgr.activeTab?.viewerType == FileViewerType.TEXT }
        ) { onExecuteAction("TOGGLE_COMMENT") })

        registry.registerCommand(Command(
            "code.toggle_block_comment", "Toggle Block Comment", "Code", "Ctrl+Shift+/",
            { editorMgr.activeTab?.viewerType == FileViewerType.TEXT }
        ) { onExecuteAction("TOGGLE_COMMENT") })

        registry.registerCommand(Command(
            "code.indent", "Indent", "Code", "Ctrl+]",
            { editorMgr.activeTab?.viewerType == FileViewerType.TEXT }
        ) { onExecuteAction("INDENT") })

        registry.registerCommand(Command(
            "code.outdent", "Outdent", "Code", "Ctrl+[",
            { editorMgr.activeTab?.viewerType == FileViewerType.TEXT }
        ) { onExecuteAction("OUTDENT") })

        registry.registerCommand(Command(
            "code.fold", "Fold", "Code", "Ctrl+Shift+[",
            { editorMgr.activeTab?.viewerType == FileViewerType.TEXT }
        ) { onExecuteAction("FOLD") })

        registry.registerCommand(Command(
            "code.unfold", "Unfold", "Code", "Ctrl+Shift+]",
            { editorMgr.activeTab?.viewerType == FileViewerType.TEXT }
        ) { onExecuteAction("UNFOLD") })

        registry.registerCommand(Command(
            "code.fold_all", "Fold All", "Code", "Ctrl+K Ctrl+0",
            { editorMgr.activeTab?.viewerType == FileViewerType.TEXT }
        ) { onExecuteAction("FOLD_ALL") })

        registry.registerCommand(Command(
            "code.unfold_all", "Unfold All", "Code", "Ctrl+K Ctrl+J",
            { editorMgr.activeTab?.viewerType == FileViewerType.TEXT }
        ) { onExecuteAction("UNFOLD_ALL") })

        // --- Editor ---
        registry.registerCommand(Command(
            "command.palette", "Command Palette", "Editor", "Ctrl+Shift+P",
            { true }
        ) { onOpenCommandPalette() })

        registry.registerCommand(Command(
            "editor.toggle_word_wrap", "Toggle Word Wrap", "Editor", "Alt+Z",
            { true }
        ) { settings.isWordWrap = !settings.isWordWrap })

        registry.registerCommand(Command(
            "editor.toggle_line_numbers", "Toggle Line Numbers", "Editor", "Ctrl+Alt+L",
            { true }
        ) { settings.isLineNumbersEnabled = !settings.isLineNumbersEnabled })

        registry.registerCommand(Command(
            "editor.change_language", "Change Language Mode...", "Editor", "Ctrl+K M",
            { editorMgr.activeTab != null }
        ) { onShowChangeLanguageDialog() })

        registry.registerCommand(Command(
            "editor.change_encoding", "Change File Encoding...", "Editor", "Ctrl+K E",
            { editorMgr.activeTab != null }
        ) { onShowChangeEncodingDialog() })

        registry.registerCommand(Command(
            "editor.change_line_ending", "Change Line Ending...", "Editor", "Ctrl+K L",
            { editorMgr.activeTab?.viewerType == FileViewerType.TEXT }
        ) { onShowChangeLineEndingDialog() })

        // --- File ---
        registry.registerCommand(Command(
            "file.save", "Save", "File", "Ctrl+S",
            { editorMgr.activeTab?.isModified == true }
        ) { editorMgr.saveActiveTab() })

        registry.registerCommand(Command(
            "file.save_as", "Save As...", "File", "Ctrl+Shift+S",
            { editorMgr.activeTab != null }
        ) { onShowSaveAsDialog() })

        registry.registerCommand(Command(
            "file.save_all", "Save All", "File", "Ctrl+K S",
            { editorMgr.tabs.any { it.isModified } }
        ) { editorMgr.saveAllTabs() })

        registry.registerCommand(Command(
            "file.revert", "Revert/Reload from Disk", "File", "Ctrl+R",
            { editorMgr.activeTab?.file?.exists() == true }
        ) { editorMgr.reloadActiveTab() })

        registry.registerCommand(Command(
            "file.close_tab", "Close Tab", "File", "Ctrl+W",
            { editorMgr.activeTab != null }
        ) {
            val idx = editorMgr.activeTabIndex
            if (idx >= 0) editorMgr.closeTab(idx)
        })

        registry.registerCommand(Command(
            "file.close_other_tabs", "Close Other Tabs", "File", "Ctrl+K W",
            { editorMgr.tabs.size > 1 }
        ) {
            val idx = editorMgr.activeTabIndex
            if (idx >= 0) editorMgr.closeOtherTabs(idx)
        })

        registry.registerCommand(Command(
            "file.close_tabs_to_right", "Close Tabs to the Right", "File", "Ctrl+K Shift+W",
            { editorMgr.activeTabIndex >= 0 && editorMgr.activeTabIndex < editorMgr.tabs.size - 1 }
        ) {
            val idx = editorMgr.activeTabIndex
            if (idx >= 0) editorMgr.closeTabsToRight(idx)
        })

        // --- Developer Tools ---
        registry.registerCommand(Command(
            "devtools.run", "Run File", "Developer Tools", "F5",
            { editorMgr.activeTab != null }
        ) { onExecuteAction("RUN") })

        registry.registerCommand(Command(
            "devtools.debug", "Debug File", "Developer Tools", "Shift+F5",
            { editorMgr.activeTab != null }
        ) { onExecuteAction("DEBUG") })

        registry.registerCommand(Command(
            "devtools.open_terminal", "Open Terminal Here", "Developer Tools", "Ctrl+`",
            { editorMgr.activeTab != null || workspaceMgr.hasOpenWorkspace() }
        ) { onOpenTerminalPanel() })

        registry.registerCommand(Command(
            "devtools.git_diff", "Git Diff", "Developer Tools", "Ctrl+K D",
            { isGitRepo() }
        ) { onExecuteAction("GIT_DIFF") })

        registry.registerCommand(Command(
            "devtools.git_blame", "Git Blame", "Developer Tools", "Ctrl+K B",
            { isGitRepo() }
        ) { onExecuteAction("GIT_BLAME") })

        registry.registerCommand(Command(
            "devtools.git_history", "Git History", "Developer Tools", "Ctrl+K H",
            { isGitRepo() }
        ) { onExecuteAction("GIT_HISTORY") })
    }
}
