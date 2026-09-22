package com.droidcode.core.capability

/**
 * Central registry of DroidCode platform capabilities.
 *
 * This registry is strictly descriptive: it documents genuine support levels
 * across subsystems without pretending that planned features are active, and
 * without driving core IDE business logic directly.
 */
object CapabilityRegistry {

    private val catalog: List<Capability> = listOf(
        // Editor Subsystem
        Capability(
            id = "editor.basic-editing",
            name = "Basic Text Editing",
            group = CapabilityGroup.EDITOR,
            status = CapabilityStatus.AVAILABLE,
            description = "In-memory text editing buffer with reactive state updates.",
            documentationRef = "docs/architecture/overview.md"
        ),
        Capability(
            id = "editor.open-file",
            name = "Open File into Editor",
            group = CapabilityGroup.EDITOR,
            status = CapabilityStatus.AVAILABLE,
            description = "Load local text files into active editor buffers.",
            dependencies = listOf("filesystem.list")
        ),
        Capability(
            id = "editor.save",
            name = "Save File",
            group = CapabilityGroup.EDITOR,
            status = CapabilityStatus.AVAILABLE,
            description = "Flush modified buffer contents atomically back to disk.",
            dependencies = listOf("editor.basic-editing")
        ),
        Capability(
            id = "editor.undo-redo",
            name = "Undo & Redo Stack",
            group = CapabilityGroup.EDITOR,
            status = CapabilityStatus.AVAILABLE,
            description = "Linear undo/redo history stack tracking text edit deltas.",
            dependencies = listOf("editor.basic-editing")
        ),
        Capability(
            id = "editor.tabs",
            name = "Multi-Tab Management",
            group = CapabilityGroup.EDITOR,
            status = CapabilityStatus.AVAILABLE,
            description = "Open, switch, reorder, and close multiple concurrent editor tabs.",
            dependencies = listOf("editor.basic-editing")
        ),
        Capability(
            id = "editor.cursor-status",
            name = "Cursor & Position Indicator",
            group = CapabilityGroup.EDITOR,
            status = CapabilityStatus.AVAILABLE,
            description = "Display current line, column, selection length, and character offsets."
        ),
        Capability(
            id = "editor.unsaved-state",
            name = "Unsaved Changes Tracking",
            group = CapabilityGroup.EDITOR,
            status = CapabilityStatus.AVAILABLE,
            description = "Visual indicators (dot badge) and close-confirmation dialogs for dirty tabs."
        ),
        Capability(
            id = "editor.context-menu",
            name = "Editor Context Menu",
            group = CapabilityGroup.EDITOR,
            status = CapabilityStatus.AVAILABLE,
            description = "Contextual action menu providing clipboard, formatting, and navigation operations."
        ),
        Capability(
            id = "editor.quick-key-bar",
            name = "Developer Quick Key Bar",
            group = CapabilityGroup.EDITOR,
            status = CapabilityStatus.AVAILABLE,
            description = "Accessory keyboard toolbar with brackets, symbols, cursor navigation, and tab insertion."
        ),

        // Workspace Subsystem
        Capability(
            id = "workspace.open",
            name = "Open Workspace",
            group = CapabilityGroup.WORKSPACE,
            status = CapabilityStatus.AVAILABLE,
            description = "Load an existing directory as an active project workspace."
        ),
        Capability(
            id = "workspace.create",
            name = "Create Project Workspace",
            group = CapabilityGroup.WORKSPACE,
            status = CapabilityStatus.AVAILABLE,
            description = "Scaffold new projects from built-in templates (Android, Web, Python, Empty)."
        ),
        Capability(
            id = "workspace.recent",
            name = "Recent Workspaces List",
            group = CapabilityGroup.WORKSPACE,
            status = CapabilityStatus.AVAILABLE,
            description = "Query and display previously opened projects sorted by access time."
        ),
        Capability(
            id = "workspace.persistence",
            name = "Workspace Persistence",
            group = CapabilityGroup.WORKSPACE,
            status = CapabilityStatus.AVAILABLE,
            description = "Persist workspace history and tab state locally using Room SQLite.",
            dependencies = listOf("workspace.recent")
        ),
        Capability(
            id = "workspace.stale-workspace-detection",
            name = "Stale Workspace Detection",
            group = CapabilityGroup.WORKSPACE,
            status = CapabilityStatus.AVAILABLE,
            description = "Detect when physical workspace directory has been removed and clean up database."
        ),

        // Filesystem Subsystem
        Capability(
            id = "filesystem.list",
            name = "Directory Listing",
            group = CapabilityGroup.FILESYSTEM,
            status = CapabilityStatus.AVAILABLE,
            description = "Enumerate file trees with hierarchical folder expansion."
        ),
        Capability(
            id = "filesystem.create-file",
            name = "Create File",
            group = CapabilityGroup.FILESYSTEM,
            status = CapabilityStatus.AVAILABLE,
            description = "Create new files inside the current workspace."
        ),
        Capability(
            id = "filesystem.create-folder",
            name = "Create Folder",
            group = CapabilityGroup.FILESYSTEM,
            status = CapabilityStatus.AVAILABLE,
            description = "Create new directories inside the current workspace."
        ),
        Capability(
            id = "filesystem.rename",
            name = "Rename File or Folder",
            group = CapabilityGroup.FILESYSTEM,
            status = CapabilityStatus.AVAILABLE,
            description = "Safely rename filesystem nodes within project boundaries."
        ),
        Capability(
            id = "filesystem.delete",
            name = "Delete File or Folder",
            group = CapabilityGroup.FILESYSTEM,
            status = CapabilityStatus.AVAILABLE,
            description = "Delete files and directories recursively with user confirmation."
        ),
        Capability(
            id = "filesystem.refresh",
            name = "Refresh File Tree",
            group = CapabilityGroup.FILESYSTEM,
            status = CapabilityStatus.AVAILABLE,
            description = "Re-scan disk hierarchy to synchronize external file changes."
        ),
        Capability(
            id = "filesystem.saf",
            name = "Storage Access Framework (SAF)",
            group = CapabilityGroup.FILESYSTEM,
            status = CapabilityStatus.AVAILABLE,
            description = "Open external directories via Android Storage Access Framework (SAF) folder picker."
        ),

        // Language & Syntax Support
        Capability(
            id = "language.javascript",
            name = "JavaScript Support",
            group = CapabilityGroup.LANGUAGE,
            status = CapabilityStatus.AVAILABLE,
            description = "Syntax highlighting keywords, comment prefixes, and file classification."
        ),
        Capability(
            id = "language.sql",
            name = "SQL Support",
            group = CapabilityGroup.LANGUAGE,
            status = CapabilityStatus.AVAILABLE,
            description = "SQL keyword highlighting, formatting keywords, and database file classification."
        ),
        Capability(
            id = "language.python",
            name = "Python Support",
            group = CapabilityGroup.LANGUAGE,
            status = CapabilityStatus.AVAILABLE,
            description = "Python keyword syntax highlighting, indentation helpers, and classification."
        ),
        Capability(
            id = "language.java",
            name = "Java & Kotlin Support",
            group = CapabilityGroup.LANGUAGE,
            status = CapabilityStatus.AVAILABLE,
            description = "Java & Kotlin keyword highlighting, structure recognition, and file icons."
        ),
        Capability(
            id = "language.html",
            name = "HTML Support",
            group = CapabilityGroup.LANGUAGE,
            status = CapabilityStatus.PARTIAL,
            description = "HTML file recognition, classification, and text editing; full DOM parsing planned."
        ),
        Capability(
            id = "language.css",
            name = "CSS Support",
            group = CapabilityGroup.LANGUAGE,
            status = CapabilityStatus.PARTIAL,
            description = "CSS/SCSS recognition and basic editing; CSS property auto-completion planned."
        ),
        Capability(
            id = "language.json",
            name = "JSON Support",
            group = CapabilityGroup.LANGUAGE,
            status = CapabilityStatus.PARTIAL,
            description = "JSON file classification and basic editing; schema validation planned."
        ),
        Capability(
            id = "language.markdown",
            name = "Markdown Support",
            group = CapabilityGroup.LANGUAGE,
            status = CapabilityStatus.PARTIAL,
            description = "Markdown document recognition and editing; live HTML preview planned."
        ),

        // Version Control (Git)
        Capability(
            id = "git.repository-detection",
            name = "Git Repository Detection",
            group = CapabilityGroup.GIT,
            status = CapabilityStatus.AVAILABLE,
            description = "Inspect .git directory presence to detect version-controlled workspaces."
        ),
        Capability(
            id = "git.status",
            name = "Git Working Tree Status",
            group = CapabilityGroup.GIT,
            status = CapabilityStatus.AVAILABLE,
            description = "Query modified/untracked file count and branch summary."
        ),
        Capability(
            id = "git.branch",
            name = "Git Current Branch Detection",
            group = CapabilityGroup.GIT,
            status = CapabilityStatus.PARTIAL,
            description = "Read current branch name from .git/HEAD; branch switching planned."
        ),
        Capability(
            id = "git.diff",
            name = "Git Diff Viewer",
            group = CapabilityGroup.GIT,
            status = CapabilityStatus.PLANNED,
            description = "Side-by-side or unified diff visualization against HEAD."
        ),
        Capability(
            id = "git.commit",
            name = "Git Commit Operations",
            group = CapabilityGroup.GIT,
            status = CapabilityStatus.PLANNED,
            description = "Stage modified files and create Git commits with message."
        ),
        Capability(
            id = "git.remote",
            name = "Git Remote Operations",
            group = CapabilityGroup.GIT,
            status = CapabilityStatus.PLANNED,
            description = "Push, pull, and fetch from remote Git repositories."
        ),

        // Terminal Workstation
        Capability(
            id = "terminal.ui",
            name = "Terminal UI Console",
            group = CapabilityGroup.TERMINAL,
            status = CapabilityStatus.AVAILABLE,
            description = "Interactive terminal panel with input prompt, history, and ANSI monospace output display."
        ),
        Capability(
            id = "terminal.session",
            name = "In-Memory Terminal Session",
            group = CapabilityGroup.TERMINAL,
            status = CapabilityStatus.PARTIAL,
            description = "Buffered terminal session management with simulated command execution."
        ),
        Capability(
            id = "terminal.real-pty",
            name = "Real PTY Terminal Runner",
            group = CapabilityGroup.TERMINAL,
            status = CapabilityStatus.PLANNED,
            description = "Native pseudoterminal (PTY) runner via JNI/Android subshell."
        ),

        // Database Management
        Capability(
            id = "database.architecture",
            name = "Database Provider Architecture",
            group = CapabilityGroup.DATABASE,
            status = CapabilityStatus.AVAILABLE,
            description = "Abstract DatabaseProvider and QueryEngine contract for data source integration."
        ),
        Capability(
            id = "database.sqlite",
            name = "SQLite Database Provider",
            group = CapabilityGroup.DATABASE,
            status = CapabilityStatus.PARTIAL,
            description = "Internal Room SQLite database active; direct developer SQLite query runner planned."
        ),
        Capability(
            id = "database.mysql",
            name = "MySQL Database Provider",
            group = CapabilityGroup.DATABASE,
            status = CapabilityStatus.PLANNED,
            description = "Network-based MySQL/MariaDB database driver and query console."
        ),
        Capability(
            id = "database.postgresql",
            name = "PostgreSQL Database Provider",
            group = CapabilityGroup.DATABASE,
            status = CapabilityStatus.PLANNED,
            description = "Network-based PostgreSQL database driver and query console."
        ),

        // AI Assistance
        Capability(
            id = "ai.provider-abstraction",
            name = "AI Provider Abstraction",
            group = CapabilityGroup.AI,
            status = CapabilityStatus.AVAILABLE,
            description = "Interface definitions for pluggable AI code completion and assistance engines."
        ),
        Capability(
            id = "ai.service",
            name = "AI Service Lifecycle",
            group = CapabilityGroup.AI,
            status = CapabilityStatus.AVAILABLE,
            description = "Singleton service managing active AI provider registration."
        ),
        Capability(
            id = "ai.provider-configuration",
            name = "AI Provider Configuration",
            group = CapabilityGroup.AI,
            status = CapabilityStatus.PARTIAL,
            description = "Settings UI for AI parameters; live backend integration pending."
        ),

        // UI & Themes
        Capability(
            id = "ui.dark-theme",
            name = "Dark Theme",
            group = CapabilityGroup.UI,
            status = CapabilityStatus.AVAILABLE,
            description = "High-contrast dark color scheme optimized for OLED displays and code readability."
        ),
        Capability(
            id = "ui.light-theme",
            name = "Light Theme",
            group = CapabilityGroup.UI,
            status = CapabilityStatus.AVAILABLE,
            description = "Clean, daylight-readable light color scheme with consistent M3 tokens."
        ),
        Capability(
            id = "ui.responsive-layout",
            name = "Responsive Split Layout",
            group = CapabilityGroup.UI,
            status = CapabilityStatus.AVAILABLE,
            description = "Adaptive layout adapting seamlessly between phones, foldables, and tablets."
        ),
        Capability(
            id = "ui.accessibility",
            name = "Accessibility & TalkBack Support",
            group = CapabilityGroup.UI,
            status = CapabilityStatus.AVAILABLE,
            description = "Semantic content descriptions, 48dp touch targets, and non-color dependent state indicators."
        ),
        Capability(
            id = "ui.command-palette",
            name = "Quick Command Palette",
            group = CapabilityGroup.UI,
            status = CapabilityStatus.AVAILABLE,
            description = "Searchable modal dialog for executing IDE actions and navigating commands."
        ),
        Capability(
            id = "ui.quick-key-bar",
            name = "Key Bar Customization",
            group = CapabilityGroup.UI,
            status = CapabilityStatus.AVAILABLE,
            description = "Configurable key bar density (Compact, Normal, Comfortable) and key sets."
        )
    )

    private val capabilityMap: Map<String, Capability> = catalog.associateBy { it.id }

    init {
        // Enforce uniqueness at initialization
        require(catalog.size == capabilityMap.size) {
            "Duplicate capability IDs detected in CapabilityRegistry catalog"
        }
    }

    /** Returns all registered capabilities. */
    fun getAll(): List<Capability> = catalog

    /** Looks up a capability by unique ID. */
    fun get(id: String): Capability? = capabilityMap[id]

    /** Filters capabilities belonging to a specific subsystem group. */
    fun getByGroup(group: CapabilityGroup): List<Capability> =
        catalog.filter { it.group == group }

    /** Filters capabilities having a specific implementation status. */
    fun getByStatus(status: CapabilityStatus): List<Capability> =
        catalog.filter { it.status == status }

    /** Checks whether a capability is fully implemented and available. */
    fun isAvailable(id: String): Boolean =
        capabilityMap[id]?.status == CapabilityStatus.AVAILABLE

    /** Returns total count of capabilities registered. */
    fun size(): Int = catalog.size
}
