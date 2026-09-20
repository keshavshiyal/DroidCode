# Changelog

All notable changes to DroidCode will be documented in this file.

## [0.1.0-alpha01] - 2026-09-20

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
