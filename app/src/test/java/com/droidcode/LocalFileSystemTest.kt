package com.droidcode

import com.droidcode.filesystem.LocalFileSystem
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

    @Test
    fun testCreateFileDoesNotAddTxtExtension() {
        val root = tempFolder.newFolder("create_ext_test")

        val batFile = fs.createFile(root, "temp.bat")
        assertEquals("temp.bat", batFile.name)

        val phpFile = fs.createFile(root, "about.php")
        assertEquals("about.php", phpFile.name)

        val makefile = fs.createFile(root, "Makefile")
        assertEquals("Makefile", makefile.name)

        val dockerfile = fs.createFile(root, "Dockerfile")
        assertEquals("Dockerfile", dockerfile.name)

        val gitignore = fs.createFile(root, ".gitignore")
        assertEquals(".gitignore", gitignore.name)

        val htmlFile = fs.createFile(root, "test.html")
        assertEquals("test.html", htmlFile.name)

        val txtFile = fs.createFile(root, "notes.txt")
        assertEquals("notes.txt", txtFile.name)
    }

    @Test
    fun testDuplicateFilePreservesExactExtension() {
        val root = tempFolder.newFolder("dup_ext_test")

        val batFile = fs.createFile(root, "temp.bat")
        val dupBat = fs.duplicateFile(batFile)
        assertEquals("temp_copy.bat", dupBat.name)
        assertTrue(dupBat.exists())

        val phpFile = fs.createFile(root, "about.php")
        val dupPhp = fs.duplicateFile(phpFile)
        assertEquals("about_copy.php", dupPhp.name)
        assertTrue(dupPhp.exists())

        val makefile = fs.createFile(root, "Makefile")
        val dupMake = fs.duplicateFile(makefile)
        assertEquals("Makefile_copy", dupMake.name)
        assertTrue(dupMake.exists())

        val txtFile = fs.createFile(root, "notes.txt")
        val dupTxt = fs.duplicateFile(txtFile)
        assertEquals("notes_copy.txt", dupTxt.name)
        assertTrue(dupTxt.exists())
    }

    @Test
    fun testSafUtilsGetCleanFileName() {
        assertEquals("temp.bat", com.droidcode.filesystem.SafUtils.getCleanFileName("temp.bat.txt"))
        assertEquals("push_copy.bat", com.droidcode.filesystem.SafUtils.getCleanFileName("push_copy.bat.txt"))
        assertEquals("about.php", com.droidcode.filesystem.SafUtils.getCleanFileName("about.php.txt"))
        assertEquals("script.sh", com.droidcode.filesystem.SafUtils.getCleanFileName("script.sh.txt"))
        assertEquals("notes.txt", com.droidcode.filesystem.SafUtils.getCleanFileName("notes.txt"))
        assertEquals("README.txt", com.droidcode.filesystem.SafUtils.getCleanFileName("README.txt"))
        assertEquals("requirements.txt", com.droidcode.filesystem.SafUtils.getCleanFileName("requirements.txt"))
        assertEquals("temp.bat", com.droidcode.filesystem.SafUtils.getCleanFileName("temp.bat"))
        assertEquals("Makefile", com.droidcode.filesystem.SafUtils.getCleanFileName("Makefile"))
    }

    @Test
    fun testListDirectoryRecursiveRepairsAccidentalTxt() {
        val root = tempFolder.newFolder("repair_test")
        val brokenFile = File(root, "temp.bat.txt")
        brokenFile.createNewFile()
        assertTrue(brokenFile.exists())

        val nodes = fs.listDirectoryRecursive(root)
        assertEquals(1, nodes.size)
        assertEquals("temp.bat", nodes[0].name)
        assertFalse(brokenFile.exists())
        assertTrue(File(root, "temp.bat").exists())
    }
}
