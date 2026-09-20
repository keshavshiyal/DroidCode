# DroidCode Architecture

DroidCode is architected as a modular, production-ready, native Android IDE written in Java with Jetpack Compose UI.

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

- `com.example.core`: Central domain models (`Command`, `Event`, `Result`, `Disposable`)
- `com.example.project`: Workspace models (`Project`, `WorkspaceManager`, `ProjectTemplate`)
- `com.example.filesystem`: Real file system layer (`FileNode`, `LocalFileSystem`, SAF handling)
- `com.example.editor`: Text buffer management, editor tabs, cursor state, undo history
- `com.example.settings`: App configuration manager (`AppSettings`, `ThemeMode`, `KeyBarDensity`)
- `com.example.git`: Git status provider and repository abstraction
- `com.example.terminal`: Terminal session manager and PTY runner abstraction
- `com.example.database`: Generic database provider interface (`DatabaseProvider`, `QueryEngine`)
- `com.example.language`: Language definitions, tokenizers, syntax highlighters
- `com.example.ai`: Optional AI service provider interface
- `com.example.extensions`: Modular plugin and extension lifecycle manager
- `com.example.db`: Room database entities & DAOs for persisting workspaces and open tab sessions
- `com.example.ui`: Jetpack Compose views, layouts, themes, and Developer Quick Key Bar

## Core Architectural Principles

1. **SOLID & Dependency Inversion**: Core modules depend on interfaces, not implementations.
2. **Strict Real State Principle**: No hardcoded demo content in production code. Empty states reflect true system conditions.
3. **Unified Command Architecture**: Keybindings, quick key bar, command palette, and menu items execute the exact same command objects registered in `CommandRegistry`.
4. **Offline-First & Local Storage**: Workspace state, files, and user preferences are local and offline by default.
5. **Thread Safety**: File I/O and heavy parsing execute off the main thread using Coroutines and background Executors.
