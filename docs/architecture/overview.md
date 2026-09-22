# Architectural Overview

DroidCode is architected as a native Android IDE designed for high reliability, responsiveness, and clean separation between domain logic and declarative user interfaces.

---

## 1. Architectural Philosophy

1. **Decoupled Domain & UI**: The core logic (filesystem, editor state, workspace management, command registry, settings) is implemented in clean, testable Java and Kotlin classes independent of the UI layer.
2. **Declarative Presentation**: The user interface is implemented entirely with Jetpack Compose (Material Design 3), consuming domain models and emitting user actions.
3. **Truthful Operations**: The IDE interacts exclusively with real device storage, real Git repositories, and genuine runtime environments. It contains zero simulated or fake production data.
4. **Resilient Persistence**: Workspaces, tab sessions, and configuration are persisted using Room (SQLite) and Android SharedPreferences. Disconnected or moved directories remain accessible in a recoverable state.

---

## 2. Layered Architecture

```
┌────────────────────────────────────────────────────────┐
│                   Jetpack Compose UI                   │
│   (HomeView, MainShell, EditorView, ExplorerPanel,    │
│    QuickKeyBar, CommandPalette, SettingsView)          │
└───────────────────────────┬────────────────────────────┘
                            │ Observers / Actions
┌───────────────────────────▼────────────────────────────┐
│                    Editor & Workspace                  │
│       (EditorManager, EditorTab, WorkspaceManager)     │
└───────────────────────────┬────────────────────────────┘
                            │ Operations
┌───────────────────────────▼────────────────────────────┐
│                 Core Domain & Subsystems               │
│  - CommandRegistry & EventBus                          │
│  - LocalFileSystem & SafUtils                          │
│  - GitService (Real .git parser)                       │
│  - SettingsManager & AppSettings                       │
│  - Media & Document Viewers                            │
└───────────────────────────┬────────────────────────────┘
                            │ Persistence
┌───────────────────────────▼────────────────────────────┐
│                  Local Storage & Room                  │
│    (DroidCodeDatabase, WorkspaceDao, TabSessionDao,    │
│     SharedPreferences, Storage Access Framework)       │
└────────────────────────────────────────────────────────┘
```

---

## 3. Subsystem Breakdown

### 3.1 Capability Subsystem (`com.droidcode.core.capability`)
- **`CapabilityRegistry`**: Central descriptive registry declaring exact feature maturity (`PLANNED`, `PARTIAL`, `AVAILABLE`) to prevent simulated claims. See [Capability Registry](capability_registry.md).

### 3.2 Filesystem Subsystem (`com.droidcode.filesystem`)
- **`LocalFileSystem`**: Core singleton providing thread-safe operations on real files and directories (`createFile`, `createDirectory`, `readFileToString`, `writeStringToFile`, `copyFileOrDirectory`, `moveFileOrDirectory`, `renameFile`, `deleteFile`, `listDirectoryRecursive`).
- **`SafUtils`**: Bridges modern Android Storage Access Framework (SAF) document URIs to standard local storage paths when user selects external storage trees.
- **`FileNode`**: Tree node data structure representing hierarchical files and directories for `ExplorerPanel`.

### 3.3 Editor Subsystem (`com.droidcode.editor`)
- **`EditorManager`**: Central controller managing open tabs (`tabs`), active tab tracking (`activeTabIndex`), multi-file lifecycle, and undo/redo histories.
- **`EditorTab`**: Encapsulates active file path, title, in-memory buffer, cursor position (line and column), dirty state (`isModified`), and viewer type (Text, Image, PDF, Video, Audio, Binary).
- **`UndoManager`**: Bounded double-ended stack providing undo and redo capabilities for buffer edits.

### 3.4 Workspace Subsystem (`com.droidcode.project`)
- **`WorkspaceManager`**: Manages the current active project (`currentProject`), open/close lifecycle, and persists recent workspaces to Room SQLite database via `WorkspaceDao`.
- **Non-Destructive Persistence**: Inaccessible directories are flagged rather than deleted, allowing users to reconnect moved folders or explicitly dismiss them.

### 3.5 Command & Event Subsystem (`com.droidcode.core`)
- **`CommandRegistry`**: Central registry of all executable IDE actions with title, category, keyboard shortcut, and optional dynamic `enabledSupplier`.
- **`EventBus`**: Lightweight publish/subscribe bus for decoupled inter-component events.

### 3.6 Settings Subsystem (`com.droidcode.settings`)
- **`SettingsManager`**: SharedPreferences-backed singleton managing `AppSettings`.
- **`AppSettings`**: Configurable preferences including theme mode (Dark / Light / System), font size (sp), word wrap, Quick Key Bar visibility and density (Compact / Normal / Comfortable), line numbers, and font family (JetBrains Mono, Fira Code, Roboto Mono, Source Code Pro).

### 3.6 Abstraction Layers (Future Milestones)
- **`GitService`**: Real Git detection inspecting `.git/HEAD` for branch names, with honest empty state reporting when outside a repository.
- **`TerminalService`**, **`DatabaseService`**, **`AiService`**: Architectural contracts providing stable interfaces for Milestone 3+ engine integrations.

---

## 4. Threading & Concurrency Model

- **Background IO**: All disk access (`LocalFileSystem`), Room database queries, and SAF operations are executed on background threads (`Dispatchers.IO`).
- **Main Thread UI**: Jetpack Compose state mutations (`mutableStateOf`, `mutableStateListOf`, `collectAsStateWithLifecycle`) execute on the Main thread to ensure flicker-free rendering.
