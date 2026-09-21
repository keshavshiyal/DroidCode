# DroidCode Roadmap

DroidCode follows an incremental, architectural roadmap to build a native Android IDE.

---

## Phase Progression

```
[Phase 1: Architecture Foundation]  -->  [Phase 1.5: Stabilization & Quality] (CURRENT)  -->  [Phase 2 / M1: Shell Polish]  -->  [M2: Web IDE]
```

---

## Milestones

### Phase 1 — Architecture & Foundation (Complete)
- Modular Android application structure combining clean Java core models with Jetpack Compose UI
- Real local filesystem abstraction and Storage Access Framework (SAF) integration
- Multi-tab file editor with real read/write/save, line numbers, cursor state, undo/redo
- Mobile Developer Quick Key Bar with sticky modifiers (`Ctrl`, `Shift`, `Alt`) and symbol shortcuts
- Centralized Command System and Command Palette
- Persisted Settings engine (Theme, Editor Font Size, Word Wrap, Quick Key Bar density)
- Real Workspace Manager with recent projects local persistence
- Git status architectural detection (Honest "Not a Git repository" handling)
- Terminal, Database, Extension, and AI service provider abstractions

### Phase 1.5 — Stabilization & Professionalization (Complete - Current)
- **Dependency & Build Cleanliness**: Completely decoupled from Firebase and Google Services plugins; self-contained offline build.
- **Continuous Integration Pipeline**: Robust `.github/workflows/build.yml` with strict test execution before packaging.
- **Workspace Reliability**: Non-destructive workspace handling; moved/inaccessible directories are preserved in a recoverable state with explicit UI indicators and removal controls.
- **Truthful Context Actions**: Removed all fabricated mock data from editor actions; real buffer-to-disk Git Diff and truthful runtime notifications.
- **Expanded Icon System**: Support for 40+ language, config, and media extensions with unit test validation.
- **Media Viewer Hardening**: Error states and graceful degradation for unsupported/corrupt image decoding.
- **Test Suite Modernization**: Replaced sample template tests with real unit tests covering `WorkspaceManager`, `EditorManager`, `LocalFileSystem`, `FileIconUtils`, `SettingsManager`, and core command/event engines.
- **Observability**: Replaced silent catch blocks with explicit Android logging and user toasts.

### Phase 2 / Milestone 1 — Professional IDE Shell Polish (Next)
- Split editor panes (horizontal and vertical multi-pane layouts)
- Advanced workspace text search (recursive grep across project files)
- Enhanced syntax highlighting engines for Stage 1 languages
- Side-by-side graphical diff viewer for modified files
- Breadcrumb navigation bar above active editor tab

### Milestone 2 — Web IDE (Stage 1 Language Ecosystem)
- HTML live preview renderer (sandboxed local WebView)
- CSS style auto-completion and color picker
- JavaScript lightweight syntax checking and linting
- JSON formatter, validator, and tree viewer
- SQLite query execution console for local databases

### Milestone 3 — Developer Infrastructure
- Full JGit integration for local Git commits, branches, staging, and diffs
- Terminal process engine using local Android PTY/shell execution
- Extension loading mechanism and plugin API

### Milestone 4 — Python (Stage 2 Language Ecosystem)
- Python syntax parser and code analysis
- Local Chaquopy / Python interpreter integration for script execution

### Milestone 5 — Java Engine (Stage 3 Language Ecosystem)
- ECJ / Eclipse Compiler for Java integration
- Maven / Gradle project structure parsing and dependency inspection

### Milestone 6 — Kotlin & Android Workflows (Stage 4 Language Ecosystem)
- Kotlin compiler & language server integration
- Android SDK toolchain & local APK packaging
- Real-time Logcat viewer and ADB device bridge

### Milestone 7 — Advanced Ecosystem
- C/C++, Rust, Go, PHP support
- Multi-database provider tools (MySQL, PostgreSQL, MongoDB connections)
- Remote SSH workspace connections
