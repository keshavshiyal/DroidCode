package com.example.db;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import kotlinx.coroutines.flow.Flow;

@Dao
public interface TabSessionDao {

    @NonNull
    @Query("SELECT * FROM tab_sessions WHERE workspacePath = :workspacePath ORDER BY tabOrder ASC")
    Flow<List<TabSessionEntity>> getTabsForWorkspaceFlow(String workspacePath);

    @NonNull
    @Query("SELECT * FROM tab_sessions WHERE workspacePath = :workspacePath ORDER BY tabOrder ASC")
    List<TabSessionEntity> getTabsForWorkspaceSync(String workspacePath);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTabSession(TabSessionEntity tabSession);

    @Query("DELETE FROM tab_sessions WHERE workspacePath = :workspacePath")
    void clearTabsForWorkspace(String workspacePath);
}
