# Contributing to DroidCode

Thank you for your interest in contributing to DroidCode — the open-source native Android IDE!

## Core Engineering Principles

1. **No Fake / Mocked UI**: Features must be backed by real Android APIs, actual file I/O, and real system state. Empty states should honestly report when no workspace or data is present.
2. **Modular Architecture**: Maintain strict separation between core models (`com.example.core`), workspace/file services (`com.example.project`, `com.example.filesystem`), editor logic (`com.example.editor`), and Jetpack Compose UI (`com.example.ui`).
3. **Java First Core**: Maintain clean Java implementations for domain objects and service interfaces where appropriate.
4. **Testing**: Add unit tests for core logic in `app/src/test/java/com/example/`.

## Development Workflow

1. Fork & clone repository.
2. Build project using `./gradlew assembleDebug` or via Android Studio / AI Studio.
3. Verify test suite using `./gradlew testDebugUnitTest`.
4. Submit pull request targeting `main` branch.
