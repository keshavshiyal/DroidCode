# DroidCode Capability Registry

## 1. Purpose

The **DroidCode Capability Registry** (`com.droidcode.core.capability`) provides an authoritative, lightweight, and extensible catalog answering a fundamental architectural question:

> **"What does DroidCode actually support right now?"**

The registry serves as a single source of truth for genuine feature availability across the platform. It strictly enforces the distinction between:
- **UI Exists**: Visual elements, tabs, buttons, or placeholder screens are present in the layout.
- **Feature Actually Works**: The underlying subsystem, driver, parser, or filesystem logic is genuinely operational and verified.

## 2. Architecture & Design Principles

```
┌─────────────────────────────────────────────────────────────┐
│                    CapabilityRegistry                       │
│  (com.droidcode.core.capability.CapabilityRegistry)         │
└──────────────────────────────┬──────────────────────────────┘
                               │
            ┌──────────────────┴──────────────────┐
            ▼                                     ▼
    ┌───────────────┐                     ┌───────────────┐
    │  Capability   │                     │  Capability   │
    │ (Editor Sub.) │                     │   (Git Sub.)  │
    └───────┬───────┘                     └───────┬───────┘
            │                                     │
            ▼                                     ▼
    ┌───────────────┐                     ┌───────────────┐
    │Status:        │                     │Status:        │
    │AVAILABLE      │                     │PLANNED        │
    └───────────────┘                     └───────────────┘
```

### Core Design Rules
1. **Descriptive, Not Driving Business Logic**: The registry describes capabilities for diagnostics, About dialogs, documentation verification, and developer tools. Core IDE operations must **never** check `CapabilityRegistry` to simulate or mock behavior.
2. **No False Claims**: If a feature is only partially implemented or planned for a future milestone (e.g. real PTY terminals, MySQL connections, Git diffing), it MUST NOT be labeled as `AVAILABLE`.
3. **Zero Sensitive Data**: Capabilities only hold metadata (identifiers, names, descriptions, groups, dependencies, documentation links).

## 3. Capability Status Taxonomy

| Status | Meaning |
|---|---|
| `PLANNED` | Feature is on the project roadmap; no functional implementation exists yet in the codebase. |
| `DESIGNED` | Architectural contracts, data models, or UI frames exist, but backend implementation is pending. |
| `IN_PROGRESS` | Implementation is actively underway. |
| `PARTIAL` | Basic operations work (e.g., in-memory terminal or branch name reading), but full scope is incomplete. |
| `AVAILABLE` | Fully implemented, verified by automated unit tests, and functional on device. |
| `EXPERIMENTAL` | Operational but unstable, unoptimized, or subject to breaking changes. |
| `BLOCKED` | Dependent on unavailable Android OS APIs, external libraries, or missing permissions. |
| `DEPRECATED` | Superceded by a newer architecture; scheduled for sunset. |

## 4. Subsystem Groups

- **`EDITOR`**: Text buffer, multi-tab management, undo/redo history, cursor position tracking, syntax display, quick key bar.
- **`WORKSPACE`**: Project directory discovery, scaffolding templates, recent workspace history, Room SQLite persistence, stale directory detection.
- **`FILESYSTEM`**: Local filesystem tree enumeration, file creation, deletion, renaming, refresh, and Storage Access Framework (SAF) folder picker.
- **`LANGUAGE`**: Keyword tokenization, comment formatting, and file classification for JavaScript, SQL, Python, Java, Kotlin, HTML, CSS, JSON, and Markdown.
- **`GIT`**: `.git` directory presence detection, branch parsing from `HEAD`, status summaries.
- **`TERMINAL`**: Console UI panel, command dispatch, and terminal session buffers.
- **`DATABASE`**: `DatabaseProvider` and `QueryEngine` interfaces, Room SQLite persistence.
- **`AI`**: `AiProvider` and `AiService` pluggable service lifecycle abstractions.
- **`UI`**: Material 3 dark/light theming, adaptive multi-screen layouts, accessibility semantics, and command palette.

## 5. How to Add or Update a Capability

1. Open `com.droidcode.core.capability.CapabilityRegistry.kt`.
2. Add a `Capability` definition to `catalog`:
   ```kotlin
   Capability(
       id = "editor.folding",
       name = "Code Folding",
       group = CapabilityGroup.EDITOR,
       status = CapabilityStatus.PLANNED,
       description = "Fold and unfold code blocks based on indentation or braces.",
       dependencies = listOf("editor.basic-editing"),
       documentationRef = "docs/architecture/editor.md"
   )
   ```
3. Run `CapabilityRegistryTest.kt` to ensure ID uniqueness and dependency resolution.

## 6. Relationship to Project Roadmap & Tests

- **`ROADMAP.md`**: Defines target milestones (M1, M2, M3) and the trajectory of future engineering.
- **`CapabilityRegistry`**: Documents the exact baseline capabilities functioning in the current build.
- **`CapabilityRegistryTest`**: Automatically enforces that baseline capabilities remain marked `AVAILABLE`, planned capabilities do not accidentally report as available, and IDs are unique.
