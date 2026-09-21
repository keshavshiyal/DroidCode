# DroidCode Testing Guide

DroidCode prioritizes reliability, correctness, and testability. The automated test suite executes on the JVM using JUnit 4 and Robolectric, providing fast feedback and high confidence without requiring an attached Android emulator.

---

## 1. Running Tests

Execute the complete test suite using Gradle:

```bash
# Run all unit and Robolectric tests
gradle :app:testDebugUnitTest

# Re-run without using task cache
gradle :app:testDebugUnitTest --rerun-tasks
```

HTML test reports are generated at:
```
app/build/reports/tests/testDebugUnitTest/index.html
```

---

## 2. Test Architecture & Structure

Tests reside under `app/src/test/java/com/example/`:

| Test Suite | Target Subsystem | Key Verifications |
| :--- | :--- | :--- |
| **`WorkspaceManagerTest`** | `WorkspaceManager` | Workspace opening/closing, active project state, recent workspaces persistence, and non-destructive removal. |
| **`EditorManagerTest`** | `EditorManager` | Multi-tab opening, tab switching, buffer updates, modified flag tracking, undo/redo stacks, and tab closing. |
| **`LocalFileSystemTest`** | `LocalFileSystem` | File and directory creation, read/write integrity, recursive directory listing, and recursive deletion. |
| **`FileIconUtilsTest`** | `FileIconUtils` | File classification mapping across 40+ language, config, script, build, and media extensions. |
| **`SettingsManagerTest`** | `SettingsManager` | Settings defaults, modifications, deep copy integrity, and SharedPreferences persistence. |
| **`DroidCodeUnitTest`** | Core / Subsystems | `CommandRegistry` queries/execution, `EventBus` pub/sub, `EditorTab` line/column calculations, and project generation. |

---

## 3. Testing Principles for DroidCode

1. **Zero Mock Data**: Tests must exercise real filesystem operations (using JUnit `TemporaryFolder`) and real Room/SharedPreferences stores (using Robolectric `ApplicationProvider`).
2. **Deterministic & Isolated**: Each test must clean up its state before and after execution (e.g. closing open tabs or resetting singletons).
3. **No Flaky Timing**: Use synchronous operations and avoid relying on thread sleeps.
4. **Comprehensive Coverage**: When adding a new capability (e.g., a file extension or a command), accompany it with a test in the appropriate test suite.
