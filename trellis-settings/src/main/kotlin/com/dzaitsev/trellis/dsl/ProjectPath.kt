package com.dzaitsev.trellis.dsl

public data class ProjectPath(
    public val segments: List<String>,
) {
    init {
        require(segments.all(::isValidSegment)) {
            "Invalid project path: ${segments.joinToString(":")}"
        }
    }

    public val isRoot: Boolean
        get() = segments.isEmpty()

    public val gradlePath: String
        get() = segments.joinToString(":", prefix = ":")

    public val relativeDirectory: String
        get() = segments.joinToString("/")

    /** The last path segment, or an empty string for [ROOT]. */
    public val name: String
        get() = segments.lastOrNull().orEmpty()

    public fun child(name: String): ProjectPath =
        ProjectPath(segments + name)

    override fun toString(): String = gradlePath

    public companion object {
        /** The Gradle root project, `:`. */
        public val ROOT: ProjectPath = ProjectPath(emptyList())

        public fun parse(value: String): ProjectPath {
            val trimmed = value.trim()
            require(trimmed.isNotEmpty()) {
                "Project path must not be blank"
            }

            val normalized = trimmed.removePrefix(":")
            return if (normalized.isEmpty()) ROOT else ProjectPath(normalized.split(':'))
        }

        internal fun isValidSegment(value: String): Boolean =
            value.isNotBlank() &&
                ':' !in value &&
                '/' !in value &&
                '\\' !in value
    }
}
