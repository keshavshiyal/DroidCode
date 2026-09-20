package com.example.project;

import androidx.annotation.NonNull;

import java.io.File;

public class Project {

    private final String name;
    private final String path;
    private final String type;
    private final long createdAt;
    private long lastOpenedAt;
    private final boolean isGitRepo;

    public Project(@NonNull String name, @NonNull String path, String type, long createdAt, long lastOpenedAt, boolean isGitRepo) {
        this.name = name;
        this.path = path;
        this.type = type != null ? type : "General";
        this.createdAt = createdAt;
        this.lastOpenedAt = lastOpenedAt;
        this.isGitRepo = isGitRepo;
    }

    public static Project fromDirectory(File dir, String type) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return null;
        }
        File gitDir = new File(dir, ".git");
        return new Project(
                dir.getName(),
                dir.getAbsolutePath(),
                type != null ? type : "General Workspace",
                dir.lastModified(),
                System.currentTimeMillis(),
                gitDir.exists() && gitDir.isDirectory()
        );
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getPath() {
        return path;
    }

    public String getType() {
        return type;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getLastOpenedAt() {
        return lastOpenedAt;
    }

    public void updateLastOpenedAt() {
        this.lastOpenedAt = System.currentTimeMillis();
    }

    public boolean isGitRepo() {
        return isGitRepo;
    }

    public File getDirectory() {
        return new File(path);
    }
}
