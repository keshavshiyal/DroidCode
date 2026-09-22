package com.droidcode.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TabSessionDao {

    @Query("SELECT * FROM tab_sessions WHERE workspacePath = :workspacePath ORDER BY tabOrder ASC")
    fun getTabsForWorkspaceFlow(workspacePath: String): Flow<List<TabSessionEntity>>

    @Query("SELECT * FROM tab_sessions WHERE workspacePath = :workspacePath ORDER BY tabOrder ASC")
    suspend fun getTabsForWorkspace(workspacePath: String): List<TabSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTabSession(tabSession: TabSessionEntity)

    @Query("DELETE FROM tab_sessions WHERE workspacePath = :workspacePath")
    suspend fun clearTabsForWorkspace(workspacePath: String)
}
