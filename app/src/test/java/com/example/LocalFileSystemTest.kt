package com.example

import com.example.filesystem.LocalFileSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LocalFileSystemTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val fs = LocalFileSystem.getInstance()

    @Test
    fun testFileAndDirectoryCreation() {
        val root = tempFolder.newFolder("fs_test")

        val subDir = fs.createDirectory(root, "src")
        assertTrue(subDir.exists())
        assertTrue(subDir.isDirectory)

        val newFile = fs.createFile(subDir, "Index.kt")
        assertTrue(newFile.exists())
        assertTrue(newFile.isFile)
    }

    @Test
    fun testWriteAndReadOperations() {
        val root = tempFolder.newFolder("io_test")
        val file = fs.createFile(root, "sample.txt")

        val payload = "DroidCode Native IDE\nBuilt with Kotlin & Jetpack Compose"
        fs.writeStringToFile(file, payload)

        val readBack = fs.readFileToString(file)
        assertEquals(payload, readBack)
    }

    @Test
    fun testListDirectoryAndRecursiveListing() {
        val root = tempFolder.newFolder("tree_test")
        val sub = fs.createDirectory(root, "sub")
        fs.createFile(root, "fileA.txt")
        fs.createFile(sub, "fileB.txt")

        val shallow = fs.listDirectory(root)
        assertEquals(2, shallow.size)

        val recursive = fs.listDirectoryRecursive(root)
        assertEquals(2, recursive.size) // sub and fileA at root level
        val subNode = recursive.find { it.name == "sub" }
        assertNotNull(subNode)
        assertTrue(subNode!!.isFolder)
        assertEquals(1, subNode.children.size)
        assertEquals("fileB.txt", subNode.children[0].name)
    }

    @Test
    fun testDeleteOperations() {
        val root = tempFolder.newFolder("del_test")
        val file = fs.createFile(root, "to_delete.txt")
        assertTrue(file.exists())

        val deleted = fs.deleteFile(file)
        assertTrue(deleted)
        assertFalse(file.exists())

        val dir = fs.createDirectory(root, "dir_to_delete")
        fs.createFile(dir, "nested.txt")
        val dirDeleted = fs.deleteFile(dir)
        assertTrue(dirDeleted)
        assertFalse(dir.exists())
    }
}
