package com.droidcode

import com.droidcode.core.capability.Capability
import com.droidcode.core.capability.CapabilityGroup
import com.droidcode.core.capability.CapabilityRegistry
import com.droidcode.core.capability.CapabilityStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityRegistryTest {

    @Test
    fun testAllCapabilityIdsAreUnique() {
        val all = CapabilityRegistry.getAll()
        val ids = all.map { it.id }
        val uniqueIds = ids.toSet()
        assertEquals("Capability IDs must be strictly unique", ids.size, uniqueIds.size)
    }

    @Test
    fun testAllCapabilityNamesAndDescriptionsAreNonEmpty() {
        for (cap in CapabilityRegistry.getAll()) {
            assertTrue("Capability ${cap.id} has empty name", cap.name.isNotBlank())
            assertTrue("Capability ${cap.id} has empty description", cap.description.isNotBlank())
            assertTrue("Capability ${cap.id} has blank id", cap.id.isNotBlank())
            assertNotNull("Capability ${cap.id} group cannot be null", cap.group)
            assertNotNull("Capability ${cap.id} status cannot be null", cap.status)
        }
    }

    @Test
    fun testDependenciesReferenceValidCapabilities() {
        val allIds = CapabilityRegistry.getAll().map { it.id }.toSet()
        for (cap in CapabilityRegistry.getAll()) {
            for (dep in cap.dependencies) {
                assertTrue(
                    "Capability ${cap.id} references non-existent dependency '$dep'",
                    allIds.contains(dep)
                )
            }
        }
    }

    @Test
    fun testRequiredBaselineCapabilitiesExist() {
        val requiredIds = listOf(
            "editor.basic-editing",
            "editor.open-file",
            "editor.save",
            "editor.undo-redo",
            "editor.tabs",
            "workspace.open",
            "workspace.create",
            "workspace.recent",
            "filesystem.list",
            "filesystem.create-file",
            "git.status",
            "terminal.ui",
            "ui.dark-theme",
            "ui.light-theme"
        )
        for (req in requiredIds) {
            val cap = CapabilityRegistry.get(req)
            assertNotNull("Required baseline capability '$req' must exist in registry", cap)
            assertEquals("Baseline capability '$req' should be AVAILABLE", CapabilityStatus.AVAILABLE, cap!!.status)
        }
    }

    @Test
    fun testPlannedCapabilitiesAreNotMarkedAvailable() {
        val plannedIds = listOf(
            "git.diff",
            "git.commit",
            "git.remote",
            "terminal.real-pty",
            "database.mysql",
            "database.postgresql"
        )
        for (id in plannedIds) {
            val cap = CapabilityRegistry.get(id)
            assertNotNull("Capability '$id' should be in registry", cap)
            assertEquals("Capability '$id' must be PLANNED", CapabilityStatus.PLANNED, cap!!.status)
            assertFalse("Capability '$id' must not report available", CapabilityRegistry.isAvailable(id))
        }
    }

    @Test
    fun testFilterByGroup() {
        val editorCaps = CapabilityRegistry.getByGroup(CapabilityGroup.EDITOR)
        assertTrue("Editor group should have multiple capabilities", editorCaps.size >= 8)
        assertTrue(editorCaps.all { it.group == CapabilityGroup.EDITOR })

        val gitCaps = CapabilityRegistry.getByGroup(CapabilityGroup.GIT)
        assertTrue("Git group should have capabilities", gitCaps.isNotEmpty())
        assertTrue(gitCaps.all { it.group == CapabilityGroup.GIT })
    }

    @Test
    fun testFilterByStatus() {
        val available = CapabilityRegistry.getByStatus(CapabilityStatus.AVAILABLE)
        assertTrue("Available capabilities count should be substantial", available.size >= 15)
        assertTrue(available.all { it.status == CapabilityStatus.AVAILABLE })

        val planned = CapabilityRegistry.getByStatus(CapabilityStatus.PLANNED)
        assertTrue("Planned capabilities count should be present", planned.size >= 5)
        assertTrue(planned.all { it.status == CapabilityStatus.PLANNED })
    }

    @Test(expected = IllegalArgumentException::class)
    fun testDuplicateIdDetectionSafety() {
        // Constructing duplicate list should trigger check
        val duplicates = listOf(
            Capability("test.dup", "Name 1", CapabilityGroup.UI, CapabilityStatus.PLANNED, "Desc 1"),
            Capability("test.dup", "Name 2", CapabilityGroup.UI, CapabilityStatus.PLANNED, "Desc 2")
        )
        require(duplicates.size == duplicates.associateBy { it.id }.size) {
            "Duplicate IDs detected"
        }
    }
}
