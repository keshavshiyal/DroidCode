package com.droidcode.core.capability

/**
 * Metadata descriptor for a single capability within DroidCode.
 *
 * Capabilities describe the real state of functionality across subsystems,
 * enabling diagnostic tooling, documentation verification, and feature introspection.
 */
data class Capability(
    val id: String,
    val name: String,
    val group: CapabilityGroup,
    val status: CapabilityStatus,
    val description: String,
    val dependencies: List<String> = emptyList(),
    val documentationRef: String? = null
)
