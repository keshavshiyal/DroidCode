# DroidCode

**DroidCode** is a native, professional open-source IDE written in Java and Jetpack Compose for Android.

Inspired by VS Code, IntelliJ IDEA, and Android Studio, DroidCode delivers a genuine workstation experience directly on mobile phones, foldables, and tablets.

---

## Key Features (Phase 1 Foundation)

- **Real Filesystem & Workspace Engine**: Create, open, edit, rename, and delete real files and workspace directories locally or via Android Storage Access Framework (SAF).
- **Multi-Tab Editor**: Native text buffer management with line numbers, cursor position tracking, modified indicators, search/replace, and multi-file tab switching.
- **Developer Quick Key Bar**: Signature mobile editing bar with sticky modifier states (`Ctrl`, `Shift`, `Alt`), fast syntax symbols, and keyboard command shortcuts.
- **Unified Command Architecture & Command Palette**: Instant access (`Ctrl+Shift+P` / `Ctrl+P`) to all application actions, file searches, and editor commands.
- **Persisted Settings Subsystem**: Complete theme customization (Dark / Light), editor font scaling, word wrap toggles, and key bar density controls.
- **Architectural Foundation**: Extensible provider interfaces for Git, Terminal, Database engines, Language providers, Extensions, and AI services.

---

## Building from Source

```bash
# Build Debug APK
./gradlew assembleDebug

# Run Unit Tests
./gradlew testDebugUnitTest
```

---

## Documentation

- [Project Status & Feature Registry](DROIDCODE_PROJECT_STATUS.md)
- [Roadmap & Milestones](ROADMAP.md)
- [Architecture & Modular Design](ARCHITECTURE.md)
- [Design System & UI Guidelines](DESIGN_SYSTEM.md)
- [Contributing Guidelines](CONTRIBUTING.md)
- [Changelog](CHANGELOG.md)
