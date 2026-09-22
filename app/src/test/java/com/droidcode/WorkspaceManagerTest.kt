package com.droidcode

import androidx.test.core.app.ApplicationProvider
import com.droidcode.project.Project
import com.droidcode.project.WorkspaceManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WorkspaceManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var workspaceMgr: WorkspaceManager

    @Before
    fun setUp() {
        workspaceMgr = WorkspaceManager.getInstance()
        workspaceMgr.closeWorkspace()
    }

    @Test
    fun testOpenAndCloseWorkspace() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dir = tempFolder.newFolder("TestWorkspace")

        assertFalse(workspaceMgr.hasOpenWorkspace())
        assertNull(workspaceMgr.currentProject)

        workspaceMgr.openWorkspace(context, dir, "KOTLIN")

        assertTrue(workspaceMgr.hasOpenWorkspace())
        val project = workspaceMgr.currentProject
        assertNotNull(project)
        assertEquals("TestWorkspace", project?.name)
        assertEquals(dir.absolutePath, project?.path)
        assertEquals("KOTLIN", project?.type)

        workspaceMgr.closeWorkspace()
        assertFalse(workspaceMgr.hasOpenWorkspace())
        assertNull(workspaceMgr.currentProject)
    }

    @Test
    fun testRecentWorkspacesPersistenceAndRemoval() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dir1 = tempFolder.newFolder("Project1")
        val dir2 = tempFolder.newFolder("Project2")

        workspaceMgr.openWorkspace(context, dir1, "WEB")
        workspaceMgr.openWorkspace(context, dir2, "JAVA")

        val recents = workspaceMgr.getRecentWorkspaces(context)
        assertTrue(recents.any { it.path == dir1.absolutePath })
        assertTrue(recents.any { it.path == dir2.absolutePath })

        // Explicitly remove dir1
        workspaceMgr.removeWorkspace(context, dir1.absolutePath)
        val updatedRecents = workspaceMgr.getRecentWorkspaces(context)
        assertFalse(updatedRecents.any { it.path == dir1.absolutePath })
        assertTrue(updatedRecents.any { it.path == dir2.absolutePath })
    }

    @Test(expected = IllegalArgumentException::class)
    fun testOpenNonExistentDirectoryThrowsException() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val nonExistent = File(tempFolder.root, "non_existent_folder_xyz")
        workspaceMgr.openWorkspace(context, nonExistent, "GENERAL")
    }
}
