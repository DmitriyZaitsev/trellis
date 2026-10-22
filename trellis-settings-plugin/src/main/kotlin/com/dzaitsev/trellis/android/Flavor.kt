package com.dzaitsev.trellis.android

/**
 * A single Android product flavor, e.g. a backend environment or a market region. Built by the
 * `flavors { dimension(...) { flavor(...) } }` DSL of `androidApp`/`androidLibrary`.
 */
public data class Flavor(
    public val name: String,
    public val dimension: String,
    public val isDefault: Boolean = false,
    /** Ignored for library modules, which AGP does not allow to set it. */
    public val applicationIdSuffix: String? = null,
)
