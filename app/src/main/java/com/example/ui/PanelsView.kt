package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.DatabaseManager
import com.example.git.GitService
import com.example.project.WorkspaceManager
import com.example.terminal.TerminalService

enum class PanelTab(val title: String, val icon: ImageVector) {
    GIT("Git", Icons.Default.AccountTree),
    TERMINAL("Terminal", Icons.Default.Terminal),
    DATABASE("Database", Icons.Default.Storage),
    PROBLEMS("Problems", Icons.Default.BugReport)
}

@Composable
fun BottomPanel(
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(PanelTab.GIT) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        // Tab Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PanelTab.values().forEach { tab ->
                val isSelected = activeTab == tab
                val bg = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)

                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .background(bg, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .border(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                        )
                        .clickable { activeTab = tab }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("panel_tab_${tab.name.lowercase()}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tab.title,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
        }

        // Panel Body Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            when (activeTab) {
                PanelTab.GIT -> GitPanelView()
                PanelTab.TERMINAL -> TerminalPanelView()
                PanelTab.DATABASE -> DatabasePanelView()
                PanelTab.PROBLEMS -> ProblemsPanelView()
            }
        }
    }
}

@Composable
private fun GitPanelView() {
    val workspaceMgr = remember { WorkspaceManager.getInstance() }
    val gitService = remember { GitService.getInstance() }
    val status = remember(workspaceMgr.currentProject) {
        gitService.inspectWorkspace(workspaceMgr.currentProject)
    }

    Column {
        if (!status.isGitRepo) {
            Text(
                text = "Not a Git repository",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "The active workspace is not tracked by Git.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Branch: ",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = status.currentBranch,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Changes: 0 staged, 0 modified, 0 untracked",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun TerminalPanelView() {
    val termService = remember { TerminalService.getInstance() }

    Column {
        Text(
            text = termService.statusMessage,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DatabasePanelView() {
    val dbMgr = remember { DatabaseManager.getInstance() }

    Column {
        Text(
            text = dbMgr.statusMessage,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProblemsPanelView() {
    Column {
        Text(
            text = "No problems detected in workspace.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
