# Changelog

All notable changes to DroidCode will be documented in this file.

## [0.1.0-alpha01] - 2026-09-21

### Verified & Audit Fixed
- **Phase 1 Audit & Verification**:
  - Audited full repository, source code, CI workflow, test suite, and documentation (`docs/testing/phase1_audit.md`).
  - Removed outdated template screenshot test and stale image artifact (`greeting.png`), restoring 100% clean unit test execution.
  - Cleaned up CI workflow (`.github/workflows/build.yml`) by removing fake Google Services JSON generation and adding automated unit test validation prior to debug APK packaging.
  - Unified project versioning to `0.1.0-alpha01` across `app/build.gradle.kts`, `DROIDCODE_PROJECT_STATUS.md`, and changelogs.
  - Expanded unit test coverage in `DroidCodeUnitTest.kt` for `CommandRegistry`, `EventBus`, `EditorTab`, `UndoManager`, `LocalFileSystem`, `ProjectTemplate`, `AppSettings`, and `Project`.
  - Fixed Robolectric runner SDK configuration in `ExampleRobolectricTest.kt` (`@Config(sdk = [34])`) to resolve `UnsupportedOperationException` in `DefaultSdkProvider` on API 36.
  - Resolved Material Icon deprecation warnings across `EditorView`, `ExplorerPanel`, `FileIconUtils`, and `SettingsView`.
  - Updated `ARCHITECTURE.md` to formally document the Kotlin/Compose UI and Java/Kotlin domain service architecture.

### Added
- **Phase 1 Foundation**:
  - Modular Java-based architecture with Jetpack Compose UI shell.
  - Real workspace and filesystem manager backed by Android Storage Access Framework (SAF) & local storage.
  - Multi-tab code editor with file loading, editing, saving, line numbers, cursor position tracking, and undo/redo.
  - Developer Quick Key Bar with modifier key support (`Ctrl`, `Shift`, `Alt`), long-press symbol menus, and command shortcuts.
  - Centralized Command Registry and filterable Command Palette (`Ctrl+Shift+P` / `Ctrl+P`).
  - Persisted Settings subsystem supporting Theme switching (Dark/Light/System), Editor font size, word wrapping, and Quick Key Bar density.
  - Real Git, Terminal, Database, Extension, and AI service layer architectural foundations.
  - Room database for recent workspace persistence and session history.
  - GitHub Actions CI build workflow (`.github/workflows/build.yml`).
