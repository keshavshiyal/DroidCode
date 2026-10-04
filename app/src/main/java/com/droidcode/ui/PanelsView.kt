package com.droidcode.ui

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
import com.droidcode.database.DatabaseManager
import com.droidcode.git.GitService
import com.droidcode.project.WorkspaceManager
import com.droidcode.terminal.TerminalService

enum class PanelTab(val title: String, val icon: ImageVector) {
    GIT("Git", Icons.Default.AccountTree),
    TERMINAL("Terminal", Icons.Default.Terminal),
    DATABASE("Database", Icons.Default.Storage),
    PROBLEMS("Problems", Icons.Default.BugReport)
}

enum class PanelDockPosition {
    BOTTOM,
    RIGHT
}

enum class PanelSizePreset(val label: String, val heightDp: Int, val widthDp: Int) {
    COMPACT("Compact", 140, 260),
    NORMAL("Normal", 200, 340),
    EXPANDED("Expanded", 300, 460);

    fun next(): PanelSizePreset = when (this) {
        COMPACT -> NORMAL
        NORMAL -> EXPANDED
        EXPANDED -> COMPACT
    }
}

@Composable
fun BottomOrSidePanel(
    activeTab: PanelTab = PanelTab.GIT,
    onTabSelected: (PanelTab) -> Unit = {},
    dockPosition: PanelDockPosition = PanelDockPosition.BOTTOM,
    sizePreset: PanelSizePreset = PanelSizePreset.NORMAL,
    onSizePresetChange: (PanelSizePreset) -> Unit = {},
    onToggleDockPosition: () -> Unit = {},
    onUndock: () -> Unit = {},
    onClose: () -> Unit = {},
    supportsDockRight: Boolean = true,
    modifier: Modifier = Modifier
) {
    val panelModifier = if (dockPosition == PanelDockPosition.BOTTOM) {
        modifier
            .fillMaxWidth()
            .height(sizePreset.heightDp.dp)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
    } else {
        modifier
            .width(sizePreset.widthDp.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }

    Column(modifier = panelModifier) {
        // Tab Header with action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tabs
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxHeight(),
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
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                            )
                            .clickable { onTabSelected(tab) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("panel_tab_${tab.name.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tab.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(3.dp))
                }
            }

            // Header Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (supportsDockRight) {
                    Surface(
                        onClick = onToggleDockPosition,
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = androidx.compose.material.icons.filled.SwapHoriz,
                                contentDescription = if (dockPosition == PanelDockPosition.BOTTOM) "Dock to Right" else "Dock to Bottom",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Surface(
                    onClick = { onSizePresetChange(sizePreset.next()) },
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .height(24.dp)
                        .padding(horizontal = 4.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxHeight()) {
                        Text(
                            text = sizePreset.label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    onClick = onUndock,
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = androidx.compose.material.icons.automirrored.filled.OpenInNew,
                            contentDescription = "Undock Panel",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Surface(
                    onClick = onClose,
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = androidx.compose.material.icons.filled.Close,
                            contentDescription = "Close Panel",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // Panel Body Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
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
fun FloatingPanelDialog(
    activeTab: PanelTab,
    onTabSelected: (PanelTab) -> Unit,
    onDock: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.75f)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header with Tabs and Dock / Close buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PanelTab.values().forEach { tab ->
                            val isSelected = activeTab == tab
                            val bg = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)

                            Row(
                                modifier = Modifier
                                    .background(bg, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .border(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                    )
                                    .clickable { onTabSelected(tab) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
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

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            onClick = onDock,
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = "Dock to Shell",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Surface(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.filled.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
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
    }
}

@Composable
fun BottomPanel(
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(PanelTab.GIT) }
    var sizePreset by remember { mutableStateOf(PanelSizePreset.NORMAL) }

    BottomOrSidePanel(
        activeTab = activeTab,
        onTabSelected = { activeTab = it },
        dockPosition = PanelDockPosition.BOTTOM,
        sizePreset = sizePreset,
        onSizePresetChange = { sizePreset = it },
        onToggleDockPosition = {},
        onUndock = {},
        onClose = {},
        supportsDockRight = false,
        modifier = modifier
    )
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
