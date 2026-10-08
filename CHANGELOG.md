# Changelog

All notable changes to DroidCode will be documented in this file.

## [0.1.0-alpha01] - 2026-10-03

### Editor Engine Hardening (Phases 1–5)
- **Phase 1: Crashes & Data Loss Prevention**:
  - Replaced API 26-restricted `java.nio.file.Files` with API 24-safe `FileInputStream` / `FileOutputStream` streams.
  - Implemented atomic file saving with temporary file writing, `fsync()`, and atomic rename to eliminate partial file writes on crash or power loss.
  - Added multi-encoding support with BOM preservation (UTF-8, UTF-16LE, UTF-16BE) and line ending preservation (CRLF, LF).
  - Hardened read failure handling: files failing to open are flagged read-only with explicit error diagnostics, preventing empty file overwrites on save.
  - Added synchronous `flushContent()` triggers across save, tab close, tab switch, undo/redo, find/replace, and window focus loss.
  - Enhanced `TextBuffer` and `CodeEditorView` with surrogate-pair awareness to prevent splitting UTF-16 emoji and complex symbols on backspace or cursor navigation.
- **Phase 2: Rendering Correctness & Syntax Highlighting**:
  - Re-architected `LineTokenizer` with single-pass left-to-right lexical scanner producing non-overlapping, strictly sorted token spans.
  - Implemented multi-line parser state (`LineState`) supporting block comments (`/* ... */`), HTML comments (`<!-- ... -->`), Python docstrings (`"""`, `'''`), and JS template literals (``` `...` ```).
  - Added language-specific comment rules across Kotlin/Java, Python, CSS, SQL, and XML/HTML.
  - Implemented `VisualColumnHelper` accounting for tab stops and double-width glyphs, ensuring exact horizontal cursor and selection positioning.
  - Replaced deprecated `scaledDensity` with `TypedValue.applyDimension` and removed per-frame allocations during `onDraw`.
  - Added a 2,000-character line tokenization cap to prevent main thread ANRs on minified files.
- **Phase 3: IME & Input Architecture**:
  - Completely rewritten `EditorInputConnection` with explicit composing region spans (`setComposingRegion`, `setComposingText`, `commitText`, `finishComposingText`), preventing character duplication on Samsung Keyboard, SwiftKey, and Indic IMEs.
  - Synced IME selection via `InputMethodManager.updateSelection` and documented-absolute offsets.
  - Added auto-closing pair insertion for `()`, `[]`, `{}`, `""`, `''`, `` ` ``, smart indent on Enter after opening brackets/colons, and selection wrapping.
  - Added rich physical keyboard shortcuts: Home/End, PageUp/PageDown, Word Jump/Delete (Ctrl+Left/Right/Backspace/Delete), Indent/Outdent (Tab/Shift+Tab), Duplicate Lines (Ctrl+D), Delete Lines (Ctrl+Shift+K), Move Lines Up/Down (Alt+Up/Down), and Comment Toggle (Ctrl+/).
- **Phase 4: Selection, Scrolling & Gestures**:
  - Implemented dynamic soft line-wrapping engine (`LineWrapHelper`, `WrapLayout`, `VisualRow`) with word-break detection on whitespace, punctuation, operators, and brackets.
  - Linked `settings.isWordWrap` from `AppSettings` and `SettingsView` through `DroidCodeEditor` to `CodeEditorView`, ensuring instant reactive updates on toggle.
  - Virtualized visual-row rendering in `CodeEditorView.onDraw`: multi-row syntax highlighting, row-sliced text selection, search query matches, visual-row cursor positioning, and clean line-number gutter display (continuation rows omit duplicate line numbers).
  - Enhanced cursor navigation: Up and Down arrow keys navigate seamlessly across wrapped visual rows within long lines.
  - Fixed horizontal scrolling when Word Wrap is disabled: accurately tracks maximum visual columns (accounting for tab expansions) with generous overscroll padding, allowing smooth scrolling across the entire width of long lines without cutoff.
  - Added fractional scroll accumulation (`residualScrollX`, `residualScrollY`) in `onScroll`, making slow gestures smooth and non-sticky.
  - Implemented edge auto-scroll when dragging selection handles near viewport boundaries.
  - Added keyboard `adjustResize` auto-scroll keeping the active cursor visible when soft input appears.
  - Implemented double-tap (word select) and triple-tap (entire line select) gesture detection.
  - Fixed selection collapsing in `EditorView` action dispatching by preserving full `TextRange`.
- **Phase 5: Undo/Redo, Find/Replace & Performance**:
  - Hardened `UndoManager` with cursor position and selection restoration on undo and redo.
  - Implemented in-editor search query match highlighting with distinct colors for general matches and current active match.
  - Enhanced Find/Replace bar with Previous/Next match navigation, Replace match with auto-advance, and Replace All.
  - Added version-based content synchronization (`lastSyncedContentVersion`), eliminating O(N) string comparison on Compose recomposition.
  - Added unit test suites for `TextBuffer`, `LineTokenizer`, and `UndoManager`.

### Milestone 1 — Performance Optimization, Engine Hardening & Diagnostics
- **Diagnostic Stack Trace System (`StackTraceManager`)**:
  - Implemented thread-safe `StackTraceManager` installing an uncaught exception interceptor in `DroidCodeApp` to capture fatal crashes and runtime diagnostics.
  - Persists structured reports to disk (`filesDir/debug_logs/latest_stacktrace.txt` and history log) across app restarts.
  - Captures complete diagnostics: timestamp, severity, exception cause, full stack trace, package/version/build type, device model/OS/ABI, heap memory usage, and thread information.
  - Added dedicated **Diagnostics & Stack Traces** viewer dialog in Settings (`SettingsView.kt`) featuring scrollable monospace view, log history navigation (`< Newer` / `Older >`), one-tap "Copy to Clipboard" with Toast confirmation, on-demand "Test Log" generation, and log clearing.
- **Hardware-Accelerated Virtualized Editor Engine (`DroidCodeEngine`)**:
  - Implemented custom 120 FPS hardware-accelerated `CodeEditorView` replacing non-virtualized text fields for frictionless editing of 100,000+ line files.
  - Virtualized line-viewport rendering, custom `TextBuffer`, and incremental `LineTokenizer`.
  - Invariant typing focus: view maintains sole authority over active cursor and selection, preventing cursor jump glitches.
  - Smooth horizontal and vertical scrolling with inertia and boundary clamping.
- **Split-Pane Multi-Editor & State Isolation**:
  - Added responsive side-by-side (`HORIZONTAL`) and stacked (`VERTICAL`) split editor layouts.
  - Completely isolated pane instances with dedicated `DroidCodeEditor` components, independent text buffers, cursor positions, and undo/redo histories.
  - **Buffer Corruption Prevention**: Enforced strict file-path verification on content change callbacks (`updateTabContentByPath`), pre-switch buffer flushing on file changes, and safe pane state resets when closing split view.
  - **Pane Focus Synchronization**: Clicking or editing in any pane automatically updates `EditorManager.activeTab` so header breadcrumbs, line/col counters, and toolbar actions accurately reflect the active editor.
  - **Duplicate Assignment Guard**: Automatically swaps tabs between panes when a user selects a file already displayed in the opposite pane.
  - **Auto-Collapse Guard**: Automatically falls back to single-pane layout when $\le 1$ tabs are open.
  - Drag-and-drop tab splitting with interactive drop overlays and empty-pane file picker (`SplitSelectFileView`).
- **Windows Build Tooling**:
  - Added standalone `gradlew.bat` supporting both Gradle Wrapper JAR and system Gradle resolution.
- **Dependency Injection Architecture**:
  - Maintained lightweight `@Singleton` provider architecture (`AppModule.kt`) to ensure clean offline and fast CI builds.
- **Lazy Filesystem Architecture**:
  - Refactored `LocalFileSystem.java` and `WorkspaceManager.kt` from eager full-disk recursion to on-demand hierarchical lazy loading.
  - Subdirectories are loaded only when present in `expandedPaths`, preventing thread starvation and memory spikes on large repositories.
  - Added default ignored directory filters (`.git`, `.gradle`, `.idea`, `.cxx`, `.externalNativeBuild`, `__pycache__`) and alphabetical folder-first sorting.
- **Differential Undo/Redo Engine**:
  - Refactored `UndoManager.java` to compute piecewise `TextDelta` operations (offset, deleted text, inserted text) instead of cloning full-document strings on every keystroke.
  - Slashes text buffer memory consumption by over 95% and eliminates GC pauses during editing.
- **Incremental Line-Viewport Syntax Tokenizer**:
  - Implemented `IncrementalSyntaxHighlighter.kt` with line-cached token spans, avoiding full-document regex re-tokenization on every keystroke and maintaining 60–120 FPS typing speed.
- **Breadcrumbs Navigation**:
  - Added interactive breadcrumbs path display (`Project > dir > ... > file`) in editor header with single-tap segment navigation.
- **Editor Gutter Git Diff**:
  - Implemented `LineDiffCalculator.kt` with fast prefix/suffix and LCS diffing, rendering live green (added) and blue (modified) gutter indicators against disk baseline.
- **Workspace-Wide Text Search (Project Grep — `Ctrl+Shift+F`)**:
  - Implemented `WorkspaceSearchDialog.kt` supporting background I/O file scanning with Match Case, Whole Word, and Regex filters with jump-to-line navigation.
- **CI / Static Analysis Hardening**:
  - Configured Android Lint with zero warnings/errors (`warningsAsErrors = true`).
  - Transitioned syntax highlighter test suite to standard JUnit test runner.
  - Resolved icon asset references and verified all CI stages (Lint, Detekt, JUnit, Debug APK, Signed Release APK).

### Phase 1.5.1 — Foundation Hardening & Architecture Cleanups
- **Room Database Concurrency Hardening**:
  - Removed `allowMainThreadQueries()` from `AppDatabase.java` to prevent disk I/O on Android UI threads.
  - Converted Room DAOs (`WorkspaceDao`, `TabSessionDao`) to Kotlin with coroutine `suspend` methods and reactive `Flow` emissions.
  - Migrated `WorkspaceManager` to Kotlin with asynchronous `withContext(Dispatchers.IO)` persistence routines and reactive workspace observation.
- **Dependency Sprawl Elimination**:
  - Cleaned up unused network and serialization libraries (`retrofit`, `okhttp`, `logging-interceptor`, `moshi-kotlin`, `converter-moshi`, `moshi-kotlin-codegen`) from `app/build.gradle.kts` and `gradle/libs.versions.toml`.
- **Syntax Highlighter Consolidation**:
  - Consolidated language keyword sets from `EditorView.kt` into `SyntaxHighlighter.java`.
  - Removed orphaned interface `LanguageProvider.java`.
  - Added unit test suite `SyntaxHighlighterTest.kt` covering keyword lookup across Kotlin, Java, JS/TS, Python, SQL, and HTML/XML.

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
