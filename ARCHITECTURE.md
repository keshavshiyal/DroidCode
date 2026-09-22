# DroidCode Architecture

DroidCode is architected as a modular, production-ready, native Android IDE using Jetpack Compose (Kotlin) for UI components and Java/Kotlin service abstractions for core IDE subsystems.

## Technical Architecture Stack

- **UI & Presentation Layer**: Kotlin + Jetpack Compose (Material Design 3)
- **Domain & Service Layer**: Java / Kotlin robust services (`CommandRegistry`, `WorkspaceManager`, `LocalFileSystem`, `EditorManager`, `SettingsManager`)
- **Persistence Layer**: Android Room Database (SQLite) + SharedPreferences
- **Asynchronous Execution**: Kotlin Coroutines & Java Concurrent Executores

## High-Level Component Topology

```
+-----------------------------------------------------------------------+
|                              UI Shell                                 |
|   (Activity, Composable Screens, Navigation, Theme Engine)             |
+-----------------------------------+-----------------------------------+
                                    |
+-----------------------------------v-----------------------------------+
|                           Command Registry                            |
|             (Unified Command Bus, Keyboard Shortcuts)                 |
+------+---------------+---------------+---------------+----------------+
       |               |               |               |
+------v------+ +------v------+ +------v------+ +------v------+
| Workspace   | | Editor      | | Filesystem  | | Settings    |
| Manager     | | Buffer &    | | Operations  | | Subsystem   |
| (Projects)  | | Tab State   | | (Local/SAF) | | (Preferences)|
+------+------+ +------+------+ +------+------+ +------+------+
       |               |               |               |
+------v---------------v---------------v---------------v----------------+
|                          Subsystem Adapters                           |
|       Git Service  |  Terminal  |  Database  |  Language  |  AI       |
+-----------------------------------------------------------------------+
```

## Modular Package Structure

- `com.droidcode.core`: Central domain models (`Command`, `Event`, `Result`, `Disposable`)
- `com.droidcode.core.capability`: System capability registry, status descriptors, and verification catalog
- `com.droidcode.project`: Workspace models (`Project`, `WorkspaceManager`, `ProjectTemplate`)
- `com.droidcode.filesystem`: Real file system layer (`FileNode`, `LocalFileSystem`, SAF handling)
- `com.droidcode.editor`: Text buffer management, editor tabs, cursor state, undo history
- `com.droidcode.settings`: App configuration manager (`AppSettings`, `ThemeMode`, `KeyBarDensity`)
- `com.droidcode.git`: Git status provider and repository abstraction
- `com.droidcode.terminal`: Terminal session manager and PTY runner abstraction
- `com.droidcode.database`: Generic database provider interface (`DatabaseProvider`, `QueryEngine`)
- `com.droidcode.language`: Language definitions, tokenizers, syntax highlighters
- `com.droidcode.ai`: Optional AI service provider interface
- `com.droidcode.extensions`: Modular plugin and extension lifecycle manager
- `com.droidcode.db`: Room database entities & DAOs for persisting workspaces and open tab sessions
- `com.droidcode.ui`: Jetpack Compose views, layouts, themes, and Developer Quick Key Bar

## Core Architectural Principles

1. **SOLID & Dependency Inversion**: Core modules depend on interfaces, not implementations.
2. **Strict Real State Principle**: No hardcoded demo content in production code. Empty states reflect true system conditions.
3. **Unified Command Architecture**: Keybindings, quick key bar, command palette, and menu items execute the exact same command objects registered in `CommandRegistry`.
4. **Offline-First & Local Storage**: Workspace state, files, and user preferences are local and offline by default.
5. **Thread Safety**: File I/O and heavy parsing execute off the main thread using Coroutines and background Executors.
