package com.droidcode.project;

import android.content.Context;

import com.droidcode.db.AppDatabase;
import com.droidcode.db.WorkspaceDao;
import com.droidcode.db.WorkspaceEntity;
import com.droidcode.filesystem.FileNode;
import com.droidcode.filesystem.LocalFileSystem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WorkspaceManager {

    private static volatile WorkspaceManager INSTANCE;

    private Project currentProject;
    private final LocalFileSystem fileSystem;

    private WorkspaceManager() {
        this.fileSystem = LocalFileSystem.getInstance();
    }

    public static WorkspaceManager getInstance() {
        if (INSTANCE == null) {
            synchronized (WorkspaceManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new WorkspaceManager();
                }
            }
        }
        return INSTANCE;
    }

    public Project getCurrentProject() {
        return currentProject;
    }

    public boolean hasOpenWorkspace() {
        return currentProject != null && new File(currentProject.getPath()).exists();
    }

    public void openWorkspace(Context context, File directory, String projectType) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            throw new IllegalArgumentException("Target directory does not exist or is invalid");
        }

        Project project = Project.fromDirectory(directory, projectType);
        if (project != null) {
            this.currentProject = project;

            // Save or update in database
            AppDatabase db = AppDatabase.getInstance(context);
            WorkspaceDao dao = db.workspaceDao();
            dao.insertWorkspace(new WorkspaceEntity(
                    project.getPath(),
                    project.getName(),
                    project.getType(),
                    project.getLastOpenedAt(),
                    project.isGitRepo()
            ));
        }
    }

    public void closeWorkspace() {
        this.currentProject = null;
    }

    public List<FileNode> getWorkspaceFileTree() {
        if (!hasOpenWorkspace()) {
            return new ArrayList<>();
        }
        return fileSystem.listDirectoryRecursive(currentProject.getDirectory());
    }

    public List<WorkspaceEntity> getRecentWorkspaces(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        List<WorkspaceEntity> list = db.workspaceDao().getAllWorkspacesSync();
        return list != null ? list : new ArrayList<>();
    }

    public void removeWorkspace(Context context, String path) {
        if (path != null && context != null) {
            AppDatabase db = AppDatabase.getInstance(context);
            db.workspaceDao().deleteWorkspaceByPath(path);
        }
    }
}
