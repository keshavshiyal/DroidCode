# DroidCode

**DroidCode** is a native, professional open-source IDE written in Java and Kotlin (Jetpack Compose) for Android.

Inspired by VS Code, IntelliJ IDEA, and Android Studio, DroidCode delivers a genuine workstation experience directly on mobile phones, foldables, and tablets.

---

## Current Status: Phase 1.5.1 Complete → Next: Milestone 1 (M1)

DroidCode has completed **Phase 1.5.1 (Foundation Hardening & Capability Registry)** and is advancing into **Milestone 1 (M1 — Professional IDE Shell)**. All core subsystems operate with real local persistence, truthful status reporting, zero mock data, modern dependency injection (Hilt), type-safe Navigation Compose, and adaptive multi-pane layouts.

### Capability Registry & Feature Status

| Feature ID | Subsystem | Capability | Status | Architecture Notes |
| :--- | :--- | :--- | :---: | :--- |
| **CORE-001** | Core | Dependency Injection | `AVAILABLE` | Hilt DI with `@Singleton` managers (`AppModule.kt`) |
| **CORE-002** | Core | Command Architecture | `AVAILABLE` | Unified Command Registry & Event Bus |
| **CORE-003** | Core | Capability Registry | `AVAILABLE` | Honest programmatic feature discovery (`CapabilityRegistry`) |
| **NAV-001** | Navigation | Navigation Compose | `AVAILABLE` | Routes: `Home`, `Workspace`, `Settings`, `Git`, `Terminal` |
| **UI-001** | Shell | Adaptive Multi-Pane | `AVAILABLE` | WindowSizeClass responsive split (`EXPANDED` tablet / `COMPACT` phone) |
| **UI-002** | Shell | Theme System | `AVAILABLE` | Deep dark & soft light Material 3 theming |
| **UI-003** | Shell | Developer Quick Key Bar | `AVAILABLE` | Sticky modifiers (`Ctrl`, `Shift`, `Alt`), fast syntax symbols |
| **UI-004** | Shell | Command Palette | `AVAILABLE` | Fuzzy search & ranking dialog (`Ctrl+Shift+P`) |
| **WORKSPACE-001** | Workspace | Workspace Management | `AVAILABLE` | Room DB persistence, SAF folder selection, non-destructive reconnect |
| **FILES-001** | Filesystem | Local File System & SAF | `AVAILABLE` | Real CRUD operations, directory tree recursion, SAF document tree |
| **FILES-002** | Filesystem | File Icon Classification | `AVAILABLE` | 40+ language, config, and media extensions mapped |
| **EDITOR-001** | Editor | Multi-Tab Buffer Management | `AVAILABLE` | Tab lifecycle, undo/redo history, dirty indicators, cursor tracking |
| **EDITOR-002** | Editor | Truthful Actions & Diff | `AVAILABLE` | Real disk-to-buffer Git diff, honest runtime reporting |
| **MEDIA-001** | Media | Image & Document Viewer | `AVAILABLE` | Resilient raster/modern image decoding, PDF preview |
| **SETTINGS-001** | Settings | Settings Subsystem | `AVAILABLE` | Preferences persistence for editor, typography, key bar |
| **GIT-001** | VCS | Git Service Integration | `PARTIAL` | Workspace `.git` inspection, branch detection, buffer diff |
| **TERMINAL-001** | Runtime | Terminal Subsystem | `PARTIAL` | Terminal panel shell with real execution diagnostics |
| **DB-001** | Database | Database Explorer | `PLANNED` | Provider interfaces & connection manager |
| **AI-001** | AI | AI Assistant Services | `PLANNED` | Provider abstraction for future localized & remote models |
| **CI-001** | CI/CD | GitHub Actions Workflow | `AVAILABLE` | Android Lint, Detekt static analysis, unit test suite, APK assembly |
| **TEST-001** | Quality | Automated Test Suite | `AVAILABLE` | Unit tests, Robolectric tests, Compose UI tests & benchmarks |

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
