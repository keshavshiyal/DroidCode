# DroidCode Roadmap

DroidCode follows an incremental, architectural roadmap to build a native Android IDE.

## Milestones

### M0 — Architecture & Foundation (Current - Phase 1)
- Modular Android application structure in Java & Jetpack Compose
- Real local filesystem abstraction and Storage Access Framework (SAF) integration
- Multi-tab file editor with real read/write/save, line numbers, cursor state, undo/redo
- Mobile Developer Quick Key Bar with sticky modifiers (Ctrl, Shift, Alt) and symbol shortcuts
- Centralized Command System and Command Palette
- Persisted Settings engine (Theme, Editor Font Size, Word Wrap, Quick Key Bar density)
- Real Workspace Manager with recent projects local persistence
- Git status architectural detection (Honest "Not a Git repository" handling)
- Terminal, Database, Extension, and AI service provider abstractions
- GitHub Actions CI build pipeline

### M1 — Professional IDE Shell Polish
- Split editor panes (horizontal/vertical)
- Advanced file search (grep across workspace)
- Enhanced syntax highlighting engines for Stage 1 languages
- Diff viewer for modified workspace files

### M2 — Web IDE (Stage 1 Language Ecosystem)
- HTML live preview renderer (local WebView sandbox)
- CSS style auto-completion
- JS lightweight syntax checker
- JSON formatter and validator
- SQLite query execution console

### M3 — Developer Infrastructure
- Full JGit integration for local Git commits, branches, and diffs
- Terminal process engine using local Android PTY/shell execution
- Extension loading mechanism

### M4 — Python (Stage 2 Language Ecosystem)
- Python syntax parser
- Local Chaquopy / Python interpreter integration for script execution

### M5 — Java Engine (Stage 3 Language Ecosystem)
- ECJ / Java compiler integration
- Maven / Gradle project structure parsing

### M6 — Kotlin & Android Workflows (Stage 4 Language Ecosystem)
- Kotlin compiler & language server setup
- Android SDK toolchain & APK builder
- Local Logcat viewer

### M7 — Advanced Ecosystem
- C/C++, Rust, Go, PHP support
- Multi-database provider tools (MySQL, PostgreSQL, MongoDB)
- Remote SSH workspace connections
