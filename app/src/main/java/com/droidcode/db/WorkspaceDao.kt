package com.droidcode.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {

    @Query("SELECT * FROM workspaces ORDER BY lastOpenedTimestamp DESC")
    fun getAllWorkspacesFlow(): Flow<List<WorkspaceEntity>>

    @Query("SELECT * FROM workspaces ORDER BY lastOpenedTimestamp DESC")
    suspend fun getAllWorkspaces(): List<WorkspaceEntity>

    @Query("SELECT * FROM workspaces WHERE path = :path LIMIT 1")
    suspend fun getWorkspaceByPath(path: String): WorkspaceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspace(workspace: WorkspaceEntity)

    @Query("DELETE FROM workspaces WHERE path = :path")
    suspend fun deleteWorkspaceByPath(path: String)

    @Query("DELETE FROM workspaces")
    suspend fun clearAllWorkspaces()
}
