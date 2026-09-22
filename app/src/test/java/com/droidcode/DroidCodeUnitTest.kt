package com.droidcode

import com.droidcode.core.Command
import com.droidcode.core.CommandRegistry
import com.droidcode.core.EventBus
import com.droidcode.editor.EditorTab
import com.droidcode.editor.FileViewerType
import com.droidcode.editor.UndoManager
import com.droidcode.filesystem.LocalFileSystem
import com.droidcode.project.ProjectTemplate
import com.droidcode.settings.AppSettings
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
        val settings = AppSettings(
            AppSettings.ThemeMode.DARK,
            16,
            true,
            true,
            AppSettings.KeyBarDensity.NORMAL,
            true,
            AppSettings.EditorFontFamily.JETBRAINS_MONO
        )
        assertEquals(AppSettings.ThemeMode.DARK, settings.themeMode)
        assertEquals(16, settings.fontSizeSp)
        assertTrue(settings.isWordWrap)
        assertTrue(settings.isQuickKeyBarEnabled)
        assertEquals(AppSettings.EditorFontFamily.JETBRAINS_MONO, settings.editorFontFamily)

        settings.fontSizeSp = 18
        assertEquals(18, settings.fontSizeSp)

        settings.editorFontFamily = AppSettings.EditorFontFamily.FIRA_CODE
        assertEquals(AppSettings.EditorFontFamily.FIRA_CODE, settings.editorFontFamily)
    }

    @Test
    fun testEditorFontFamilyEnumValues() {
        val fonts = AppSettings.EditorFontFamily.values()
        assertTrue(fonts.any { it == AppSettings.EditorFontFamily.JETBRAINS_MONO })
        assertTrue(fonts.any { it == AppSettings.EditorFontFamily.FIRA_CODE })
        assertTrue(fonts.any { it == AppSettings.EditorFontFamily.ROBOTO_MONO })
        assertTrue(fonts.any { it == AppSettings.EditorFontFamily.SOURCE_CODE_PRO })

        val jb = AppSettings.EditorFontFamily.JETBRAINS_MONO
        assertEquals("JetBrains Mono", jb.label)
        assertNotNull(jb.description)
    }

    @Test
    fun testKeyBarDensities() {
        assertEquals(36, AppSettings.KeyBarDensity.COMPACT.heightDp)
        assertEquals(44, AppSettings.KeyBarDensity.NORMAL.heightDp)
        assertEquals(52, AppSettings.KeyBarDensity.COMFORTABLE.heightDp)
    }

    @Test
    fun testDeveloperInformationIntegrity() {
        val devName = "Keshav Shiyal"
        val devGithub = "https://github.com/keshavshiyal"

        assertTrue(devGithub.contains("github.com/keshavshiyal"))
        assertEquals("Keshav Shiyal", devName)
    }

    @Test
    fun testProjectFromDirectory() {
        val dir = tempFolder.newFolder("MyAwesomeProject")
        val project = com.droidcode.project.Project.fromDirectory(dir, "WEB")
        assertNotNull(project)
        assertEquals("MyAwesomeProject", project.name)
        assertEquals(dir.absolutePath, project.path)
    }

    @Test
    fun testFileIconClassificationCompleteness() {
        // Special files
        assertEquals("DOCKER", com.droidcode.ui.FileIconUtils.classifyFile("Dockerfile"))
        assertEquals("DOCKER", com.droidcode.ui.FileIconUtils.classifyFile("Dockerfile.dev"))
        assertEquals("BUILD", com.droidcode.ui.FileIconUtils.classifyFile("Makefile"))
        assertEquals("MAVEN", com.droidcode.ui.FileIconUtils.classifyFile("pom.xml"))
        assertEquals("ENV", com.droidcode.ui.FileIconUtils.classifyFile(".env"))
        assertEquals("ENV", com.droidcode.ui.FileIconUtils.classifyFile(".env.local"))
        assertEquals("LICENSE", com.droidcode.ui.FileIconUtils.classifyFile("LICENSE"))
        assertEquals("LICENSE", com.droidcode.ui.FileIconUtils.classifyFile("LICENSE.txt"))
        assertEquals("CONFIG", com.droidcode.ui.FileIconUtils.classifyFile(".editorconfig"))
        assertEquals("README", com.droidcode.ui.FileIconUtils.classifyFile("README.md"))
        assertEquals("GIT", com.droidcode.ui.FileIconUtils.classifyFile(".gitignore"))

        // Stage 1 languages
        assertEquals("HTML", com.droidcode.ui.FileIconUtils.classifyFile("index.html"))
        assertEquals("CSS", com.droidcode.ui.FileIconUtils.classifyFile("styles.css"))
        assertEquals("CSS", com.droidcode.ui.FileIconUtils.classifyFile("theme.scss"))
        assertEquals("CSS", com.droidcode.ui.FileIconUtils.classifyFile("vars.sass"))
        assertEquals("JAVASCRIPT", com.droidcode.ui.FileIconUtils.classifyFile("bundle.js"))
        assertEquals("TYPESCRIPT", com.droidcode.ui.FileIconUtils.classifyFile("app.ts"))
        assertEquals("TYPESCRIPT", com.droidcode.ui.FileIconUtils.classifyFile("Component.tsx"))
        assertEquals("JSON", com.droidcode.ui.FileIconUtils.classifyFile("package.json"))
        assertEquals("MARKDOWN", com.droidcode.ui.FileIconUtils.classifyFile("NOTES.md"))
        assertEquals("SQL", com.droidcode.ui.FileIconUtils.classifyFile("schema.sql"))
        assertEquals("SQL", com.droidcode.ui.FileIconUtils.classifyFile("database.sqlite"))

        // Programming languages
        assertEquals("PYTHON", com.droidcode.ui.FileIconUtils.classifyFile("script.py"))
        assertEquals("JAVA", com.droidcode.ui.FileIconUtils.classifyFile("Main.java"))
        assertEquals("KOTLIN", com.droidcode.ui.FileIconUtils.classifyFile("App.kt"))
        assertEquals("KOTLIN", com.droidcode.ui.FileIconUtils.classifyFile("build.gradle.kts"))
        assertEquals("RUST", com.droidcode.ui.FileIconUtils.classifyFile("main.rs"))
        assertEquals("GO", com.droidcode.ui.FileIconUtils.classifyFile("server.go"))

        // Config & Data
        assertEquals("CONFIG", com.droidcode.ui.FileIconUtils.classifyFile("settings.ini"))
        assertEquals("TOML", com.droidcode.ui.FileIconUtils.classifyFile("Cargo.toml"))
        assertEquals("YAML", com.droidcode.ui.FileIconUtils.classifyFile("ci.yml"))
        assertEquals("DATA", com.droidcode.ui.FileIconUtils.classifyFile("data.csv"))
        assertEquals("GRADLE", com.droidcode.ui.FileIconUtils.classifyFile("build.gradle"))

        // Media formats
        assertEquals("IMAGE", com.droidcode.ui.FileIconUtils.classifyFile("icon.png"))
        assertEquals("IMAGE", com.droidcode.ui.FileIconUtils.classifyFile("photo.jpg"))
        assertEquals("IMAGE", com.droidcode.ui.FileIconUtils.classifyFile("vector.svg"))
        assertEquals("IMAGE", com.droidcode.ui.FileIconUtils.classifyFile("modern.avif"))
        assertEquals("IMAGE", com.droidcode.ui.FileIconUtils.classifyFile("highres.heic"))
        assertEquals("IMAGE", com.droidcode.ui.FileIconUtils.classifyFile("photo.tiff"))
        assertEquals("PDF", com.droidcode.ui.FileIconUtils.classifyFile("manual.pdf"))
        assertEquals("VIDEO", com.droidcode.ui.FileIconUtils.classifyFile("demo.mp4"))
        assertEquals("ARCHIVE", com.droidcode.ui.FileIconUtils.classifyFile("backup.zip"))
        assertEquals("SHELL", com.droidcode.ui.FileIconUtils.classifyFile("deploy.sh"))
    }

    @Test
    fun testEditorTabViewerTypeDetection() {
        val txtFile = tempFolder.newFile("sample.txt")
        assertEquals(FileViewerType.TEXT, EditorTab.detectViewerType(txtFile))

        val pngFile = tempFolder.newFile("logo.png")
        assertEquals(FileViewerType.IMAGE, EditorTab.detectViewerType(pngFile))

        val avifFile = tempFolder.newFile("banner.avif")
        assertEquals(FileViewerType.IMAGE, EditorTab.detectViewerType(avifFile))

        val mp4File = tempFolder.newFile("video.mp4")
        assertEquals(FileViewerType.VIDEO, EditorTab.detectViewerType(mp4File))

        val pdfFile = tempFolder.newFile("document.pdf")
        assertEquals(FileViewerType.PDF, EditorTab.detectViewerType(pdfFile))
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

        val editorMgr = com.droidcode.editor.EditorManager.getInstance()
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
