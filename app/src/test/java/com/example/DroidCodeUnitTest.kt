package com.example

import com.example.core.Command
import com.example.core.CommandRegistry
import com.example.core.EventBus
import com.example.editor.EditorTab
import com.example.editor.UndoManager
import com.example.filesystem.LocalFileSystem
import com.example.project.ProjectTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DroidCodeUnitTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testCommandRegistryExecutionAndSearch() {
        val registry = CommandRegistry.getInstance()
        var executed = false

        val cmd = Command("test.save", "Save Workspace", "File", "Ctrl+S") {
            executed = true
        }

        registry.registerCommand(cmd)

        val found = registry.getCommand("test.save")
        assertNotNull(found)
        assertEquals("Save Workspace", found?.title)

        found?.execute()
        assertTrue(executed)

        val results = registry.searchCommands("save")
        assertTrue(results.isNotEmpty())
        assertEquals("Save Workspace", results[0].title)
    }

    @Test
    fun testEventBusPublishAndSubscribe() {
        val eventBus = EventBus.getInstance()
        var receivedData: String? = null

        val listener = EventBus.EventListener<String> { event ->
            receivedData = event
        }

        eventBus.subscribe(String::class.java, listener)
        eventBus.publish("Hello DroidCode")

        assertEquals("Hello DroidCode", receivedData)

        eventBus.unsubscribe(String::class.java, listener)
        eventBus.publish("Should not receive")
        assertEquals("Hello DroidCode", receivedData)
    }

    @Test
    fun testEditorTabLineColumnCalculationAndModification() {
        val content = "line 1\nline 2\nline 3"
        val tab = EditorTab("/tmp/test.txt", "test.txt", content, "text")

        assertFalse(tab.isModified)
        assertEquals(1, tab.line)
        assertEquals(1, tab.column)

        // Move cursor to "line 2"
        tab.updateCursor(8) // right after "line 1\n"
        assertEquals(2, tab.line)
        assertEquals(2, tab.column)

        // Modify content
        tab.updateContent("modified content")
        assertTrue(tab.isModified)

        tab.markSaved()
        assertFalse(tab.isModified)
    }

    @Test
    fun testUndoManagerStack() {
        val undoManager = UndoManager()
        undoManager.pushState("state 1")
        undoManager.pushState("state 2")
        undoManager.pushState("state 3")

        assertTrue(undoManager.canUndo())
        assertFalse(undoManager.canRedo())

        var restored = undoManager.undo("state 3")
        assertEquals("state 2", restored)

        restored = undoManager.undo("state 2")
        assertEquals("state 1", restored)

        assertTrue(undoManager.canRedo())
        restored = undoManager.redo("state 1")
        assertEquals("state 2", restored)
    }

    @Test
    fun testLocalFileSystemRealOperations() {
        val root = tempFolder.newFolder("workspace")
        val fs = LocalFileSystem.getInstance()

        // Create file
        val file = fs.createFile(root, "main.js")
        assertTrue(file.exists())

        // Write and read
        fs.writeStringToFile(file, "console.log('test');")
        val content = fs.readFileToString(file)
        assertEquals("console.log('test');", content)

        // List directory
        val nodes = fs.listDirectory(root)
        assertEquals(1, nodes.size)
        assertEquals("main.js", nodes[0].name)

        // Delete
        val deleted = fs.deleteFile(file)
        assertTrue(deleted)
        assertFalse(file.exists())
    }

    @Test
    fun testProjectTemplateGeneration() {
        val parent = tempFolder.newFolder("projects")
        val project = ProjectTemplate.createProjectFromTemplate(parent, "WebTest", ProjectTemplate.Type.WEB)

        assertNotNull(project)
        assertEquals("WebTest", project.name)
        assertTrue(File(project.path, "index.html").exists())
        assertTrue(File(project.path, "style.css").exists())
        assertTrue(File(project.path, "app.js").exists())
    }

    @Test
    fun testAppSettingsModel() {
        val settings = com.example.settings.AppSettings(
            com.example.settings.AppSettings.ThemeMode.DARK,
            16,
            true,
            true,
            com.example.settings.AppSettings.KeyBarDensity.NORMAL,
            true,
            com.example.settings.AppSettings.EditorFontFamily.JETBRAINS_MONO
        )
        assertEquals(com.example.settings.AppSettings.ThemeMode.DARK, settings.themeMode)
        assertEquals(16, settings.fontSizeSp)
        assertTrue(settings.isWordWrap)
        assertTrue(settings.isQuickKeyBarEnabled)
        assertEquals(com.example.settings.AppSettings.EditorFontFamily.JETBRAINS_MONO, settings.editorFontFamily)

        settings.fontSizeSp = 18
        assertEquals(18, settings.fontSizeSp)

        settings.editorFontFamily = com.example.settings.AppSettings.EditorFontFamily.FIRA_CODE
        assertEquals(com.example.settings.AppSettings.EditorFontFamily.FIRA_CODE, settings.editorFontFamily)
    }

    @Test
    fun testEditorFontFamilyEnumValues() {
        val fonts = com.example.settings.AppSettings.EditorFontFamily.values()
        assertTrue(fonts.any { it == com.example.settings.AppSettings.EditorFontFamily.JETBRAINS_MONO })
        assertTrue(fonts.any { it == com.example.settings.AppSettings.EditorFontFamily.FIRA_CODE })
        assertTrue(fonts.any { it == com.example.settings.AppSettings.EditorFontFamily.ROBOTO_MONO })
        assertTrue(fonts.any { it == com.example.settings.AppSettings.EditorFontFamily.SOURCE_CODE_PRO })

        val jb = com.example.settings.AppSettings.EditorFontFamily.JETBRAINS_MONO
        assertEquals("JetBrains Mono", jb.label)
        assertNotNull(jb.description)
    }

    @Test
    fun testKeyBarDensities() {
        assertEquals(36, com.example.settings.AppSettings.KeyBarDensity.COMPACT.heightDp)
        assertEquals(44, com.example.settings.AppSettings.KeyBarDensity.NORMAL.heightDp)
        assertEquals(52, com.example.settings.AppSettings.KeyBarDensity.COMFORTABLE.heightDp)
    }

    @Test
    fun testDeveloperInformationIntegrity() {
        val devName = "Keshu Shiyal"
        val devEmail = "keshushiyal@gmail.com"
        val devGithub = "https://github.com/keshushiyal"

        assertTrue(devGithub.contains("github.com/keshushiyal"))
        assertTrue(devEmail.contains("@gmail.com"))
        assertEquals("Keshu Shiyal", devName)
    }

    @Test
    fun testProjectFromDirectory() {
        val dir = tempFolder.newFolder("MyAwesomeProject")
        val project = com.example.project.Project.fromDirectory(dir, "WEB")
        assertNotNull(project)
        assertEquals("MyAwesomeProject", project.name)
        assertEquals(dir.absolutePath, project.path)
    }

    @Test
    fun testFileIconClassificationCompleteness() {
        // Special files
        assertEquals("DOCKER", com.example.ui.FileIconUtils.classifyFile("Dockerfile"))
        assertEquals("DOCKER", com.example.ui.FileIconUtils.classifyFile("Dockerfile.dev"))
        assertEquals("BUILD", com.example.ui.FileIconUtils.classifyFile("Makefile"))
        assertEquals("MAVEN", com.example.ui.FileIconUtils.classifyFile("pom.xml"))
        assertEquals("ENV", com.example.ui.FileIconUtils.classifyFile(".env"))
        assertEquals("ENV", com.example.ui.FileIconUtils.classifyFile(".env.local"))
        assertEquals("LICENSE", com.example.ui.FileIconUtils.classifyFile("LICENSE"))
        assertEquals("LICENSE", com.example.ui.FileIconUtils.classifyFile("LICENSE.txt"))
        assertEquals("CONFIG", com.example.ui.FileIconUtils.classifyFile(".editorconfig"))
        assertEquals("README", com.example.ui.FileIconUtils.classifyFile("README.md"))
        assertEquals("GIT", com.example.ui.FileIconUtils.classifyFile(".gitignore"))

        // Stage 1 languages
        assertEquals("HTML", com.example.ui.FileIconUtils.classifyFile("index.html"))
        assertEquals("CSS", com.example.ui.FileIconUtils.classifyFile("styles.css"))
        assertEquals("CSS", com.example.ui.FileIconUtils.classifyFile("theme.scss"))
        assertEquals("CSS", com.example.ui.FileIconUtils.classifyFile("vars.sass"))
        assertEquals("JAVASCRIPT", com.example.ui.FileIconUtils.classifyFile("bundle.js"))
        assertEquals("TYPESCRIPT", com.example.ui.FileIconUtils.classifyFile("app.ts"))
        assertEquals("TYPESCRIPT", com.example.ui.FileIconUtils.classifyFile("Component.tsx"))
        assertEquals("JSON", com.example.ui.FileIconUtils.classifyFile("package.json"))
        assertEquals("MARKDOWN", com.example.ui.FileIconUtils.classifyFile("NOTES.md"))
        assertEquals("SQL", com.example.ui.FileIconUtils.classifyFile("schema.sql"))
        assertEquals("SQL", com.example.ui.FileIconUtils.classifyFile("database.sqlite"))

        // Programming languages
        assertEquals("PYTHON", com.example.ui.FileIconUtils.classifyFile("script.py"))
        assertEquals("JAVA", com.example.ui.FileIconUtils.classifyFile("Main.java"))
        assertEquals("KOTLIN", com.example.ui.FileIconUtils.classifyFile("App.kt"))
        assertEquals("KOTLIN", com.example.ui.FileIconUtils.classifyFile("build.gradle.kts"))
        assertEquals("RUST", com.example.ui.FileIconUtils.classifyFile("main.rs"))
        assertEquals("GO", com.example.ui.FileIconUtils.classifyFile("server.go"))

        // Config & Data
        assertEquals("CONFIG", com.example.ui.FileIconUtils.classifyFile("settings.ini"))
        assertEquals("TOML", com.example.ui.FileIconUtils.classifyFile("Cargo.toml"))
        assertEquals("YAML", com.example.ui.FileIconUtils.classifyFile("ci.yml"))
        assertEquals("DATA", com.example.ui.FileIconUtils.classifyFile("data.csv"))
        assertEquals("GRADLE", com.example.ui.FileIconUtils.classifyFile("build.gradle"))

        // Media formats
        assertEquals("IMAGE", com.example.ui.FileIconUtils.classifyFile("icon.png"))
        assertEquals("IMAGE", com.example.ui.FileIconUtils.classifyFile("photo.jpg"))
        assertEquals("IMAGE", com.example.ui.FileIconUtils.classifyFile("vector.svg"))
        assertEquals("IMAGE", com.example.ui.FileIconUtils.classifyFile("modern.avif"))
        assertEquals("IMAGE", com.example.ui.FileIconUtils.classifyFile("highres.heic"))
        assertEquals("IMAGE", com.example.ui.FileIconUtils.classifyFile("photo.tiff"))
        assertEquals("PDF", com.example.ui.FileIconUtils.classifyFile("manual.pdf"))
        assertEquals("VIDEO", com.example.ui.FileIconUtils.classifyFile("demo.mp4"))
        assertEquals("ARCHIVE", com.example.ui.FileIconUtils.classifyFile("backup.zip"))
        assertEquals("SHELL", com.example.ui.FileIconUtils.classifyFile("deploy.sh"))
    }

    @Test
    fun testEditorTabViewerTypeDetection() {
        val txtFile = tempFolder.newFile("sample.txt")
        assertEquals(com.example.editor.FileViewerType.TEXT, com.example.editor.EditorTab.detectViewerType(txtFile))

        val pngFile = tempFolder.newFile("logo.png")
        assertEquals(com.example.editor.FileViewerType.IMAGE, com.example.editor.EditorTab.detectViewerType(pngFile))

        val avifFile = tempFolder.newFile("banner.avif")
        assertEquals(com.example.editor.FileViewerType.IMAGE, com.example.editor.EditorTab.detectViewerType(avifFile))

        val mp4File = tempFolder.newFile("video.mp4")
        assertEquals(com.example.editor.FileViewerType.VIDEO, com.example.editor.EditorTab.detectViewerType(mp4File))

        val pdfFile = tempFolder.newFile("document.pdf")
        assertEquals(com.example.editor.FileViewerType.PDF, com.example.editor.EditorTab.detectViewerType(pdfFile))
    }

    @Test
    fun testCommandRegistryConditionEvaluation() {
        val registry = CommandRegistry.getInstance()
        var conditionMet = false
        val cmd = Command(
            "conditional.cmd",
            "Conditional Command",
            "Test",
            null,
            { conditionMet },
            { }
        )
        registry.registerCommand(cmd)

        val retrieved = registry.getCommand("conditional.cmd")
        assertNotNull(retrieved)
        assertFalse(retrieved!!.isEnabled)

        conditionMet = true
        assertTrue(retrieved.isEnabled)
    }

    @Test
    fun testLocalFileSystemCopyAndRename() {
        val root = tempFolder.newFolder("fs_test")
        val fs = LocalFileSystem.getInstance()

        val origFile = fs.createFile(root, "source.txt")
        fs.writeStringToFile(origFile, "Original Content")

        // Rename
        val renamedFile = fs.renameFile(origFile, "renamed.txt")
        assertNotNull(renamedFile)
        assertTrue(renamedFile.exists())
        assertFalse(origFile.exists())
        assertEquals("Original Content", fs.readFileToString(renamedFile))

        // Duplicate
        val duplicate = fs.copyFileOrDirectory(renamedFile, renamedFile.parentFile)
        assertNotNull(duplicate)
        assertTrue(duplicate.exists())
        assertEquals("Original Content", fs.readFileToString(duplicate))
    }

    @Test
    fun testEditorManagerLifecycle() {
        val root = tempFolder.newFolder("editor_mgr_test")
        val testFile = File(root, "test.kt")
        testFile.writeText("fun main() {}")

        val editorMgr = com.example.editor.EditorManager.getInstance()
        val tab = editorMgr.openFile(testFile)
        assertNotNull(tab)
        assertEquals("test.kt", tab.fileName)
        assertEquals("fun main() {}", tab.content)
        assertFalse(tab.isModified)

        // Modify
        editorMgr.updateActiveTabContent("fun main() { println(1) }")
        assertTrue(tab.isModified)
        assertTrue(editorMgr.canUndoActiveTab())

        // Save
        editorMgr.saveActiveTab()
        assertFalse(tab.isModified)
        assertEquals("fun main() { println(1) }", testFile.readText())

        // Close
        val activeIdx = editorMgr.activeTabIndex
        editorMgr.closeTab(activeIdx)
        assertTrue(editorMgr.tabs.none { it.filePath == testFile.absolutePath })
    }
}
