package com.example.editor;

import com.example.filesystem.LocalFileSystem;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditorManager {

    private static volatile EditorManager INSTANCE;

    private final List<EditorTab> tabs = new ArrayList<>();
    private final Map<String, UndoManager> undoManagers = new HashMap<>();
    private int activeTabIndex = -1;
    private final LocalFileSystem fileSystem;

    private EditorManager() {
        this.fileSystem = LocalFileSystem.getInstance();
    }

    public static EditorManager getInstance() {
        if (INSTANCE == null) {
            synchronized (EditorManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new EditorManager();
                }
            }
        }
        return INSTANCE;
    }

    public List<EditorTab> getTabs() {
        return tabs;
    }

    public EditorTab getActiveTab() {
        if (activeTabIndex >= 0 && activeTabIndex < tabs.size()) {
            return tabs.get(activeTabIndex);
        }
        return null;
    }

    public int getActiveTabIndex() {
        return activeTabIndex;
    }

    public void setActiveTabIndex(int index) {
        if (index >= 0 && index < tabs.size()) {
            this.activeTabIndex = index;
        } else if (tabs.isEmpty()) {
            this.activeTabIndex = -1;
        }
    }

    public EditorTab openFile(File file) throws Exception {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("File does not exist or is not a valid file");
        }

        String path = file.getAbsolutePath();
        // Check if already open
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).getFilePath().equals(path)) {
                activeTabIndex = i;
                return tabs.get(i);
            }
        }

        String content = fileSystem.readFileToString(file);
        EditorTab tab = new EditorTab(path, file.getName(), content, null);
        tabs.add(tab);
        activeTabIndex = tabs.size() - 1;

        UndoManager undoMgr = new UndoManager();
        undoMgr.pushState(content);
        undoManagers.put(path, undoMgr);

        return tab;
    }

    public void updateActiveTabContent(String newContent) {
        EditorTab tab = getActiveTab();
        if (tab != null) {
            tab.updateContent(newContent);
            UndoManager undoMgr = undoManagers.get(tab.getFilePath());
            if (undoMgr != null) {
                undoMgr.pushState(newContent);
            }
        }
    }

    public void saveActiveTab() throws Exception {
        EditorTab tab = getActiveTab();
        if (tab != null && tab.isModified()) {
            fileSystem.writeStringToFile(tab.getFile(), tab.getContent());
            tab.markSaved();
        }
    }

    public void saveAllTabs() throws Exception {
        for (EditorTab tab : tabs) {
            if (tab.isModified()) {
                fileSystem.writeStringToFile(tab.getFile(), tab.getContent());
                tab.markSaved();
            }
        }
    }

    public void undoActiveTab() {
        EditorTab tab = getActiveTab();
        if (tab != null) {
            UndoManager undoMgr = undoManagers.get(tab.getFilePath());
            if (undoMgr != null && undoMgr.canUndo()) {
                String previous = undoMgr.undo(tab.getContent());
                tab.updateContent(previous);
            }
        }
    }

    public void redoActiveTab() {
        EditorTab tab = getActiveTab();
        if (tab != null) {
            UndoManager undoMgr = undoManagers.get(tab.getFilePath());
            if (undoMgr != null && undoMgr.canRedo()) {
                String next = undoMgr.redo(tab.getContent());
                tab.updateContent(next);
            }
        }
    }

    public void closeTab(int index) {
        if (index >= 0 && index < tabs.size()) {
            EditorTab removed = tabs.remove(index);
            undoManagers.remove(removed.getFilePath());
            if (activeTabIndex >= tabs.size()) {
                activeTabIndex = tabs.size() - 1;
            }
        }
    }

    public void closeAllTabs() {
        tabs.clear();
        undoManagers.clear();
        activeTabIndex = -1;
    }

    public void closeOtherTabs(int keepIndex) {
        if (keepIndex >= 0 && keepIndex < tabs.size()) {
            EditorTab keepTab = tabs.get(keepIndex);
            tabs.clear();
            tabs.add(keepTab);
            activeTabIndex = 0;
        }
    }
}
