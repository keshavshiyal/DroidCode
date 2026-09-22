package com.droidcode.core.capability

/**
 * Indicates the actual implementation maturity of a DroidCode capability.
 * Strictly separates UI presentation from genuinely working features.
 */
enum class CapabilityStatus {
    /** Feature is on the project roadmap but implementation has not started. */
    PLANNED,

    /** Architecture, interfaces, or mockups designed; implementation pending. */
    DESIGNED,

    /** Implementation is currently active and under construction. */
    IN_PROGRESS,

    /** Basic or partial implementation exists; not yet fully complete. */
    PARTIAL,

    /** Fully implemented, functional, and verified in the current build. */
    AVAILABLE,

    /** Implemented but considered experimental or subject to breaking changes. */
    EXPERIMENTAL,

    /** Implementation is blocked by external dependencies or runtime constraints. */
    BLOCKED,

    /** Feature was supported previously but is now deprecated or superseded. */
    DEPRECATED
}
