# Architectural Overview

DroidCode architecture decouples UI from domain models.

Key core modules:
- `com.example.core`: Event bus, command bus, disposable lifecycle.
- `com.example.filesystem`: File operations, SAF document provider adapter.
- `com.example.editor`: Text buffer, tab bar controller, undo/redo manager.
- `com.example.project`: Workspace metadata, recent project persistence.
- `com.example.settings`: App settings state store.
