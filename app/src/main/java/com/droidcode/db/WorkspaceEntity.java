package com.droidcode.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "workspaces")
public class WorkspaceEntity {

    @PrimaryKey
    @NonNull
    private String path;

    private String name;
    private String type;
    private long lastOpenedTimestamp;
    private boolean isGitRepo;

    public WorkspaceEntity(@NonNull String path, String name, String type, long lastOpenedTimestamp, boolean isGitRepo) {
        this.path = path;
        this.name = name;
        this.type = type;
        this.lastOpenedTimestamp = lastOpenedTimestamp;
        this.isGitRepo = isGitRepo;
    }

    @NonNull
    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public long getLastOpenedTimestamp() {
        return lastOpenedTimestamp;
    }

    public boolean isGitRepo() {
        return isGitRepo;
    }
}
