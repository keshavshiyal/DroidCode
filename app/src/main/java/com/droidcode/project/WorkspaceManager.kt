package com.droidcode.project

import android.content.Context
import com.droidcode.db.AppDatabase
import com.droidcode.db.WorkspaceEntity
import com.droidcode.filesystem.FileNode
import com.droidcode.filesystem.LocalFileSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkspaceManager @Inject constructor() {

    @Volatile
    private var _currentProject: Project? = null

    val currentProject: Project?
        get() = _currentProject

    private val _currentProjectFlow = MutableStateFlow<Project?>(null)
    val currentProjectFlow: StateFlow<Project?> = _currentProjectFlow.asStateFlow()

    private val _treeVersion = MutableStateFlow(0)
    val treeVersion: StateFlow<Int> = _treeVersion.asStateFlow()

    fun notifyTreeChanged() {
        _treeVersion.value += 1
    }

    private val fileSystem: LocalFileSystem = LocalFileSystem.getInstance()

    val hasOpenWorkspace: Boolean
        get() {
            val proj = _currentProject ?: return false
            return File(proj.path).exists()
        }

    fun hasOpenWorkspace(): Boolean = hasOpenWorkspace

    suspend fun openWorkspace(context: Context, directory: File?, projectType: String?) = withContext(Dispatchers.IO) {
        if (directory == null || !directory.exists() || !directory.isDirectory) {
            throw IllegalArgumentException("Target directory does not exist or is invalid")
        }

        val project = Project.fromDirectory(directory, projectType)
        if (project != null) {
            _currentProject = project
            _currentProjectFlow.value = project
            _treeVersion.value += 1

            val db = AppDatabase.getInstance(context)
            db.workspaceDao().insertWorkspace(
                WorkspaceEntity(
                    project.path,
                    project.name,
                    project.type,
                    project.lastOpenedAt,
                    project.isGitRepo
                )
            )
        }
    }

    fun closeWorkspace() {
        _currentProject = null
        _currentProjectFlow.value = null
        _treeVersion.value += 1
    }

    val workspaceFileTree: List<FileNode>
        get() {
            val proj = _currentProject ?: return emptyList()
            if (!File(proj.path).exists()) return emptyList()
            return fileSystem.listDirectoryRecursive(proj.directory)
        }

    fun getRecentWorkspacesFlow(context: Context): Flow<List<WorkspaceEntity>> {
        val db = AppDatabase.getInstance(context)
        return db.workspaceDao().getAllWorkspacesFlow()
    }

    suspend fun getRecentWorkspaces(context: Context): List<WorkspaceEntity> = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        db.workspaceDao().getAllWorkspaces()
    }

    suspend fun removeWorkspace(context: Context, path: String?) = withContext(Dispatchers.IO) {
        if (path != null) {
            val db = AppDatabase.getInstance(context)
            db.workspaceDao().deleteWorkspaceByPath(path)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: WorkspaceManager? = null

        @JvmStatic
        fun getInstance(): WorkspaceManager {
            return INSTANCE ?: synchronized(WorkspaceManager::class.java) {
                INSTANCE ?: WorkspaceManager().also { INSTANCE = it }
            }
        }
    }
}
