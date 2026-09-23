# DroidCode

**DroidCode** is a native, professional open-source IDE written in Java and Kotlin (Jetpack Compose) for Android.

Inspired by VS Code, IntelliJ IDEA, and Android Studio, DroidCode delivers a genuine workstation experience directly on mobile phones, foldables, and tablets.

---

## Current Status: Phase 1.5 Foundation (v0.1.0-alpha01)

DroidCode has achieved **Phase 1.5 (Stabilization & Professionalization)**. All core subsystems operate with real local persistence, truthful status reporting, zero mock data, and comprehensive test coverage.

### Key Capabilities

- **Real Filesystem & Workspace Engine**: Create, open, edit, rename, duplicate, and delete real files and workspace directories locally or via Android Storage Access Framework (SAF). Inaccessible/moved directories are preserved in a recoverable state with explicit UI indicators.
- **Multi-Tab Native Editor**: High-performance text buffer management with line numbers, cursor position tracking, modified indicators, undo/redo stacks, and multi-file tab switching.
- **Developer Quick Key Bar**: Signature mobile editing bar with sticky modifier states (`Ctrl`, `Shift`, `Alt`), fast syntax symbols, and keyboard command shortcuts with configurable density.
- **Unified Command Architecture & Command Palette**: Instant access (`Ctrl+Shift+P` / `Ctrl+P`) to all application actions, file searches, and editor commands.
- **Truthful Context Actions**: Real buffer-to-disk Git Diff and honest runtime status banners (no simulated output or fake commit histories).
- **Comprehensive File Icon System**: Over 40 file extensions mapped across web languages, systems languages, data configs, build files, and media assets.
- **Media & Asset Viewing**: Native image viewer with error recovery for raster and modern formats, integrated PDF viewer, and external intent launching.
- **Persisted Settings Subsystem**: Complete theme customization (Dark / Light / System), editor font scaling, word wrap toggles, line number controls, and key bar density controls.
- **Architectural Abstractions**: Clean domain abstractions for Git, Terminal, Database engines, Language providers, Extensions, and AI services.

---

## Architecture

DroidCode uses a **hybrid architecture**:
- **Domain & Data Layer (Java & Kotlin)**: Core models, filesystem operations (`LocalFileSystem`), workspace management (`WorkspaceManager`), command registry (`CommandRegistry`), event bus (`EventBus`), and Room SQLite persistence (`DroidCodeDatabase`).
- **UI Layer (Kotlin & Jetpack Compose Material 3)**: Modern declarative interface with `EditorView`, `ExplorerPanel`, `QuickKeyBar`, `CommandPaletteDialog`, and `HomeView`.
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
