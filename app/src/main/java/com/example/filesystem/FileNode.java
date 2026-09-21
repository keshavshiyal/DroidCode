package com.example.filesystem;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileNode {

    private final String name;
    private final String path;
    private final boolean isFolder;
    private final long size;
    private final long lastModified;
    private boolean isExpanded;
    private final List<FileNode> children;

    public FileNode(@NonNull String name, @NonNull String path, boolean isFolder, long size, long lastModified) {
        this.name = name;
        this.path = path;
        this.isFolder = isFolder;
        this.size = size;
        this.lastModified = lastModified;
        this.isExpanded = false;
        this.children = new ArrayList<>();
    }

    public static FileNode fromFile(File file) {
        if (file == null || !file.exists()) {
            return null;
        }
        return new FileNode(
                file.getName(),
                file.getAbsolutePath(),
                file.isDirectory(),
                file.isFile() ? file.length() : 0,
                file.lastModified()
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

    public boolean isFolder() {
        return isFolder;
    }

    public boolean isDirectory() {
        return isFolder;
    }

    public long getSize() {
        return size;
    }

    public long getLastModified() {
        return lastModified;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

    public List<FileNode> getChildren() {
        return children;
    }

    public void setChildren(List<FileNode> newChildren) {
        this.children.clear();
        if (newChildren != null) {
            this.children.addAll(newChildren);
        }
        sortChildren();
    }

    public void sortChildren() {
        Collections.sort(children, (f1, f2) -> {
            if (f1.isFolder() && !f2.isFolder()) return -1;
            if (!f1.isFolder() && f2.isFolder()) return 1;
            return f1.getName().compareToIgnoreCase(f2.getName());
        });
    }

    public String getExtension() {
        if (isFolder) return "";
        int dotIdx = name.lastIndexOf('.');
        if (dotIdx >= 0 && dotIdx < name.length() - 1) {
            return name.substring(dotIdx + 1).toLowerCase();
        }
        return "";
    }
}
