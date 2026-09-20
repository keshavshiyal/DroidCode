package com.example.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import kotlinx.coroutines.flow.Flow;

@Dao
public interface WorkspaceDao {

    @Query("SELECT * FROM workspaces ORDER BY lastOpenedTimestamp DESC")
    Flow<List<WorkspaceEntity>> getAllWorkspacesFlow();

    @Query("SELECT * FROM workspaces ORDER BY lastOpenedTimestamp DESC")
    List<WorkspaceEntity> getAllWorkspacesSync();

    @Query("SELECT * FROM workspaces WHERE path = :path LIMIT 1")
    WorkspaceEntity getWorkspaceByPath(String path);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertWorkspace(WorkspaceEntity workspace);

    @Query("DELETE FROM workspaces WHERE path = :path")
    void deleteWorkspaceByPath(String path);

    @Query("DELETE FROM workspaces")
    void clearAllWorkspaces();
}
