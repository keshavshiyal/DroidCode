package com.droidcode.core.capability

/**
 * Functional subsystem groups for DroidCode capabilities.
 */
enum class CapabilityGroup(val displayName: String) {
    EDITOR("Code Editor"),
    WORKSPACE("Workspace Management"),
    FILESYSTEM("File System & SAF"),
    LANGUAGE("Language & Syntax Support"),
    GIT("Version Control (Git)"),
    TERMINAL("Terminal Workstation"),
    DATABASE("Database Management"),
    AI("AI Assistance"),
    UI("User Interface & Theme")
}
