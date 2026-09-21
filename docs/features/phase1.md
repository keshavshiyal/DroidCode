# Phase 1 & 1.5 Features

This document provides an exhaustive inventory of the features implemented in **Phase 1 (Foundation)** and stabilized in **Phase 1.5 (Professionalization)**.

---

## 1. Workspace Management
- **Directory Selection**: Open any local directory or SAF directory tree as an active workspace.
- **Recent Workspaces**: Persistent list of opened workspaces stored in Room SQLite (`WorkspaceEntity`).
- **Graceful Availability Tracking**: Inaccessible or moved workspaces are visually labeled as "Unavailable" with explicit reconnect or removal controls.
- **Workspace Switching**: Close workspace safely and switch between projects.

---

## 2. File Explorer & Management
- **Hierarchical Directory Tree**: Real-time listing of files and directories with folder collapse/expand states.
- **File & Directory Creation**: Interactive dialogs with filename validation to create files and nested folders.
- **File Operations**:
  - Rename with collision detection
  - Copy and Move operations with automatic unique name resolution (`name_copy`, `name_copy1`)
  - Duplicate file action
  - Recursive directory deletion with user confirmation
- **File Classification & Icons**: Specialized icons for 40+ extensions (.html, .css, .sass, .js, .ts, .json, .md, .sql, .py, .java, .kt, .c, .cpp, .cs, .go, .rs, .rb, .php, .swift, .xml, .yaml, .toml, .ini, .properties, .env, .csv, .tsv, Dockerfile, Makefile, pom.xml, .gitignore, LICENSE, .editorconfig, PNG, JPG, SVG, WebP, AVIF, HEIC, PDF).

---

## 3. Editor Subsystem
- **Multi-Tab Interface**: Open multiple files simultaneously; switch between tabs with smooth tab headers and close buttons.
- **Real File Buffer**: Reads actual contents from disk; writes changes to disk upon save (`Ctrl+S` or menu).
- **Modified Indicator**: Visual asterisk / dot indicating dirty unsaved changes.
- **Unsaved Changes Protection**: Prompts user when closing tabs or workspaces with unsaved modifications (`Save`, `Don't Save`, `Cancel`).
- **Undo / Redo Engine**: Stack-based history tracking for fast rollback and re-application.
- **Line Numbers & Position Indicator**: Real-time line and column coordinates displayed in the status bar.
- **Search & Replace**: In-editor text search with match highlighting and next/previous navigation.

---

## 4. Mobile Quick Key Bar
- **Developer Quick Keys**: Touch-friendly keys for common syntax tokens (`{`, `}`, `(`, `)`, `[`, `]`, `;`, `:`, `=`, `"`, `'`, `/`, `\`, `<`, `>`, `&`, `|`, `!`, `?`, `#`, `$`, `%`, `^`, `*`, `+`, `-`, `_`, `~`, `` ` ``).
- **Modifier Keys**: Sticky modifier states for `Ctrl`, `Shift`, and `Alt` that integrate with software keyboards.
- **Navigation Controls**: Directional cursor arrows (Left, Right, Up, Down), Tab (indentation), Home, End, and Delete.
- **Density Control**: Configurable bar height (Compact 36dp, Normal 44dp, Comfortable 52dp).

---

## 5. Command System & Palette
- **Unified Command Registry**: All IDE operations registered with keyboard shortcuts, categorization, and state suppliers.
- **Command Palette**: Filterable fuzzy search dialog accessible via `Ctrl+Shift+P` / `Ctrl+P` or top bar search icon.
- **Keyboard Shortcuts**: Native hardware keyboard shortcuts for all major IDE functions (`Ctrl+S` Save, `Ctrl+Z` Undo, `Ctrl+Y` Redo, `Ctrl+F` Find, `Ctrl+W` Close Tab).

---

## 6. Context Actions & Diff
- **Buffer vs Disk Diff**: Computes real differences between the in-memory tab buffer and the saved file on disk.
- **Git Context Detection**: Reads branch names from `.git/HEAD`; honestly indicates repository status.
- **Truthful Runtime Banners**: Displays informative status for Run and Debug actions explaining the milestone roadmap.

---

## 7. Media & Document Viewers
- **Image Viewer**: Renders raster and modern image formats (.png, .jpg, .svg, .webp, .avif, .heic) with graceful fallback cards on decoding failure.
- **PDF Viewer**: Embedded native Android `PdfRenderer` with page navigation and zoom controls.
- **External App Launcher**: Safely launches system intent to open any file in specialized external applications.

---

## 8. Settings Subsystem
- **Theme Selection**: Dark (Neutral Deep Grey/Black), Light (Crisp Warm Off-White), or System Default.
- **Typography**: Configurable font size (10sp to 24sp) and font families (JetBrains Mono, Fira Code, Roboto Mono, Source Code Pro).
- **Editor Preferences**: Word wrap toggle, line number visibility toggle.
- **Persistent Storage**: All user preferences persist across application restarts.
