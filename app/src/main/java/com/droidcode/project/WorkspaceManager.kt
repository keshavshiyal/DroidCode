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
import kotlinx.coroutines.flow.map
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
        if (directory.name in IGNORED_INTERNAL_DIRS) {
            throw IllegalArgumentException("Cannot open internal system directory '${directory.name}' as workspace")
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
        get() = getWorkspaceFileTree(emptySet())

    fun getWorkspaceFileTree(expandedPaths: Set<String> = emptySet()): List<FileNode> {
        val proj = _currentProject ?: return emptyList()
        if (!File(proj.path).exists()) return emptyList()
        return fileSystem.listDirectory(proj.directory, expandedPaths)
    }

    fun getRecentWorkspacesFlow(context: Context): Flow<List<WorkspaceEntity>> {
        val db = AppDatabase.getInstance(context)
        val filesDir = context.filesDir
        val debugDirPath = if (filesDir != null) File(filesDir, "debug_logs").absolutePath else null
        return db.workspaceDao().getAllWorkspacesFlow().map { list ->
            list.filter { entity ->
                entity.name != "debug_logs" &&
                (debugDirPath == null || entity.path != debugDirPath) &&
                !entity.path.endsWith("/debug_logs") &&
                !entity.path.endsWith("\\debug_logs") &&
                !IGNORED_INTERNAL_DIRS.contains(File(entity.path).name)
            }
        }
    }

    suspend fun getRecentWorkspaces(context: Context): List<WorkspaceEntity> = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val filesDir = context.filesDir
        val debugDirPath = if (filesDir != null) File(filesDir, "debug_logs").absolutePath else null
        db.workspaceDao().getAllWorkspaces().filter { entity ->
            entity.name != "debug_logs" &&
            (debugDirPath == null || entity.path != debugDirPath) &&
            !entity.path.endsWith("/debug_logs") &&
            !entity.path.endsWith("\\debug_logs") &&
            !IGNORED_INTERNAL_DIRS.contains(File(entity.path).name)
        }
    }

    suspend fun removeWorkspace(context: Context, path: String?) = withContext(Dispatchers.IO) {
        if (path != null) {
            val db = AppDatabase.getInstance(context)
            db.workspaceDao().deleteWorkspaceByPath(path)
        }
    }

    suspend fun syncInternalProjects(context: Context) = withContext(Dispatchers.IO) {
        try {
            val filesDir = context.filesDir ?: return@withContext
            val db = AppDatabase.getInstance(context)

            // Purge any stale internal logs directories that were historically tracked
            val debugDirPath = File(filesDir, "debug_logs").absolutePath
            db.workspaceDao().deleteWorkspaceByPath(debugDirPath)

            val existingPaths = db.workspaceDao().getAllWorkspaces().map { it.path }.toSet()

            val candidates = mutableListOf<File>()
            filesDir.listFiles()?.forEach { file ->
                if (file.isDirectory && !file.name.startsWith(".") && file.name !in IGNORED_INTERNAL_DIRS) {
                    if (file.name == "saf_projects") {
                        file.listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") && it.name !in IGNORED_INTERNAL_DIRS }?.let { candidates.addAll(it) }
                    } else {
                        candidates.add(file)
                    }
                }
            }

            for (dir in candidates) {
                if (dir.absolutePath !in existingPaths) {
                    val project = Project.fromDirectory(dir, null)
                    if (project != null) {
                        db.workspaceDao().insertWorkspace(
                            WorkspaceEntity(
                                project.path,
                                project.name,
                                project.type,
                                dir.lastModified(),
                                project.isGitRepo
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("WorkspaceManager", "Failed to sync internal projects", e)
        }
    }

    suspend fun deleteWorkspacePermanently(context: Context, path: String?): Boolean = withContext(Dispatchers.IO) {
        if (path == null) return@withContext false
        try {
            val file = File(path)
            var deleted = false
            if (file.exists()) {
                deleted = file.deleteRecursively()
            }
            val db = AppDatabase.getInstance(context)
            db.workspaceDao().deleteWorkspaceByPath(path)
            if (_currentProject?.path == path) {
                closeWorkspace()
            }
            deleted
        } catch (e: Exception) {
            android.util.Log.e("WorkspaceManager", "Failed to permanently delete workspace $path", e)
            false
        }
    }

    companion object {
        val IGNORED_INTERNAL_DIRS = setOf(
            "code_cache",
            "debug_logs",
            "databases",
            "shared_prefs",
            "app_webview",
            "cache",
            "no_backup"
        )

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
