# Changelog

All notable changes to DroidCode will be documented in this file.

## [0.1.0-alpha01] - 2026-09-21

### Phase 1.5 — Stabilization & Professionalization
- **Workspace & State Persistence**:
  - Refactored `WorkspaceManager.java` to prevent destructive auto-deletion of moved or disconnected workspaces.
  - Enhanced `HomeView.kt` with explicit "Unavailable" badges for missing directories and added an explicit removal action.
  - Added `@NonNull` annotations to Room DAOs (`WorkspaceDao`, `TabSessionDao`) to eliminate compiler warnings.
- **Truthful Context Actions (Zero Mock Data)**:
  - Eliminated fabricated commit hashes, fake git blame, and mock terminal/python execution output from `EditorActionsHandler.kt`.
  - Implemented real line-by-line Git Diff comparison between memory buffer and original disk contents.
  - Implemented honest, professional dialogs for Run/Debug stating runtime availability plans for Milestone 3/4.
- **File System & Explorer Polish**:
  - Expanded `FileIconUtils.kt` to classify over 40 file extensions (.sass, .toml, .ini, .rst, .gradle, .gradle.kts, .tif, .tiff, .avif, .heic, .heif, LICENSE, .editorconfig).
  - Added pure, non-composable `classifyFile()` method enabling comprehensive JVM unit testing.
  - Hardened `LocalFileSystem.java` with recursive directory copying, safe deletion, and unique file duplication naming.
- **Media & Asset Viewer**:
  - Enhanced `MediaViewer.kt` and `EditorTab.kt` to detect modern image formats (.tif, .tiff, .avif, .heic, .heif).
  - Added visual error recovery states and external viewer intents when image decoding encounters unsupported or corrupted files.
- **Build System & CI Hardening**:
  - Removed unused Google Services plugin and Firebase dependencies from `app/build.gradle.kts`.
  - Hardened GitHub Actions CI pipeline (`.github/workflows/build.yml`) to enforce clean unit test execution before packaging debug APKs.
- **Error Handling & Observability**:
  - Eliminated silent catch blocks across `EditorView.kt`, `MainShell.kt`, and `GitService.java`, replacing them with structured Android logging and contextual user toasts.
- **Test Suite Modernization**:
  - Removed generated template tests (`ExampleRobolectricTest`, `ExampleUnitTest`, `ExampleInstrumentedTest`).
  - Added comprehensive test suites: `WorkspaceManagerTest`, `EditorManagerTest`, `LocalFileSystemTest`, `FileIconUtilsTest`, and `SettingsManagerTest`.
  - Achieved 100% test pass rate across 33 automated tests.

---

### Phase 1 — Foundation
- Modular Java-based architecture with Jetpack Compose UI shell.
- Real workspace and filesystem manager backed by Android Storage Access Framework (SAF) & local storage.
- Multi-tab code editor with file loading, editing, saving, line numbers, cursor position tracking, and undo/redo.
- Developer Quick Key Bar with modifier key support (`Ctrl`, `Shift`, `Alt`), long-press symbol menus, and command shortcuts.
- Centralized Command Registry and filterable Command Palette (`Ctrl+Shift+P` / `Ctrl+P`).
- Persisted Settings subsystem supporting Theme switching (Dark/Light/System), Editor font size, word wrapping, and Quick Key Bar density.
- Real Git, Terminal, Database, Extension, and AI service layer architectural foundations.
- Room database for recent workspace persistence and session history.
- GitHub Actions CI build workflow (`.github/workflows/build.yml`).
