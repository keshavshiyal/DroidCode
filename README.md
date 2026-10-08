# DroidCode

**DroidCode** is a native, professional open-source IDE written in Java and Kotlin (Jetpack Compose) for Android.

Inspired by VS Code, IntelliJ IDEA, and Android Studio, DroidCode delivers a genuine workstation experience directly on mobile phones, foldables, and tablets.

---

## Current Status: Milestones 1 & 2 Complete → Advancing to Milestone 3 (Developer Infrastructure)

DroidCode has completed **Milestone 1 (M1 — Core Engine & Shell Hardening)** and **Milestone 2 (M2 — Web IDE & Tooling Ecosystem)**. The workstation delivers a desktop-grade developer workflow on Android with a 120 FPS hardware-accelerated code editor, resilient split-pane editing, live HTML rendering with console interception, CSS color swatches with Material 3 color picker, JS syntax analysis, JSON tree viewing, and a native SQLite database explorer and query console.

### Key Highlights & Capabilities

- **120 FPS Virtualized Editor (`DroidCodeEngine`)**: Custom hardware-accelerated canvas (`CodeEditorView`) capable of fluid, frictionless editing across 100,000+ line codebases with zero lag or frame drops.
- **Independent Split-Pane Multi-Editor**: Side-by-side (`HORIZONTAL`) and stacked (`VERTICAL`) split layouts featuring isolated `DroidCodeEditor` instances, active pane focus synchronization, and bulletproof buffer corruption protection.
- **HTML Live Preview & Console Logger**: Sandboxed local `WebView` preview dialog with relative file URI asset resolution and console logging drawer (intercepting `LOG`, `DEBUG`, `WARN`, `ERROR` with source lines).
- **CSS Color Detection & Material 3 HSV Color Picker**: Regex parser for `#RGB`, `#RGBA`, `#RRGGBB`, `#RRGGBBAA`, `rgb()`, `rgba()`, `hsl()`, `hsla()`, with Material 3 HSV slider dialog, palette presets, and cursor-position hex insertion.
- **JavaScript Lightweight Syntax Checker & Linter**: In-app lexical scanner detecting unclosed brackets, unclosed strings, and unclosed template literals with interactive problems dialog and tap-to-jump line navigation.
- **JSON Tools & Hierarchical Tree Viewer**: Pure `org.json` formatting (beautify), whitespace minification, syntax validation with line/column diagnosis, and expandable tree viewer with search and type badges.
- **SQLite Database Explorer & Query Console**: Local database provider detecting `.db`, `.sqlite`, and `.sqlite3` files, schema extraction from `sqlite_master` and `PRAGMA table_info`, query runner with execution timing, 2D scrollable data grid, and clipboard CSV export.
- **Diagnostics & In-App Stack Trace Viewer (`StackTraceManager`)**: Automatic unhandled crash interception that persists debug reports across restarts; includes a dedicated in-app viewer in Settings with one-tap clipboard copy and device/OS environment snapshots.
- **Workspace-Wide Text Search (Project Grep — `Ctrl+Shift+F`)**: Non-blocking background file scanning with Match Case, Whole Word, and Regex filtering, complete with direct jump-to-line navigation.
- **Live Gutter Git Diff**: Background LCS differential calculation rendering real-time green (added) and blue (modified) diff indicators against disk baselines.
- **Interactive Breadcrumb Navigation**: Single-tap segment navigation (`Project > directory > ... > file`) directly above the editor canvas.
- **Developer Quick Key Bar**: Bracket auto-closing for `(`, `[`, `{`, `"`, `'`, `` ` `` with cursor placement in the middle, and typeover navigation without duplicate characters.

### Capability Registry & Feature Status

| Feature ID | Subsystem | Capability | Status | Architecture Notes |
| :--- | :--- | :--- | :---: | :--- |
| **CORE-001** | Core | Dependency Injection | `AVAILABLE` | Hilt DI with `@Singleton` managers (`AppModule.kt`) |
| **CORE-002** | Core | Command Architecture | `AVAILABLE` | Unified Command Registry & Event Bus |
| **CORE-003** | Core | Capability Registry | `AVAILABLE` | Honest programmatic feature discovery (`CapabilityRegistry`) |
| **NAV-001** | Navigation | Navigation Compose | `AVAILABLE` | Routes: `Home`, `Workspace`, `Settings`, `Git`, `Terminal` |
| **UI-001** | Shell | Adaptive Multi-Pane & Split | `AVAILABLE` | Horizontal & Vertical split layouts with pane isolation |
| **UI-002** | Shell | Theme System | `AVAILABLE` | Deep dark & soft light Material 3 theming |
| **UI-003** | Shell | Developer Quick Key Bar | `AVAILABLE` | Sticky modifiers (`Ctrl`, `Shift`, `Alt`), fast syntax symbols, auto-close |
| **UI-004** | Shell | Command Palette | `AVAILABLE` | Fuzzy search & ranking dialog (`Ctrl+Shift+P`) |
| **WORKSPACE-001** | Workspace | Workspace Management | `AVAILABLE` | Room DB persistence, SAF folder selection, non-destructive reconnect |
| **FILES-001** | Filesystem | Local File System & SAF | `AVAILABLE` | Real CRUD operations, lazy directory tree loading, SAF document tree |
| **FILES-002** | Filesystem | File Icon Classification | `AVAILABLE` | 40+ language, config, and media extensions mapped |
| **EDITOR-001** | Editor | 120 FPS Virtualized Engine | `AVAILABLE` | `DroidCodeEngine` with line-viewport rendering for 100K+ lines |
| **EDITOR-002** | Editor | Multi-Tab Buffer Management | `AVAILABLE` | Differential undo/redo (`TextDelta`), dirty indicators, cursor tracking |
| **EDITOR-003** | Editor | Truthful Actions & Git Diff | `AVAILABLE` | Real disk-to-buffer LCS gutter diff, honest runtime reporting |
| **SEARCH-001** | Search | Workspace Search (Project Grep) | `AVAILABLE` | Background regex/case/word search dialog (`Ctrl+Shift+F`) |
| **DEBUG-001** | Diagnostics | Stack Trace & Crash Logger | `AVAILABLE` | `StackTraceManager` crash interception & in-app viewer in Settings |
| **WEB-001** | Web Tooling | HTML Live Preview & Console | `AVAILABLE` | Sandboxed `WebView` with asset loading and console logger drawer |
| **CSS-001** | Web Tooling | CSS Colors & Color Picker | `AVAILABLE` | Hex/RGB/HSL detection and Material 3 HSV color picker |
| **JS-001** | Web Tooling | JS Syntax Checker & Linter | `AVAILABLE` | Bracket & string delimiter validation, Problems list dialog |
| **JSON-001** | Web Tooling | JSON Tools & Tree Viewer | `AVAILABLE` | Format, minify, validation, and expandable tree viewer |
| **DB-001** | Database | SQLite Explorer & Console | `AVAILABLE` | SQLite schema inspector, query execution, and CSV export |
| **MEDIA-001** | Media | Image & Document Viewer | `AVAILABLE` | Resilient raster/modern image decoding, PDF preview |
| **SETTINGS-001** | Settings | Settings Subsystem | `AVAILABLE` | Preferences persistence for editor, typography, key bar, diagnostics |
| **GIT-001** | VCS | Git Service Integration | `PARTIAL` | Workspace `.git` inspection, branch detection, buffer diff |
| **TERMINAL-001** | Runtime | Terminal Subsystem | `PARTIAL` | Terminal panel shell with real execution diagnostics |
| **AI-001** | AI | AI Assistant Services | `PLANNED` | Provider abstraction for future localized & remote models |
| **CI-001** | CI/CD | GitHub Actions Workflow | `AVAILABLE` | Android Lint, Detekt static analysis, unit test suite, APK assembly |
| **TEST-001** | Quality | Automated Test Suite | `AVAILABLE` | Unit tests, Robolectric tests, Compose UI tests & benchmarks |

---

## Architecture

DroidCode uses a **hybrid architecture**:
- **Domain & Data Layer (Java & Kotlin)**: Core models, filesystem operations (`LocalFileSystem`), workspace management (`WorkspaceManager`), command registry (`CommandRegistry`), event bus (`EventBus`), diagnostic engine (`StackTraceManager`), and Room SQLite persistence (`DroidCodeDatabase`).
- **UI Layer (Kotlin & Jetpack Compose Material 3)**: Modern declarative interface with `EditorView`, `ExplorerPanel`, `QuickKeyBar`, `CommandPaletteDialog`, `SettingsView`, and `HomeView`.
- **Editor Engine Layer (`com.droidcode.editor.engine`)**: Native virtualized 120 FPS canvas (`CodeEditorView`), piecewise `TextBuffer`, and incremental `LineTokenizer`.
- **Threading Model**: IO operations dispatch cleanly on background threads (`Dispatchers.IO`), while Compose state drives reactive updates smoothly on the UI thread.

---

## Building from Source

### Prerequisites
- JDK 17
- Android SDK 36 (Minimum supported: Android 7.0 / API 24)
- Gradle 8.x (managed via Gradle wrapper or system Gradle)

### Build Commands
```bash
# Build Debug APK
gradle :app:assembleDebug

# Run Full Unit & Robolectric Test Suite
gradle :app:testDebugUnitTest
```

---

## Testing

DroidCode maintains an extensive automated test suite with 100% pass rate:
- `WorkspaceManagerTest`: Workspace creation, opening, closing, recent workspace persistence, and non-destructive removal.
- `EditorManagerTest`: Multi-tab lifecycle, active tab tracking, content updates, dirty state, undo/redo, and tab closing.
- `LocalFileSystemTest`: File/directory creation, reading, writing, recursive directory listing, and recursive deletion.
- `FileIconUtilsTest`: Extension categorization and icon mapping across all supported language and asset types.
- `SettingsManagerTest`: Default settings verification, modification, and persistent storage.
- `DroidCodeUnitTest`: Core command registry, event bus, tab cursor positioning, and project template generation.
- `Milestone2WebIdeTest`: JSON formatting/minifying/validation/tree building, CSS color extraction/parsing/hex conversion, and JavaScript syntax/unmatched bracket checking.

---

## Documentation

- [Project Status & Feature Registry](DROIDCODE_PROJECT_STATUS.md)
- [Roadmap & Milestones](ROADMAP.md)
- [Architecture Overview](docs/architecture/overview.md)
- [Phase 1 & 1.5 Feature Specification](docs/features/phase1.md)
- [Testing Guide](docs/testing/testing_guide.md)
- [Phase 1.5 Audit Report](docs/testing/phase1_5_audit.md)
- [Signing Key Setup Guide](SIGNING_KEY_SETUP.md)
- [Changelog](CHANGELOG.md)
- [Contributing Guidelines](CONTRIBUTING.md)

---

## License

DroidCode is open source software licensed under the Apache License 2.0.
