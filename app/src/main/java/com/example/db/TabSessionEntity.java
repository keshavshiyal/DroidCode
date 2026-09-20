package com.example.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tab_sessions")
public class TabSessionEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String workspacePath;
    private String filePath;
    private int tabOrder;
    private int cursorPosition;
    private boolean isPinned;

    public TabSessionEntity(String workspacePath, String filePath, int tabOrder, int cursorPosition, boolean isPinned) {
        this.workspacePath = workspacePath;
        this.filePath = filePath;
        this.tabOrder = tabOrder;
        this.cursorPosition = cursorPosition;
        this.isPinned = isPinned;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getWorkspacePath() {
        return workspacePath;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getTabOrder() {
        return tabOrder;
    }

    public int getCursorPosition() {
        return cursorPosition;
    }

    public boolean isPinned() {
        return isPinned;
    }
}
